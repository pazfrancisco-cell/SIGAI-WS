package ar.edu.wellspring.sigaiws.modules.alerta.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.repository.AlertaRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Modulo 5.13. Punto unico de creacion de alertas, invocado desde Asistencia, Disciplina, Calificaciones, etc. */
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final AuditService auditService;

    @Transactional
    public Alerta crear(String tipo, Alerta.Prioridad prioridad, Alumno alumno, String descripcion) {
        return crear(tipo, prioridad, alumno, descripcion, null, null);
    }

    /** Igual que {@link #crear(String, Alerta.Prioridad, Alumno, String)}, vinculando la alerta a lo que la genero. */
    @Transactional
    public Alerta crear(String tipo, Alerta.Prioridad prioridad, Alumno alumno, String descripcion,
                        String entidad, Long entidadId) {
        Alerta alerta = new Alerta();
        alerta.setTipo(tipo);
        alerta.setPrioridad(prioridad);
        alerta.setAlumno(alumno);
        alerta.setDescripcion(descripcion);
        alerta.setEntidadRelacionada(entidad);
        alerta.setEntidadRelacionadaId(entidadId);
        alerta = alertaRepository.save(alerta);
        auditService.registrar("ALERTAS", "GENERAR_ALERTA", "Alerta", alerta.getId(), null, tipo);
        return alerta;
    }

    /**
     * Si ya hay una alerta activa del mismo tipo para el mismo alumno y entidad, se actualiza su
     * descripcion (y queda primera por fecha) en vez de crear otra. Devuelve true si la creo.
     */
    @Transactional
    public boolean crearOActualizar(String tipo, Alerta.Prioridad prioridad, Alumno alumno, String descripcion,
                                    String entidad, Long entidadId) {
        var existente = alertaRepository.findFirstByTipoAndAlumno_IdAndEntidadRelacionadaAndEntidadRelacionadaIdAndEstadoNot(
                tipo, alumno.getId(), entidad, entidadId, Alerta.Estado.RESUELTA);
        if (existente.isEmpty()) {
            crear(tipo, prioridad, alumno, descripcion, entidad, entidadId);
            return true;
        }
        Alerta alerta = existente.get();
        alerta.setDescripcion(descripcion);
        alerta.setGeneradaEn(LocalDateTime.now());
        alertaRepository.save(alerta);
        return false;
    }

    /** Para avisos que deben existir una unica vez (aunque despues se resuelvan). */
    @Transactional(readOnly = true)
    public boolean yaGenerada(String tipo, String entidad, Long entidadId) {
        return alertaRepository.existsByTipoAndEntidadRelacionadaAndEntidadRelacionadaId(tipo, entidad, entidadId);
    }

    /** Cierra automaticamente la alerta activa vinculada a una entidad (ej. una solicitud ya respondida). */
    @Transactional
    public void resolverPorEntidad(String tipo, String entidad, Long entidadId, String notaResolucion, Usuario resolutor) {
        alertaRepository.findFirstByTipoAndEntidadRelacionadaAndEntidadRelacionadaIdAndEstadoNot(
                        tipo, entidad, entidadId, Alerta.Estado.RESUELTA)
                .ifPresent(alerta -> {
                    alerta.setEstado(Alerta.Estado.RESUELTA);
                    alerta.setNotaResolucion(notaResolucion);
                    alerta.setResueltaEn(LocalDateTime.now());
                    alerta.setResueltaPor(resolutor);
                    alertaRepository.save(alerta);
                    auditService.registrar("ALERTAS", "RESOLVER_ALERTA", "Alerta", alerta.getId(), null, notaResolucion);
                });
    }

    @Transactional(readOnly = true)
    public List<Alerta> activas() {
        List<Alerta> nuevas = alertaRepository.findAllByEstadoOrderByPrioridadDescGeneradaEnDesc(Alerta.Estado.NUEVA);
        List<Alerta> enSeguimiento = alertaRepository.findAllByEstadoOrderByPrioridadDescGeneradaEnDesc(Alerta.Estado.EN_SEGUIMIENTO);
        nuevas.addAll(enSeguimiento);
        return nuevas;
    }

    /** Seccion 5.13.3: Centro de Alertas con filtro opcional por prioridad y/o tipo. */
    @Transactional(readOnly = true)
    public List<Alerta> activas(Alerta.Prioridad prioridad, String tipo) {
        return activas().stream()
                .filter(a -> prioridad == null || a.getPrioridad() == prioridad)
                .filter(a -> tipo == null || tipo.isBlank() || tipo.equals(a.getTipo()))
                .toList();
    }

    /** Tipos de alerta presentes entre las activas, para el selector de filtro (seccion 5.13.3). */
    @Transactional(readOnly = true)
    public List<String> tiposDeAlertasActivas() {
        return activas().stream().map(Alerta::getTipo).distinct().sorted().toList();
    }

    @Transactional(readOnly = true)
    public List<Alerta> historial() {
        return alertaRepository.findAllByOrderByGeneradaEnDesc();
    }

    @Transactional
    public void marcarEnSeguimiento(Long id) {
        Alerta alerta = obtener(id);
        alerta.setEstado(Alerta.Estado.EN_SEGUIMIENTO);
        alertaRepository.save(alerta);
    }

    @Transactional
    public void resolver(Long id, String notaResolucion) {
        Alerta alerta = obtener(id);
        alerta.setEstado(Alerta.Estado.RESUELTA);
        alerta.setNotaResolucion(notaResolucion);
        alerta.setResueltaEn(LocalDateTime.now());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal principal) {
            Usuario u = new Usuario();
            u.setId(principal.getId());
            alerta.setResueltaPor(u);
        }
        alertaRepository.save(alerta);
        auditService.registrar("ALERTAS", "RESOLVER_ALERTA", "Alerta", alerta.getId(), null, notaResolucion);
    }

    private Alerta obtener(Long id) {
        return alertaRepository.findById(id).orElseThrow(() -> NotFoundException.of("Alerta", id));
    }
}
