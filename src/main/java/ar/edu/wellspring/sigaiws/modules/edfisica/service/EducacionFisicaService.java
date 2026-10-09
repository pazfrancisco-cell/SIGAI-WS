package ar.edu.wellspring.sigaiws.modules.edfisica.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.GrupoEducacionFisica;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.GrupoEfAlumno;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.PruebaFisica;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.PruebaFisicaDefinicion;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.ResultadoPruebaFisica;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.RestriccionMedica;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.GrupoForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.PruebaFisicaDefinicionForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.PruebaFisicaForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.RestriccionMedicaForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.repository.GrupoEducacionFisicaRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.repository.GrupoEfAlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.repository.PruebaFisicaDefinicionRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.repository.PruebaFisicaRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.repository.ResultadoPruebaFisicaRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.repository.RestriccionMedicaRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Modulo 5.7 (Educacion Fisica). */
@Service
@RequiredArgsConstructor
public class EducacionFisicaService {

    private static final String MODULO = "EDUCACION_FISICA";

    private final GrupoEducacionFisicaRepository grupoRepository;
    private final GrupoEfAlumnoRepository grupoEfAlumnoRepository;
    private final PruebaFisicaRepository pruebaFisicaRepository;
    private final PruebaFisicaDefinicionRepository definicionRepository;
    private final ResultadoPruebaFisicaRepository resultadoRepository;
    private final RestriccionMedicaRepository restriccionMedicaRepository;
    private final AlumnoRepository alumnoRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AlmacenamientoService almacenamientoService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<GrupoEducacionFisica> grupos() {
        return grupoRepository.findAllByCicloLectivo_IdAndActivoTrue(cicloActivo().getId());
    }

    /** Cuatrimestre por defecto para cargas "en el momento" (pruebas fisicas, grupos nuevos): el que corresponde a hoy. */
    @Transactional(readOnly = true)
    public int cuatrimestreEnCurso() {
        return cicloLectivoRepository.findByActivoTrue().map(CicloLectivo::getCuatrimestreEnCurso).orElse(1);
    }

    @Transactional(readOnly = true)
    public List<GrupoEducacionFisica> gruposDelDocente(Long docenteId) {
        return grupoRepository.findAllByDocente_IdAndCicloLectivo_Id(docenteId, cicloActivo().getId());
    }

    public record AlumnoConCurso(Alumno alumno, CursoDivision cursoDivision) {}

    /** Alumnos activos del ciclo lectivo vigente junto con su año/division actual, para el filtro de armado de grupos (seccion 5.7.2). */
    @Transactional(readOnly = true)
    public List<AlumnoConCurso> alumnosDisponiblesParaGrupo() {
        CicloLectivo ciclo = cicloActivo();
        List<Alumno> alumnos = alumnoRepository.buscar(null, EstadoAlumno.ACTIVO, null, null, ciclo.getId(), Pageable.unpaged()).getContent();
        Map<Long, CursoDivision> cursoPorAlumno = alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(ciclo.getId()).stream()
                .collect(Collectors.toMap(acl -> acl.getAlumno().getId(), AlumnoCicloLectivo::getCursoDivision, (a, b) -> a));
        return alumnos.stream().map(a -> new AlumnoConCurso(a, cursoPorAlumno.get(a.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<PruebaFisicaDefinicion> definicionesActivas() {
        return definicionRepository.findAllByActivaTrueOrderByNombre();
    }

    public record DefinicionConUso(PruebaFisicaDefinicion definicion, int cantidadPruebas) {}

    /** Catalogo completo con cuantas sesiones de prueba usa cada una, para detectar duplicados por nombre distinto (seccion 5.7.4). */
    @Transactional(readOnly = true)
    public List<DefinicionConUso> catalogoConUso() {
        return definicionRepository.findAllByActivaTrueOrderByNombre().stream()
                .map(d -> new DefinicionConUso(d, pruebaFisicaRepository.findAllByDefinicion_Id(d.getId()).size()))
                .toList();
    }

    /**
     * Fusiona dos pruebas del catalogo que en realidad son la misma actividad con nombres distintos
     * (ej. "50 Metros" y "Carrera 50 metros"): todas las sesiones y grupos que usaban la descartada
     * pasan a usar la conservada, y la descartada se elimina. Preserva el historial para los graficos
     * de evolucion en vez de perderlo.
     */
    @Transactional
    public void fusionarDefiniciones(Long descartarId, Long conservarId) {
        if (descartarId.equals(conservarId)) {
            throw new BusinessException("Elegi dos pruebas distintas para fusionar.");
        }
        PruebaFisicaDefinicion descartada = definicionRepository.findById(descartarId)
                .orElseThrow(() -> NotFoundException.of("PruebaFisicaDefinicion", descartarId));
        PruebaFisicaDefinicion conservada = definicionRepository.findById(conservarId)
                .orElseThrow(() -> NotFoundException.of("PruebaFisicaDefinicion", conservarId));

        for (PruebaFisica prueba : pruebaFisicaRepository.findAllByDefinicion_Id(descartarId)) {
            prueba.setDefinicion(conservada);
            pruebaFisicaRepository.save(prueba);
        }
        for (GrupoEducacionFisica grupo : grupoRepository.findAllByPruebasDefiniciones_Id(descartarId)) {
            grupo.getPruebasDefiniciones().remove(descartada);
            grupo.getPruebasDefiniciones().add(conservada);
            grupoRepository.save(grupo);
        }
        definicionRepository.delete(descartada);
        auditService.registrar(MODULO, "FUSIONAR_PRUEBA_FISICA_DEFINICION", "PruebaFisicaDefinicion", conservarId,
                descartada.getNombre(), conservada.getNombre());
    }

    @Transactional
    public void agregarDefinicionAlGrupo(Long grupoId, Long definicionId) {
        GrupoEducacionFisica grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> NotFoundException.of("GrupoEducacionFisica", grupoId));
        definicionRepository.findById(definicionId).ifPresent(d -> grupo.getPruebasDefiniciones().add(d));
        grupoRepository.save(grupo);
    }

    @Transactional(readOnly = true)
    public List<PruebaFisicaDefinicion> definicionesDelGrupo(Long grupoId) {
        return grupoRepository.findById(grupoId)
                .map(g -> g.getPruebasDefiniciones().stream().sorted(Comparator.comparing(PruebaFisicaDefinicion::getNombre)).toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public GrupoEducacionFisica obtenerGrupo(Long id) {
        return grupoRepository.findById(id).orElseThrow(() -> NotFoundException.of("GrupoEducacionFisica", id));
    }

    /** Alta o edicion de un grupo (seccion 5.7.2): el docente, el catalogo de pruebas asignadas y el
     * plantel de alumnos del cuatrimestre indicado se reemplazan por completo con lo enviado en el formulario. */
    @Transactional
    public GrupoEducacionFisica guardarGrupo(GrupoForm form, Usuario editor) {
        GrupoEducacionFisica grupo = form.getId() != null ? obtenerGrupo(form.getId()) : new GrupoEducacionFisica();
        grupo.setNombre(form.getNombre());
        grupo.setGenero(form.getGenero());
        grupo.setDocente(usuarioRepository.findById(form.getDocenteId())
                .orElseThrow(() -> NotFoundException.of("Usuario", form.getDocenteId())));
        if (grupo.getCicloLectivo() == null) {
            grupo.setCicloLectivo(cicloActivo());
        }

        grupo.getPruebasDefiniciones().clear();
        for (Long definicionId : form.getDefinicionIds()) {
            definicionRepository.findById(definicionId).ifPresent(d -> grupo.getPruebasDefiniciones().add(d));
        }
        for (PruebaFisicaDefinicionForm nueva : form.getNuevasDefiniciones()) {
            if (nueva.getNombre() == null || nueva.getNombre().isBlank()) {
                continue;
            }
            grupo.getPruebasDefiniciones().add(obtenerOCrearDefinicion(nueva, editor));
        }

        grupoRepository.save(grupo);

        grupoEfAlumnoRepository.deleteAllByGrupo_IdAndCuatrimestre(grupo.getId(), form.getCuatrimestre());
        for (Long alumnoId : form.getAlumnoIds()) {
            GrupoEfAlumno vinculo = new GrupoEfAlumno();
            vinculo.setGrupo(grupo);
            vinculo.setAlumno(alumnoRepository.findById(alumnoId).orElseThrow(() -> NotFoundException.of("Alumno", alumnoId)));
            vinculo.setCuatrimestre(form.getCuatrimestre());
            grupoEfAlumnoRepository.save(vinculo);
        }
        auditService.registrar(MODULO, form.getId() == null ? "CREAR_GRUPO" : "EDITAR_GRUPO",
                "GrupoEducacionFisica", grupo.getId(), null, form.getNombre());
        return grupo;
    }

    /** Da de alta una prueba nueva en el catalogo, o reutiliza la existente si ya hay una con ese nombre (sin distinguir mayusculas). */
    @Transactional
    public PruebaFisicaDefinicion obtenerOCrearDefinicion(PruebaFisicaDefinicionForm form, Usuario creadoPor) {
        return definicionRepository.findByNombreIgnoreCase(form.getNombre().trim()).orElseGet(() -> {
            PruebaFisicaDefinicion definicion = new PruebaFisicaDefinicion();
            definicion.setNombre(form.getNombre().trim());
            definicion.setUnidad(form.getUnidad().trim());
            definicion.setMayorEsMejor(form.isMayorEsMejor());
            definicion.setCreadoPor(creadoPor);
            PruebaFisicaDefinicion guardada = definicionRepository.save(definicion);
            auditService.registrar(MODULO, "CREAR_PRUEBA_FISICA_DEFINICION", "PruebaFisicaDefinicion", guardada.getId(), null, guardada.getNombre());
            return guardada;
        });
    }

    @Transactional(readOnly = true)
    public List<Alumno> alumnosDelGrupo(Long grupoId, int cuatrimestre) {
        return grupoEfAlumnoRepository.findAllByGrupo_IdAndCuatrimestre(grupoId, cuatrimestre).stream()
                .map(GrupoEfAlumno::getAlumno).toList();
    }

    @Transactional(readOnly = true)
    public List<PruebaFisica> pruebasDe(Long grupoId, int cuatrimestre) {
        return pruebaFisicaRepository.findAllByGrupo_IdAndCuatrimestre(grupoId, cuatrimestre);
    }

    @Transactional
    public PruebaFisica crearPrueba(PruebaFisicaForm form, Usuario creadoPor) {
        GrupoEducacionFisica grupo = grupoRepository.findById(form.getGrupoId())
                .orElseThrow(() -> NotFoundException.of("GrupoEducacionFisica", form.getGrupoId()));
        PruebaFisicaDefinicion definicion = definicionRepository.findById(form.getDefinicionId())
                .orElseThrow(() -> NotFoundException.of("PruebaFisicaDefinicion", form.getDefinicionId()));

        PruebaFisica prueba = new PruebaFisica();
        prueba.setGrupo(grupo);
        prueba.setCuatrimestre(form.getCuatrimestre());
        prueba.setDefinicion(definicion);
        prueba.setFecha(form.getFecha());
        prueba.setCreadoPor(creadoPor);
        prueba = pruebaFisicaRepository.save(prueba);

        for (PruebaFisicaForm.ResultadoItem item : form.getResultados()) {
            boolean sinValor = item.getValor() == null || item.getValor().isBlank();
            if (!item.isSaf() && sinValor) {
                continue; // celda vacia: no se cargo nada para este alumno ese dia
            }
            ResultadoPruebaFisica resultado = resultadoRepository.findByPrueba_IdAndAlumno_Id(prueba.getId(), item.getAlumnoId())
                    .orElseGet(ResultadoPruebaFisica::new);
            resultado.setPrueba(prueba);
            resultado.setAlumno(alumnoRepository.findById(item.getAlumnoId())
                    .orElseThrow(() -> NotFoundException.of("Alumno", item.getAlumnoId())));
            if (item.isSaf()) {
                // Seccion 5.7 (extension): SAF puntual del dia (ej. lesion), sin importar lo que haya en el input de valor.
                resultado.setSaf(true);
                resultado.setValor(null);
                resultadoRepository.save(resultado);
                continue;
            }
            try {
                resultado.setSaf(false);
                resultado.setValor(new BigDecimal(item.getValor().replace(",", ".")));
                resultadoRepository.save(resultado);
            } catch (NumberFormatException ignored) {
                // celda invalida: se omite
            }
        }
        auditService.registrar(MODULO, "CREAR_PRUEBA_FISICA", "PruebaFisica", prueba.getId(), null, definicion.getNombre());
        return prueba;
    }

    @Transactional(readOnly = true)
    public List<ResultadoPruebaFisica> resultadosDe(Long pruebaId) {
        return resultadoRepository.findAllByPrueba_Id(pruebaId);
    }

    public record FilaRanking(int posicion, Alumno alumno, BigDecimal valor, String progreso) {}

    /**
     * Seccion 5.7.4: "El sistema calcula automaticamente el ranking del grupo
     * y el progreso individual comparando con pruebas anteriores del mismo tipo."
     * Ranking: ascendente si la definicion marca que menor valor es mejor,
     * descendente si mayor valor es mejor.
     */
    @Transactional(readOnly = true)
    public List<FilaRanking> rankingDe(Long pruebaId) {
        PruebaFisica prueba = pruebaFisicaRepository.findById(pruebaId)
                .orElseThrow(() -> NotFoundException.of("PruebaFisica", pruebaId));
        boolean menorEsMejor = !prueba.getDefinicion().isMayorEsMejor();

        List<ResultadoPruebaFisica> resultados = resultadoRepository.findAllByPrueba_Id(pruebaId).stream()
                .filter(r -> !r.isSaf())
                .collect(Collectors.toCollection(ArrayList::new));
        resultados.sort(menorEsMejor
                ? Comparator.comparing(ResultadoPruebaFisica::getValor)
                : Comparator.comparing(ResultadoPruebaFisica::getValor).reversed());

        var pruebasAnteriores = pruebaFisicaRepository.findAllByGrupo_IdAndDefinicion_IdAndFechaBeforeOrderByFechaDesc(
                prueba.getGrupo().getId(), prueba.getDefinicion().getId(), prueba.getFecha());

        List<FilaRanking> filas = new ArrayList<>();
        int posicion = 1;
        for (ResultadoPruebaFisica resultado : resultados) {
            String progreso = "Primera vez";
            for (PruebaFisica anterior : pruebasAnteriores) {
                var resultadoAnterior = resultadoRepository.findByPrueba_IdAndAlumno_Id(anterior.getId(), resultado.getAlumno().getId())
                        .filter(r -> !r.isSaf());
                if (resultadoAnterior.isPresent()) {
                    BigDecimal delta = resultado.getValor().subtract(resultadoAnterior.get().getValor());
                    boolean mejoro = menorEsMejor ? delta.signum() < 0 : delta.signum() > 0;
                    if (delta.signum() == 0) {
                        progreso = "Sin cambios";
                    } else {
                        progreso = (mejoro ? "Mejoro " : "Empeoro ") + delta.abs().stripTrailingZeros().toPlainString();
                    }
                    break;
                }
            }
            filas.add(new FilaRanking(posicion++, resultado.getAlumno(), resultado.getValor(), progreso));
        }
        return filas;
    }

    public record ResumenPrueba(List<FilaRanking> mejores, int completados, int totalEsperado, int porcentajeCompletitud) {}

    /** Dashboard al entrar a una prueba: mejores puntuaciones y % de alumnos del grupo que ya tienen resultado cargado. */
    @Transactional(readOnly = true)
    public ResumenPrueba resumenDe(Long pruebaId) {
        PruebaFisica prueba = pruebaFisicaRepository.findById(pruebaId)
                .orElseThrow(() -> NotFoundException.of("PruebaFisica", pruebaId));
        List<FilaRanking> ranking = rankingDe(pruebaId);
        List<FilaRanking> mejores = ranking.subList(0, Math.min(3, ranking.size()));
        int totalEsperado = grupoEfAlumnoRepository.findAllByGrupo_IdAndCuatrimestre(
                prueba.getGrupo().getId(), prueba.getCuatrimestre()).size();
        int completados = ranking.size();
        int porcentaje = totalEsperado == 0 ? 0 : (int) Math.round(completados * 100.0 / totalEsperado);
        return new ResumenPrueba(mejores, completados, totalEsperado, porcentaje);
    }

    public record PuntoEvolucion(java.time.LocalDate fecha, BigDecimal valor, String grupoNombre) {}

    /**
     * Seccion 5.7.4: evolucion de un alumno en cada prueba del catalogo que haya
     * rendido, a traves de cualquier grupo y ciclo lectivo (la prueba fisica se
     * toma mas de una vez por año, todos los años).
     */
    @Transactional(readOnly = true)
    public Map<PruebaFisicaDefinicion, List<PuntoEvolucion>> evolucionDe(Long alumnoId) {
        Map<PruebaFisicaDefinicion, List<PuntoEvolucion>> porDefinicion = new LinkedHashMap<>();
        for (ResultadoPruebaFisica resultado : resultadoRepository.findAllByAlumno_IdOrderByPrueba_FechaAsc(alumnoId)) {
            if (resultado.isSaf()) {
                continue; // sin valor numerico: no participa de la evolucion (seccion 5.7, extension SAF)
            }
            PruebaFisica prueba = resultado.getPrueba();
            porDefinicion.computeIfAbsent(prueba.getDefinicion(), d -> new ArrayList<>())
                    .add(new PuntoEvolucion(prueba.getFecha(), resultado.getValor(), prueba.getGrupo().getNombre()));
        }
        return porDefinicion;
    }

    private static final double CHART_ANCHO = 320;
    private static final double CHART_ALTO = 110;
    private static final double CHART_PAD = 14;

    public record PuntoGraficoEvolucion(String x, String y, java.time.LocalDate fecha, BigDecimal valor, String grupoNombre) {}

    public record GraficoEvolucion(PruebaFisicaDefinicion definicion, String polyline, List<PuntoGraficoEvolucion> puntos,
                                    String minValor, String maxValor) {}

    /** Convierte la evolucion de un alumno en coordenadas listas para dibujar un SVG (sin librerias de graficos, seccion 5.7.4). */
    @Transactional(readOnly = true)
    public List<GraficoEvolucion> evolucionGraficosDe(Long alumnoId) {
        List<GraficoEvolucion> graficos = new ArrayList<>();
        for (var entry : evolucionDe(alumnoId).entrySet()) {
            List<PuntoEvolucion> serie = entry.getValue();
            if (serie.isEmpty()) {
                continue;
            }
            BigDecimal min = serie.stream().map(PuntoEvolucion::valor).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            BigDecimal max = serie.stream().map(PuntoEvolucion::valor).max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
            BigDecimal rango = max.subtract(min);
            boolean rangoNulo = rango.signum() == 0;

            int n = serie.size();
            List<PuntoGraficoEvolucion> puntos = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                PuntoEvolucion p = serie.get(i);
                double x = n == 1 ? CHART_ANCHO / 2.0 : CHART_PAD + i * (CHART_ANCHO - 2 * CHART_PAD) / (n - 1);
                double frac = rangoNulo ? 0.5 : p.valor().subtract(min).doubleValue() / rango.doubleValue();
                double y = CHART_PAD + (1 - frac) * (CHART_ALTO - 2 * CHART_PAD);
                puntos.add(new PuntoGraficoEvolucion(coord(x), coord(y), p.fecha(), p.valor(), p.grupoNombre()));
            }
            String polyline = puntos.stream().map(p -> p.x() + "," + p.y()).collect(Collectors.joining(" "));
            graficos.add(new GraficoEvolucion(entry.getKey(), polyline, puntos,
                    min.stripTrailingZeros().toPlainString(), max.stripTrailingZeros().toPlainString()));
        }
        return graficos;
    }

    private static String coord(double valor) {
        return String.format(java.util.Locale.US, "%.1f", valor);
    }

    @Transactional(readOnly = true)
    public PruebaFisica obtenerPrueba(Long pruebaId) {
        return pruebaFisicaRepository.findById(pruebaId).orElseThrow(() -> NotFoundException.of("PruebaFisica", pruebaId));
    }

    /** Da de baja una sesion de prueba ya cargada (error o duplicacion, seccion 5.7.4), junto con sus resultados. */
    @Transactional
    public void eliminarPrueba(Long pruebaId) {
        PruebaFisica prueba = obtenerPrueba(pruebaId);
        resultadoRepository.deleteAllByPrueba_Id(pruebaId);
        pruebaFisicaRepository.delete(prueba);
        auditService.registrar(MODULO, "ELIMINAR_PRUEBA_FISICA", "PruebaFisica", pruebaId, prueba.getDefinicion().getNombre(), null);
    }

    @Transactional
    public RestriccionMedica crearRestriccion(RestriccionMedicaForm form, Usuario creadoPor, MultipartFile documento) {
        RestriccionMedica restriccion = new RestriccionMedica();
        restriccion.setAlumno(alumnoRepository.findById(form.getAlumnoId())
                .orElseThrow(() -> NotFoundException.of("Alumno", form.getAlumnoId())));
        restriccion.setDescripcion(form.getDescripcion());
        restriccion.setFechaInicio(form.getFechaInicio());
        restriccion.setFechaFin(form.getFechaFin());
        restriccion.setCreadoPor(creadoPor);
        if (documento != null && !documento.isEmpty()) {
            var guardado = almacenamientoService.guardar(documento, "restricciones-medicas", 5);
            restriccion.setDocumentoPath(guardado.pathRelativo());
        }
        restriccion = restriccionMedicaRepository.save(restriccion);
        auditService.registrar(MODULO, "CREAR_RESTRICCION_MEDICA", "RestriccionMedica", restriccion.getId(), null, null);
        return restriccion;
    }

    @Transactional(readOnly = true)
    public List<RestriccionMedica> restriccionesDe(Long alumnoId) {
        return restriccionMedicaRepository.findAllByAlumno_Id(alumnoId);
    }

    @Transactional(readOnly = true)
    public List<RestriccionMedica> todasLasRestricciones() {
        return restriccionMedicaRepository.findAllByOrderByFechaInicioDesc();
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
