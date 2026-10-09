package ar.edu.wellspring.sigaiws.modules.mensajeria.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ComunicadoDestinatario;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ComunicadoDestinatario.TipoDestinatario;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ComunicadoInstitucional;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ComunicadoLectura;
import ar.edu.wellspring.sigaiws.modules.mensajeria.dto.ComunicadoForm;
import ar.edu.wellspring.sigaiws.modules.mensajeria.repository.ComunicadoDestinatarioRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.repository.ComunicadoInstitucionalRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.repository.ComunicadoLecturaRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.10.2 (Comunicados Institucionales) / 5.10.4. */
@Service
@RequiredArgsConstructor
public class ComunicadoService {

    private final ComunicadoInstitucionalRepository comunicadoRepository;
    private final ComunicadoDestinatarioRepository destinatarioRepository;
    private final ComunicadoLecturaRepository lecturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final EmailService emailService;
    private final NotificacionService notificacionService;
    private final AlmacenamientoService almacenamientoService;
    private final AuditService auditService;

    public record ResumenLectura(long leidos, long audiencia) {
    }

    @Transactional(readOnly = true)
    public List<ComunicadoInstitucional> listar() {
        return comunicadoRepository.findAllByOrderByCreadoEnDesc();
    }

    @Transactional(readOnly = true)
    public ComunicadoInstitucional obtener(Long id) {
        return comunicadoRepository.findById(id).orElseThrow(() -> NotFoundException.of("ComunicadoInstitucional", id));
    }

    @Transactional
    public ComunicadoInstitucional crear(ComunicadoForm form, Usuario autor) {
        ComunicadoInstitucional comunicado = new ComunicadoInstitucional();
        comunicado.setAsunto(form.getAsunto());
        comunicado.setContenido(form.getContenido());
        comunicado.setAutor(autor);
        comunicado.setEnviarEmail(form.isEnviarEmail());
        if (form.getAdjunto() != null && !form.getAdjunto().isEmpty()) {
            var guardado = almacenamientoService.guardar(form.getAdjunto(), "comunicados", 5);
            comunicado.setAdjuntoPath(guardado.pathRelativo());
        }
        comunicado = comunicadoRepository.save(comunicado);

        // Seccion 5.10.4: destinatarios combinables (ej. "Todos los padres" + "Todos los docentes" en el mismo envio).
        for (ComunicadoDestinatario.TipoDestinatario tipo : form.getTiposDestinatario()) {
            ComunicadoDestinatario destinatario = new ComunicadoDestinatario();
            destinatario.setComunicado(comunicado);
            destinatario.setTipoDestinatario(tipo);
            if (tipo == ComunicadoDestinatario.TipoDestinatario.CURSO && form.getCursoDivisionId() != null) {
                destinatario.setCursoDivision(cursoDivisionRepository.findById(form.getCursoDivisionId())
                        .orElseThrow(() -> NotFoundException.of("CursoDivision", form.getCursoDivisionId())));
            }
            if (tipo == ComunicadoDestinatario.TipoDestinatario.ALUMNO && form.getAlumnoId() != null) {
                destinatario.setAlumno(alumnoTutorRepository.findAllByAlumno_Id(form.getAlumnoId()).stream()
                        .findFirst().map(at -> at.getAlumno()).orElse(null));
            }
            destinatarioRepository.save(destinatario);

            if (form.isEnviarEmail()) {
                enviarPorEmail(form, tipo);
            }
        }
        notificarDestinatarios(comunicado, form.getTiposDestinatario());
        auditService.registrar("MENSAJERIA", "CREAR_COMUNICADO", "ComunicadoInstitucional", comunicado.getId(), null, form.getAsunto());
        return comunicado;
    }

    /**
     * Seccion 5.10.2: aviso en la campanita a cada destinatario. Los padres van al comunicado en su
     * portal; los docentes no tienen pantalla de comunicados, asi que la notificacion lleva el texto completo.
     */
    private void notificarDestinatarios(ComunicadoInstitucional comunicado, java.util.Collection<TipoDestinatario> tipos) {
        String titulo = "Nuevo comunicado: " + comunicado.getAsunto();
        for (Long usuarioId : audienciaPadresDe(comunicado.getId())) {
            notificacionService.notificar(usuarioId, "COMUNICADO", titulo, comunicado.getContenido(),
                    NotificacionService.ENTIDAD_COMUNICADO, comunicado.getId(), "/portal-padres/comunicados/" + comunicado.getId());
        }
        if (tipos != null && tipos.contains(TipoDestinatario.TODOS_DOCENTES)) {
            usuarioRepository.buscar(null, true, null, PageRequest.of(0, 1000)).getContent().stream()
                    .filter(u -> u.tieneRol(RolNombre.DOCENTE))
                    .forEach(u -> notificacionService.notificar(u.getId(), "COMUNICADO", titulo, comunicado.getContenido(),
                            NotificacionService.ENTIDAD_COMUNICADO, comunicado.getId(), null));
        }
    }

    private void enviarPorEmail(ComunicadoForm form, ComunicadoDestinatario.TipoDestinatario tipo) {
        List<String> destinatarios = switch (tipo) {
            case TODOS_DOCENTES -> usuarioRepository.buscar(null, true, null, PageRequest.of(0, 500)).getContent().stream()
                    .filter(u -> u.tieneRol(RolNombre.DOCENTE)).map(Usuario::getEmail).toList();
            case TODOS_PADRES -> usuarioRepository.buscar(null, true, null, PageRequest.of(0, 500)).getContent().stream()
                    .filter(u -> u.tieneRol(RolNombre.PADRE_TUTOR)).map(Usuario::getEmail).toList();
            case CURSO -> form.getCursoDivisionId() == null ? List.of() : destinatariosDeCurso(form.getCursoDivisionId());
            case ALUMNO -> form.getAlumnoId() == null ? List.of()
                    : alumnoTutorRepository.findAllByAlumno_Id(form.getAlumnoId()).stream()
                        .map(at -> at.getTutor().getEmail()).toList();
        };
        for (String email : destinatarios) {
            emailService.enviarHtml(email, form.getAsunto(), "<p>" + form.getContenido() + "</p>");
        }
    }

    @Transactional(readOnly = true)
    public List<ComunicadoDestinatario> destinatariosDe(Long comunicadoId) {
        return destinatarioRepository.findAllByComunicado_Id(comunicadoId);
    }

    /** Comunicados visibles para un padre/tutor (seccion 5.11): union de TODOS_PADRES, su(s) curso(s) y sus hijos. */
    @Transactional(readOnly = true)
    public List<ComunicadoInstitucional> comunicadosParaPadre(Long usuarioId) {
        List<AlumnoTutor> vinculos = alumnoTutorRepository.findAllByTutor_UsuarioId(usuarioId);
        List<Long> alumnoIds = vinculos.stream().map(v -> v.getAlumno().getId()).distinct().toList();

        var cicloActivo = cicloLectivoRepository.findByActivoTrue();
        Set<Long> divisionIds = new HashSet<>();
        if (cicloActivo.isPresent()) {
            for (Long alumnoId : alumnoIds) {
                alumnoCicloLectivoRepository.findByAlumno_IdAndCicloLectivo_Id(alumnoId, cicloActivo.get().getId())
                        .ifPresent(acl -> divisionIds.add(acl.getCursoDivision().getId()));
            }
        }

        Set<Long> comunicadoIds = new HashSet<>();
        destinatarioRepository.findAllByTipoDestinatario(TipoDestinatario.TODOS_PADRES)
                .forEach(d -> comunicadoIds.add(d.getComunicado().getId()));
        if (!divisionIds.isEmpty()) {
            destinatarioRepository.findAllByTipoDestinatarioAndCursoDivision_IdIn(TipoDestinatario.CURSO, divisionIds)
                    .forEach(d -> comunicadoIds.add(d.getComunicado().getId()));
        }
        if (!alumnoIds.isEmpty()) {
            destinatarioRepository.findAllByTipoDestinatarioAndAlumno_IdIn(TipoDestinatario.ALUMNO, alumnoIds)
                    .forEach(d -> comunicadoIds.add(d.getComunicado().getId()));
        }
        if (comunicadoIds.isEmpty()) {
            return List.of();
        }
        return comunicadoRepository.findAllByIdInOrderByCreadoEnDesc(comunicadoIds);
    }

    /** Solo un padre al que va dirigido el comunicado puede verlo/marcarlo leido. */
    @Transactional(readOnly = true)
    public boolean esVisibleParaPadre(Long comunicadoId, Long usuarioId) {
        return comunicadosParaPadre(usuarioId).stream().anyMatch(c -> c.getId().equals(comunicadoId));
    }

    /** Registra la primera vez que este usuario abre el comunicado; aperturas posteriores no reescriben la fecha/hora. */
    @Transactional
    public void marcarLeido(Long comunicadoId, Long usuarioId) {
        if (lecturaRepository.findByComunicado_IdAndUsuario_Id(comunicadoId, usuarioId).isPresent()) {
            return;
        }
        ComunicadoLectura lectura = new ComunicadoLectura();
        lectura.setComunicado(obtener(comunicadoId));
        lectura.setUsuario(usuarioRepository.findById(usuarioId).orElseThrow(() -> NotFoundException.of("Usuario", usuarioId)));
        lecturaRepository.save(lectura);
        auditService.registrar("MENSAJERIA", "LEER_COMUNICADO", "ComunicadoInstitucional", comunicadoId, null, null);
    }

    @Transactional(readOnly = true)
    public List<ComunicadoLectura> lecturasDe(Long comunicadoId) {
        return lecturaRepository.findAllByComunicado_IdOrderByLeidoEnAsc(comunicadoId);
    }

    /** Cuentas de padres/tutores destinatarias del comunicado (para comparar contra quienes ya lo leyeron). */
    @Transactional(readOnly = true)
    public Set<Long> audienciaPadresDe(Long comunicadoId) {
        var cicloActivo = cicloLectivoRepository.findByActivoTrue();
        Set<Long> usuarioIds = new HashSet<>();
        for (ComunicadoDestinatario d : destinatarioRepository.findAllByComunicado_Id(comunicadoId)) {
            switch (d.getTipoDestinatario()) {
                case TODOS_PADRES -> usuarioRepository.buscar(null, true, null, PageRequest.of(0, 1000)).getContent().stream()
                        .filter(u -> u.tieneRol(RolNombre.PADRE_TUTOR)).map(Usuario::getId).forEach(usuarioIds::add);
                case CURSO -> {
                    if (d.getCursoDivision() != null && cicloActivo.isPresent()) {
                        alumnoCicloLectivoRepository.findAllByCursoDivision_IdAndCicloLectivo_IdOrderByAlumno_ApellidoAscAlumno_NombreAsc(
                                        d.getCursoDivision().getId(), cicloActivo.get().getId())
                                .stream()
                                .flatMap(acl -> alumnoTutorRepository.findAllByAlumno_Id(acl.getAlumno().getId()).stream())
                                .map(at -> at.getTutor().getUsuarioId())
                                .filter(Objects::nonNull)
                                .forEach(usuarioIds::add);
                    }
                }
                case ALUMNO -> {
                    if (d.getAlumno() != null) {
                        alumnoTutorRepository.findAllByAlumno_Id(d.getAlumno().getId()).stream()
                                .map(at -> at.getTutor().getUsuarioId())
                                .filter(Objects::nonNull)
                                .forEach(usuarioIds::add);
                    }
                }
                case TODOS_DOCENTES -> {
                    // no aplica al seguimiento de lectura de padres
                }
            }
        }
        return usuarioIds;
    }

    @Transactional(readOnly = true)
    public ResumenLectura resumenLectura(Long comunicadoId) {
        return new ResumenLectura(lecturaRepository.countByComunicado_Id(comunicadoId), audienciaPadresDe(comunicadoId).size());
    }

    /** Padres destinatarios que todavia no abrieron el comunicado. */
    @Transactional(readOnly = true)
    public List<Usuario> pendientesDe(Long comunicadoId) {
        Set<Long> audiencia = new HashSet<>(audienciaPadresDe(comunicadoId));
        for (ComunicadoLectura lectura : lecturasDe(comunicadoId)) {
            audiencia.remove(lectura.getUsuario().getId());
        }
        return audiencia.isEmpty() ? List.of() : usuarioRepository.findAllById(audiencia);
    }

    private List<String> destinatariosDeCurso(Long cursoDivisionId) {
        var cicloActivo = cicloLectivoRepository.findByActivoTrue();
        if (cicloActivo.isEmpty()) {
            return List.of();
        }
        return alumnoCicloLectivoRepository
                .findAllByCursoDivision_IdAndCicloLectivo_IdOrderByAlumno_ApellidoAscAlumno_NombreAsc(cursoDivisionId, cicloActivo.get().getId())
                .stream()
                .flatMap(acl -> alumnoTutorRepository.findAllByAlumno_Id(acl.getAlumno().getId()).stream())
                .map(at -> at.getTutor().getEmail())
                .distinct()
                .toList();
    }
}
