package ar.edu.wellspring.sigaiws.modules.mensajeria.service;

import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.NotificacionSistema;
import ar.edu.wellspring.sigaiws.modules.mensajeria.repository.NotificacionSistemaRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.10.2 / 5.11.4: notificaciones personales del sistema (campanita del
 * topbar, para todos los roles). Solo depende de repositorios para que cualquier
 * modulo pueda usarlo sin generar dependencias circulares.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    public static final String ENTIDAD_CONVERSACION = "CONVERSACION";
    public static final String ENTIDAD_COMUNICADO = "COMUNICADO";
    public static final String ENTIDAD_ALUMNO = "ALUMNO";

    private static final int MAX_LISTADO = 100;

    private final NotificacionSistemaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;

    @Transactional
    public void notificar(Long usuarioId, String tipo, String titulo, String contenido,
                          String entidad, Long entidadId, String enlace) {
        NotificacionSistema notificacion = new NotificacionSistema();
        notificacion.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        completar(notificacion, tipo, titulo, contenido, entidad, entidadId, enlace);
        repository.save(notificacion);
    }

    /**
     * Para eventos que se repiten seguido (ej. mensajes de una misma conversacion): si el usuario
     * ya tiene una notificacion sin leer del mismo tipo sobre la misma entidad, se actualiza en vez
     * de apilar otra.
     */
    @Transactional
    public void notificarAgrupando(Long usuarioId, String tipo, String titulo, String contenido,
                                   String entidad, Long entidadId, String enlace) {
        var existente = repository.findFirstByUsuario_IdAndTipoAndEntidadRelacionadaAndEntidadRelacionadaIdAndLeidaFalse(
                usuarioId, tipo, entidad, entidadId);
        if (existente.isEmpty()) {
            notificar(usuarioId, tipo, titulo, contenido, entidad, entidadId, enlace);
            return;
        }
        NotificacionSistema notificacion = existente.get();
        completar(notificacion, tipo, titulo, contenido, entidad, entidadId, enlace);
        notificacion.setCreadoEn(LocalDateTime.now());
        repository.save(notificacion);
    }

    /** Avisa en el portal a los tutores del alumno que tienen cuenta y aceptan notificaciones. */
    @Transactional
    public void notificarTutores(Alumno alumno, String tipo, String titulo, String contenido) {
        for (AlumnoTutor vinculo : alumnoTutorRepository.findAllByAlumno_Id(alumno.getId())) {
            Long usuarioId = vinculo.getTutor().getUsuarioId();
            if (vinculo.isRecibeNotificaciones() && usuarioId != null) {
                notificar(usuarioId, tipo, titulo, contenido, ENTIDAD_ALUMNO, alumno.getId(),
                        "/portal-padres/alumno/" + alumno.getId());
            }
        }
    }

    /** Para avisos que se mandan una unica vez por usuario (ej. "faltan 7 dias para el cierre"). */
    @Transactional(readOnly = true)
    public boolean yaNotificado(Long usuarioId, String tipo, String entidad, Long entidadId) {
        return repository.existsByUsuario_IdAndTipoAndEntidadRelacionadaAndEntidadRelacionadaId(usuarioId, tipo, entidad, entidadId);
    }

    @Transactional(readOnly = true)
    public List<NotificacionSistema> deUsuario(Long usuarioId) {
        return repository.findAllByUsuario_IdOrderByCreadoEnDesc(usuarioId, PageRequest.of(0, MAX_LISTADO));
    }

    @Transactional(readOnly = true)
    public long noLeidas(Long usuarioId) {
        return repository.countByUsuario_IdAndLeidaFalse(usuarioId);
    }

    /** Marca como leida una notificacion propia y devuelve a donde lleva (null si no tiene destino). */
    @Transactional
    public String abrir(Long notificacionId, Long usuarioId) {
        NotificacionSistema notificacion = repository.findById(notificacionId)
                .filter(n -> n.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> NotFoundException.of("NotificacionSistema", notificacionId));
        notificacion.setLeida(true);
        repository.save(notificacion);
        return notificacion.getEnlace();
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        List<NotificacionSistema> pendientes = repository.findAllByUsuario_IdAndLeidaFalse(usuarioId);
        pendientes.forEach(n -> n.setLeida(true));
        repository.saveAll(pendientes);
    }

    /** Ej.: al abrir una conversacion se dan por vistas sus notificaciones de mensaje nuevo. */
    @Transactional
    public void marcarLeidasDeEntidad(Long usuarioId, String entidad, Long entidadId) {
        List<NotificacionSistema> pendientes = repository
                .findAllByUsuario_IdAndEntidadRelacionadaAndEntidadRelacionadaIdAndLeidaFalse(usuarioId, entidad, entidadId);
        pendientes.forEach(n -> n.setLeida(true));
        repository.saveAll(pendientes);
    }

    private static void completar(NotificacionSistema n, String tipo, String titulo, String contenido,
                                  String entidad, Long entidadId, String enlace) {
        n.setTipo(tipo);
        n.setTitulo(recortar(titulo, 200));
        n.setContenido(recortar(contenido == null ? "" : contenido, 1000));
        n.setEntidadRelacionada(entidad);
        n.setEntidadRelacionadaId(entidadId);
        n.setEnlace(enlace);
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }
}
