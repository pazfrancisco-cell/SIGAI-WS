package ar.edu.wellspring.sigaiws.modules.alumno.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.MateriaAdeudada;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.MesaExamenService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.14.2 (resuelve ADR-010): asistente de promocion al empezar un ciclo
 * lectivo. Propone para cada alumno activo del ciclo anterior si pasa de año,
 * repite o egresa (editable), y al confirmar lo inscribe en su curso del ciclo
 * nuevo (con contadores en cero). El historial del ciclo anterior no se toca.
 */
@Service
@RequiredArgsConstructor
public class PromocionService {

    /** Se puede pasar de año debiendo hasta esta cantidad de materias (contando previas); con mas, repite. */
    static final int MAXIMO_MATERIAS_ADEUDADAS = 2;
    private static final int ULTIMO_ANIO = 6;

    public enum Accion { PASA, REPITE, EGRESA }

    /** indice: posicion correlativa entre todos los alumnos, para enlazar el formulario (decisiones[indice]). */
    public record FilaPromocion(int indice, Alumno alumno, CursoDivision cursoActual, Accion sugerida, String motivo,
                                Long destinoSugeridoId, Long destinoPasaId, Long destinoRepiteId) {}

    public record GrupoPromocion(CursoDivision curso, List<FilaPromocion> filas) {}

    public record Propuesta(CicloLectivo cicloAnterior, CicloLectivo cicloNuevo, List<GrupoPromocion> grupos) {
        public int totalAlumnos() {
            return grupos.stream().mapToInt(g -> g.filas().size()).sum();
        }
    }

    /** Lo que llega del formulario: una decision por alumno. */
    @Getter
    @Setter
    public static class Decision {
        private Long alumnoId;
        private Accion accion;
        private Long cursoDestinoId;
    }

    @Getter
    @Setter
    public static class PromocionForm {
        private List<Decision> decisiones = new ArrayList<>();
    }

    public record Resultado(int pasaron, int repiten, int egresaron, int yaInscriptos) {}

    private final CicloLectivoRepository cicloLectivoRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final AlumnoRepository alumnoRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final MesaExamenService mesaExamenService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Propuesta propuesta() {
        CicloLectivo nuevo = cicloActivo();
        Optional<CicloLectivo> anteriorOpt = cicloAnterior(nuevo);
        if (anteriorOpt.isEmpty()) {
            return new Propuesta(null, nuevo, List.of());
        }
        CicloLectivo anterior = anteriorOpt.get();
        Set<Long> yaInscriptos = idsInscriptosEn(nuevo);

        List<AlumnoCicloLectivo> pendientes = alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(anterior.getId()).stream()
                .filter(acl -> acl.getAlumno().getEstadoGlobal() == EstadoAlumno.ACTIVO)
                .filter(acl -> !yaInscriptos.contains(acl.getAlumno().getId()))
                .sorted(Comparator.comparing((AlumnoCicloLectivo acl) -> acl.getCursoDivision().getAnio())
                        .thenComparing(acl -> acl.getCursoDivision().getDivision())
                        .thenComparing(acl -> acl.getAlumno().getApellido())
                        .thenComparing(acl -> acl.getAlumno().getNombre()))
                .toList();

        Map<CursoDivision, List<FilaPromocion>> porCurso = new LinkedHashMap<>();
        for (int i = 0; i < pendientes.size(); i++) {
            AlumnoCicloLectivo acl = pendientes.get(i);
            porCurso.computeIfAbsent(acl.getCursoDivision(), c -> new ArrayList<>()).add(sugerir(i, acl));
        }

        List<GrupoPromocion> grupos = porCurso.entrySet().stream()
                .map(e -> new GrupoPromocion(e.getKey(), e.getValue()))
                .toList();
        return new Propuesta(anterior, nuevo, grupos);
    }

    /**
     * Se decide con lo que el alumno sigue debiendo despues de las mesas de
     * diciembre y febrero, sumando las previas de años anteriores: hasta 2 pasa
     * (y las debe durante el año siguiente); mas de 2, repite.
     */
    private FilaPromocion sugerir(int indice, AlumnoCicloLectivo acl) {
        CursoDivision curso = acl.getCursoDivision();
        List<MateriaAdeudada> debe = mesaExamenService.pendientesDe(acl.getAlumno().getId());
        int adeudadas = debe.size();
        String cuales = debe.stream().map(MateriaAdeudada::getDescripcion).collect(Collectors.joining(", "));

        Long destinoPasa = curso.getAnio() >= ULTIMO_ANIO ? null : destinoEnAnio(curso.getAnio() + 1, curso.getDivision());
        Long destinoRepite = destinoEnAnio(curso.getAnio(), curso.getDivision());

        Accion accion;
        String motivo;
        if (acl.isCondicionLibre()) {
            accion = Accion.REPITE;
            motivo = "Quedo en condicion libre";
        } else if (adeudadas > MAXIMO_MATERIAS_ADEUDADAS) {
            accion = Accion.REPITE;
            motivo = "Debe " + adeudadas + " materias despues de febrero: " + cuales;
        } else if (curso.getAnio() >= ULTIMO_ANIO) {
            accion = Accion.EGRESA;
            motivo = "Termino " + ULTIMO_ANIO + "° año" + (adeudadas > 0 ? ". Debe " + cuales + " (titulo pendiente)" : "");
        } else {
            accion = Accion.PASA;
            motivo = adeudadas > 0 ? "Pasa debiendo " + cuales : "";
        }
        Long destinoSugerido = switch (accion) {
            case PASA -> destinoPasa;
            case REPITE -> destinoRepite;
            case EGRESA -> null;
        };
        return new FilaPromocion(indice, acl.getAlumno(), curso, accion, motivo, destinoSugerido, destinoPasa, destinoRepite);
    }

    /** Mismo año y letra si ese curso sigue activo; si no, la primera division activa de ese año. */
    private Long destinoEnAnio(int anio, String division) {
        return cursoDivisionRepository.findByAnioAndDivision(anio, division)
                .filter(CursoDivision::isActiva)
                .or(() -> cursoDivisionRepository.findAllByAnioAndActivaTrueOrderByDivision(anio).stream().findFirst())
                .map(CursoDivision::getId)
                .orElse(null);
    }

    /** Todo o nada: si alguna decision esta incompleta no se inscribe a nadie. */
    @Transactional
    public Resultado confirmar(List<Decision> decisiones) {
        CicloLectivo nuevo = cicloActivo();
        Optional<CicloLectivo> anterior = cicloAnterior(nuevo);
        Set<Long> yaInscriptos = idsInscriptosEn(nuevo);

        List<String> incompletas = new ArrayList<>();
        for (Decision d : decisiones) {
            if (d.getAccion() == null) {
                incompletas.add(nombre(d.getAlumnoId()) + " (falta elegir que hace)");
            } else if (d.getAccion() != Accion.EGRESA && d.getCursoDestinoId() == null) {
                incompletas.add(nombre(d.getAlumnoId()) + " (falta el curso de destino)");
            }
        }
        if (!incompletas.isEmpty()) {
            throw new BusinessException("No se aplico la promocion. Completa: " + String.join(", ", incompletas) + ".");
        }

        int pasaron = 0;
        int repiten = 0;
        int egresaron = 0;
        int omitidos = 0;
        for (Decision d : decisiones) {
            if (yaInscriptos.contains(d.getAlumnoId())) {
                omitidos++;
                continue;
            }
            Alumno alumno = alumnoRepository.findById(d.getAlumnoId())
                    .orElseThrow(() -> NotFoundException.of("Alumno", d.getAlumnoId()));
            if (d.getAccion() == Accion.EGRESA) {
                alumno.setEstadoGlobal(EstadoAlumno.EGRESADO);
                alumnoRepository.save(alumno);
                egresaron++;
                continue;
            }
            AlumnoCicloLectivo acl = new AlumnoCicloLectivo();
            acl.setAlumno(alumno);
            acl.setCicloLectivo(nuevo);
            acl.setCursoDivision(cursoDivisionRepository.findById(d.getCursoDestinoId())
                    .orElseThrow(() -> NotFoundException.of("CursoDivision", d.getCursoDestinoId())));
            acl.setRepetidor(d.getAccion() == Accion.REPITE);
            alumnoCicloLectivoRepository.save(acl);
            if (d.getAccion() == Accion.REPITE) {
                // Recursa el año: lo que debia de ese año deja de adeudarse (las previas de años anteriores siguen).
                anterior.ifPresent(c -> mesaExamenService.anularPorRepeticion(alumno.getId(), c.getId()));
                repiten++;
            } else {
                pasaron++;
            }
        }
        auditService.registrar("CONFIGURACION", "PROMOCION_ALUMNOS", "CicloLectivo", nuevo.getId(), null,
                "pasan=%d, repiten=%d, egresan=%d".formatted(pasaron, repiten, egresaron));
        return new Resultado(pasaron, repiten, egresaron, omitidos);
    }

    /** Alumnos activos que todavia no tienen curso en el ciclo activo (aviso en el panel de gestion). */
    @Transactional(readOnly = true)
    public long alumnosSinCursoEnCicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .map(activo -> {
                    Set<Long> inscriptos = idsInscriptosEn(activo);
                    return alumnoRepository.findAll().stream()
                            .filter(a -> a.getEstadoGlobal() == EstadoAlumno.ACTIVO && !inscriptos.contains(a.getId()))
                            .count();
                })
                .orElse(0L);
    }

    private Set<Long> idsInscriptosEn(CicloLectivo ciclo) {
        return alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(ciclo.getId()).stream()
                .map(acl -> acl.getAlumno().getId())
                .collect(Collectors.toSet());
    }

    private Optional<CicloLectivo> cicloAnterior(CicloLectivo nuevo) {
        return cicloLectivoRepository.findAll().stream()
                .filter(c -> c.getAnio() < nuevo.getAnio())
                .max(Comparator.comparing(CicloLectivo::getAnio));
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }

    private String nombre(Long alumnoId) {
        return alumnoRepository.findById(alumnoId).map(Alumno::getNombreCompleto).orElse("alumno " + alumnoId);
    }
}
