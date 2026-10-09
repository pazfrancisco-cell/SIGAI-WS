package ar.edu.wellspring.sigaiws.modules.calificacion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.MateriaAdeudada;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.NotaMesa;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.TurnoMesa;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.MateriaAdeudadaRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.NotaMesaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.horario.domain.AsignacionDocente;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materias adeudadas y mesas de examen. Al cerrar el año, cada materia con
 * promedio anual desaprobado queda adeudada; se rinde en las mesas de diciembre
 * y febrero y, si el alumno pasa de año debiendola, en las mesas siguientes
 * (julio, diciembre, febrero) hasta aprobarla. Se aprueba con la misma nota
 * minima que el año. La carga la hace el docente de la materia (o gestion).
 */
@Service
@RequiredArgsConstructor
public class MesaExamenService {

    private static final String MODULO = "CALIFICACIONES";
    public static final String NOTIFICACION_MESA = "RESULTADO_MESA";

    private final MateriaAdeudadaRepository adeudadaRepository;
    private final NotaMesaRepository notaMesaRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final MateriaRepository materiaRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final CalificacionService calificacionService;
    private final NotificacionService notificacionService;
    private final AuditService auditService;

    public record Sincronizacion(int nuevas, int actualizadas, int quitadas) {}

    public record ResultadoCarga(int cargadas, int aprobadas, int borradas) {}

    /** Una fila del formulario de la mesa. */
    @Getter
    @Setter
    public static class Carga {
        private Long adeudadaId;
        private BigDecimal nota;
        private boolean ausente;
    }

    @Getter
    @Setter
    public static class CargaForm {
        private TurnoMesa turno;
        private LocalDate fecha;
        private List<Carga> cargas = new ArrayList<>();
    }

    // ---------------------------------------------------------------- generacion

    /**
     * Genera (o pone al dia) las materias adeudadas de un ciclo a partir de los
     * promedios anuales. Idempotente: se llama al cerrar el 2° cuatrimestre, al
     * activar el ciclo siguiente y desde la pantalla de mesas. Una adeudada que
     * todavia no se rindio se actualiza si cambiaron las notas (o se quita si ya
     * no corresponde); las que ya tienen mesas cargadas no se tocan.
     */
    @Transactional
    public Sincronizacion sincronizarCiclo(CicloLectivo ciclo) {
        List<Materia> materias = materiaRepository.findAllByActivaTrueOrderByNombre();
        int nuevas = 0;
        int actualizadas = 0;
        int quitadas = 0;
        for (AlumnoCicloLectivo acl : alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(ciclo.getId())) {
            EstadoAlumno estado = acl.getAlumno().getEstadoGlobal();
            if (estado == EstadoAlumno.INACTIVO_BAJA || estado == EstadoAlumno.INACTIVO_PASE) {
                continue;
            }
            Long alumnoId = acl.getAlumno().getId();
            for (Materia materia : materias) {
                if (!materia.getCursosDivisiones().contains(acl.getCursoDivision())) {
                    continue;
                }
                BigDecimal promedio = calificacionService.promedioAnual(alumnoId, materia.getId(), ciclo.getId());
                boolean desaprobada = promedio != null && !calificacionService.estaAprobada(promedio);
                Optional<MateriaAdeudada> existente = adeudadaRepository
                        .findByAlumno_IdAndMateria_IdAndCicloLectivo_Id(alumnoId, materia.getId(), ciclo.getId());

                if (existente.isEmpty()) {
                    if (desaprobada) {
                        MateriaAdeudada nueva = new MateriaAdeudada();
                        nueva.setAlumno(acl.getAlumno());
                        nueva.setMateria(materia);
                        nueva.setCicloLectivo(ciclo);
                        nueva.setCursoDivision(acl.getCursoDivision());
                        nueva.setPromedioAnual(promedio);
                        adeudadaRepository.save(nueva);
                        nuevas++;
                    }
                    continue;
                }
                MateriaAdeudada adeudada = existente.get();
                if (!adeudada.isPendiente() || notaMesaRepository.existsByMateriaAdeudada_Id(adeudada.getId())) {
                    continue;
                }
                if (!desaprobada) {
                    adeudadaRepository.delete(adeudada);
                    quitadas++;
                } else if (adeudada.getPromedioAnual() == null || adeudada.getPromedioAnual().compareTo(promedio) != 0) {
                    adeudada.setPromedioAnual(promedio);
                    adeudadaRepository.save(adeudada);
                    actualizadas++;
                }
            }
        }
        if (nuevas + actualizadas + quitadas > 0) {
            auditService.registrar(MODULO, "SINCRONIZAR_ADEUDADAS", "CicloLectivo", ciclo.getId(), null,
                    "nuevas=%d, actualizadas=%d, quitadas=%d".formatted(nuevas, actualizadas, quitadas));
        }
        return new Sincronizacion(nuevas, actualizadas, quitadas);
    }

    // ---------------------------------------------------------------- consulta

    /** Materias adeudadas que el usuario puede calificar en esa mesa (el docente, solo las de sus materias y años). */
    @Transactional(readOnly = true)
    public List<MateriaAdeudada> paraMesa(TurnoMesa turno, int anio, Long materiaId, Integer anioCurso, Usuario usuario) {
        List<MateriaAdeudada> todas = adeudadaRepository.paraMesa(turno, anio, materiaId, anioCurso);
        if (esGestion(usuario)) {
            return todas;
        }
        Set<String> permitidas = materiaYAnioDelDocente(usuario);
        return todas.stream().filter(a -> permitidas.contains(clave(a))).toList();
    }

    /** Materias para el filtro de la pantalla: todas para gestion, las asignadas para el docente. */
    @Transactional(readOnly = true)
    public List<Materia> materiasDisponibles(Usuario usuario) {
        if (esGestion(usuario)) {
            return materiaRepository.findAllByActivaTrueOrderByNombre();
        }
        return asignacionesActivas(usuario).stream()
                .map(AsignacionDocente::getMateria)
                .distinct()
                .sorted(Comparator.comparing(Materia::getNombre))
                .toList();
    }

    /** Nota de la mesa ya cargada para cada adeudada (para mostrarla al corregir). */
    @Transactional(readOnly = true)
    public Optional<NotaMesa> notaCargada(Long adeudadaId, TurnoMesa turno, int anio) {
        return notaMesaRepository.findByMateriaAdeudada_IdAndTurnoAndAnio(adeudadaId, turno, anio);
    }

    @Transactional(readOnly = true)
    public List<MateriaAdeudada> deAlumno(Long alumnoId) {
        List<MateriaAdeudada> lista = adeudadaRepository.deAlumno(alumnoId);
        lista.forEach(a -> a.getNotasMesa().size());
        return lista;
    }

    @Transactional(readOnly = true)
    public List<MateriaAdeudada> pendientesDe(Long alumnoId) {
        return adeudadaRepository.findAllByAlumno_IdAndEstado(alumnoId, MateriaAdeudada.Estado.PENDIENTE);
    }

    // ---------------------------------------------------------------- carga

    @Transactional
    public ResultadoCarga cargar(CargaForm form, Usuario usuario) {
        if (form.getTurno() == null) {
            throw new BusinessException("Elegi el turno de la mesa.");
        }
        LocalDate fecha = form.getFecha() == null ? LocalDate.now() : form.getFecha();
        if (fecha.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de la mesa no puede ser futura.");
        }
        Set<String> permitidas = esGestion(usuario) ? null : materiaYAnioDelDocente(usuario);

        List<String> errores = new ArrayList<>();
        for (Carga c : form.getCargas()) {
            if (c.getNota() != null && !c.isAusente()
                    && (c.getNota().compareTo(BigDecimal.ONE) < 0 || c.getNota().compareTo(BigDecimal.TEN) > 0
                        || c.getNota().stripTrailingZeros().scale() > 1)) {
                errores.add(adeudadaRepository.findById(c.getAdeudadaId())
                        .map(a -> a.getAlumno().getNombreCompleto()).orElse("fila " + c.getAdeudadaId()));
            }
        }
        if (!errores.isEmpty()) {
            throw new BusinessException("No se guardo nada. Las notas van de 1 a 10 con un decimal como maximo: revisa "
                    + String.join(", ", errores) + ".");
        }

        int cargadas = 0;
        int aprobadas = 0;
        int borradas = 0;
        for (Carga c : form.getCargas()) {
            MateriaAdeudada adeudada = adeudadaRepository.findById(c.getAdeudadaId()).orElse(null);
            if (adeudada == null || adeudada.getEstado() == MateriaAdeudada.Estado.RECURSADA) {
                continue;
            }
            if (permitidas != null && !permitidas.contains(clave(adeudada))) {
                throw new BusinessException("No tenes asignada " + adeudada.getDescripcion() + ".");
            }
            Optional<NotaMesa> existente = notaMesaRepository.findByMateriaAdeudada_IdAndTurnoAndAnio(
                    adeudada.getId(), form.getTurno(), fecha.getYear());
            boolean vacia = c.getNota() == null && !c.isAusente();
            if (vacia) {
                if (existente.isPresent()) {
                    notaMesaRepository.delete(existente.get());
                    notaMesaRepository.flush();
                    recalcularEstado(adeudada);
                    borradas++;
                }
                continue;
            }
            NotaMesa nota = existente.orElseGet(NotaMesa::new);
            boolean cambio = existente.isEmpty() || !mismoResultado(nota, c);
            nota.setMateriaAdeudada(adeudada);
            nota.setTurno(form.getTurno());
            nota.setAnio(fecha.getYear());
            nota.setFecha(fecha);
            nota.setAusente(c.isAusente());
            nota.setNota(c.isAusente() ? null : c.getNota());
            nota.setCargadoPor(usuario);
            nota.setCargadoEn(java.time.LocalDateTime.now());
            notaMesaRepository.saveAndFlush(nota);
            boolean estabaAprobada = adeudada.getEstado() == MateriaAdeudada.Estado.APROBADA;
            recalcularEstado(adeudada);
            cargadas++;
            if (!estabaAprobada && adeudada.getEstado() == MateriaAdeudada.Estado.APROBADA) {
                aprobadas++;
            }
            if (cambio) {
                avisarFamilia(adeudada, nota);
            }
        }
        if (cargadas + borradas > 0) {
            auditService.registrar(MODULO, "CARGAR_MESA", "NotaMesa", null, null,
                    "turno=%s %d, cargadas=%d, aprobadas=%d, borradas=%d".formatted(form.getTurno(), fecha.getYear(), cargadas, aprobadas, borradas));
        }
        return new ResultadoCarga(cargadas, aprobadas, borradas);
    }

    /** Aprobada si alguna mesa tiene nota de aprobacion (la primera que la aprobo); si no, pendiente. */
    private void recalcularEstado(MateriaAdeudada adeudada) {
        Optional<NotaMesa> aprobo = notaMesaRepository.findAllByMateriaAdeudada_IdOrderByFechaAsc(adeudada.getId()).stream()
                .filter(n -> n.getNota() != null && calificacionService.estaAprobada(n.getNota()))
                .findFirst();
        if (aprobo.isPresent()) {
            adeudada.setEstado(MateriaAdeudada.Estado.APROBADA);
            adeudada.setNotaAprobacion(aprobo.get().getNota());
            adeudada.setAprobadaEn(aprobo.get().getFecha());
            adeudada.setTurnoAprobacion(aprobo.get().getTurno());
        } else {
            adeudada.setEstado(MateriaAdeudada.Estado.PENDIENTE);
            adeudada.setNotaAprobacion(null);
            adeudada.setAprobadaEn(null);
            adeudada.setTurnoAprobacion(null);
        }
        adeudadaRepository.save(adeudada);
    }

    private void avisarFamilia(MateriaAdeudada adeudada, NotaMesa nota) {
        String turno = "mesa de " + nota.getTurno().name().toLowerCase() + " " + nota.getAnio();
        String resultado;
        if (nota.isAusente()) {
            resultado = "estuvo ausente; la materia sigue adeudada";
        } else if (calificacionService.estaAprobada(nota.getNota())) {
            resultado = "aprobo con " + nota.getNota();
        } else {
            resultado = "no aprobo (" + nota.getNota() + "); la materia sigue adeudada";
        }
        notificacionService.notificarTutores(adeudada.getAlumno(), NOTIFICACION_MESA,
                "Resultado de " + turno,
                adeudada.getAlumno().getNombreCompleto() + " rindio " + adeudada.getDescripcion() + ": " + resultado + ".");
    }

    private static boolean mismoResultado(NotaMesa nota, Carga c) {
        if (nota.isAusente() || c.isAusente()) {
            return nota.isAusente() == c.isAusente();
        }
        return nota.getNota() != null && c.getNota() != null && nota.getNota().compareTo(c.getNota()) == 0;
    }

    // ---------------------------------------------------------------- promocion

    /** Al repetir, el alumno recursa el año: las materias que debia de ese año dejan de contar como adeudadas. */
    @Transactional
    public int anularPorRepeticion(Long alumnoId, Long cicloLectivoId) {
        int anuladas = 0;
        for (MateriaAdeudada a : adeudadaRepository.findAllByAlumno_IdAndCicloLectivo_Id(alumnoId, cicloLectivoId)) {
            if (a.isPendiente()) {
                a.setEstado(MateriaAdeudada.Estado.RECURSADA);
                adeudadaRepository.save(a);
                anuladas++;
            }
        }
        return anuladas;
    }

    // ---------------------------------------------------------------- permisos

    public static boolean esGestion(Usuario usuario) {
        return usuario.tieneRol(RolNombre.SUPERADMIN) || usuario.tieneRol(RolNombre.DIRECTIVO)
                || usuario.tieneRol(RolNombre.ADMINISTRATIVO);
    }

    /** "materiaId-año" de cada materia que el docente dicta en el ciclo activo (cualquier division de ese año). */
    private Set<String> materiaYAnioDelDocente(Usuario usuario) {
        return asignacionesActivas(usuario).stream()
                .map(a -> a.getMateria().getId() + "-" + a.getCursoDivision().getAnio())
                .collect(Collectors.toSet());
    }

    private List<AsignacionDocente> asignacionesActivas(Usuario usuario) {
        return cicloLectivoRepository.findByActivoTrue()
                .map(c -> asignacionDocenteRepository.findAllByDocente_IdAndCicloLectivo_Id(usuario.getId(), c.getId()))
                .orElse(List.of());
    }

    private static String clave(MateriaAdeudada a) {
        return a.getMateria().getId() + "-" + a.getCursoDivision().getAnio();
    }
}
