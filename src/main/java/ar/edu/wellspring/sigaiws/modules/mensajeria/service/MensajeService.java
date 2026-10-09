package ar.edu.wellspring.sigaiws.modules.mensajeria.service;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.Conversacion;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ConversacionParticipante;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.Mensaje;
import ar.edu.wellspring.sigaiws.modules.mensajeria.repository.ConversacionParticipanteRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.repository.ConversacionRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.repository.MensajeRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Seccion 5.10.2/5.10.3 (Mensajeria directa y grupal). */
@Service
@RequiredArgsConstructor
public class MensajeService {

    private static final long ADJUNTO_MAX_MB = 10;
    private static final int CONTENIDO_MAX = 2000;
    private static final int NOMBRE_GRUPO_MAX = 120;

    /** Solo formatos que el navegador no ejecuta: /archivos sirve inline y un .html o .svg seria un XSS. */
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            "pdf", "jpg", "jpeg", "png", "gif", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");

    public record ConversacionResumen(Long conversacionId, String titulo, boolean grupal, String ultimoMensaje,
                                      LocalDateTime fecha, boolean noLeido) {}

    private final ConversacionRepository conversacionRepository;
    private final ConversacionParticipanteRepository participanteRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlmacenamientoService almacenamientoService;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public List<ConversacionResumen> misConversaciones(Long usuarioId) {
        return participanteRepository.findAllByUsuario_Id(usuarioId).stream()
                .map(p -> {
                    Conversacion conversacion = p.getConversacion();
                    Mensaje ultimo = mensajeRepository.findTopByConversacion_IdOrderByEnviadoEnDesc(conversacion.getId());
                    boolean noLeido = ultimo != null && !ultimo.getRemitente().getId().equals(usuarioId)
                            && (p.getUltimoLeidoEn() == null || ultimo.getEnviadoEn().isAfter(p.getUltimoLeidoEn()));
                    return new ConversacionResumen(conversacion.getId(), titulo(conversacion, usuarioId),
                            conversacion.isGrupal(), vistaPrevia(conversacion, ultimo),
                            ultimo != null ? ultimo.getEnviadoEn() : conversacion.getCreadaEn(), noLeido);
                })
                .sorted(Comparator.comparing(ConversacionResumen::fecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Nombre del grupo, o el nombre del otro participante en una conversacion directa. */
    @Transactional(readOnly = true)
    public String titulo(Conversacion conversacion, Long usuarioId) {
        if (conversacion.isGrupal()) {
            return conversacion.getNombre();
        }
        return participanteRepository.findAllByConversacion_Id(conversacion.getId()).stream()
                .map(ConversacionParticipante::getUsuario)
                .filter(u -> !u.getId().equals(usuarioId))
                .map(Usuario::getNombreCompleto)
                .findFirst().orElse(null);
    }

    private String vistaPrevia(Conversacion conversacion, Mensaje mensaje) {
        if (mensaje == null) {
            return "";
        }
        String texto = mensaje.getContenido().isBlank() && mensaje.getAdjuntoNombre() != null
                ? "📎 " + mensaje.getAdjuntoNombre()
                : mensaje.getContenido();
        return conversacion.isGrupal() ? mensaje.getRemitente().getNombreCompleto() + ": " + texto : texto;
    }

    /** Seccion 5.10.2: "Indicador de lectura". Se llama al abrir una conversacion. */
    @Transactional
    public void marcarLeido(Long conversacionId, Long usuarioId) {
        participanteDe(conversacionId, usuarioId).ifPresent(p -> {
            p.setUltimoLeidoEn(LocalDateTime.now());
            participanteRepository.save(p);
        });
        notificacionService.marcarLeidasDeEntidad(usuarioId, NotificacionService.ENTIDAD_CONVERSACION, conversacionId);
    }

    /**
     * Para cada mensaje propio, cuantos de los demas participantes ya lo vieron
     * (en una directa, 1 = "Visto"; en un grupo, "Visto por X de Y").
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> vistosPorMensaje(Long conversacionId, Long usuarioId, List<Mensaje> mensajes) {
        List<LocalDateTime> lecturasDeOtros = participanteRepository.findAllByConversacion_Id(conversacionId).stream()
                .filter(p -> !p.getUsuario().getId().equals(usuarioId))
                .map(ConversacionParticipante::getUltimoLeidoEn)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Long> vistos = new HashMap<>();
        for (Mensaje m : mensajes) {
            if (m.getRemitente().getId().equals(usuarioId)) {
                vistos.put(m.getId(), lecturasDeOtros.stream().filter(l -> !l.isBefore(m.getEnviadoEn())).count());
            }
        }
        return vistos;
    }

    @Transactional
    public Conversacion obtenerOCrearDirecta(Long usuarioAId, Long usuarioBId) {
        if (usuarioAId.equals(usuarioBId)) {
            throw new BusinessException("No podes iniciar una conversacion con vos mismo.");
        }
        // Solo se reutilizan conversaciones DIRECTAS: un grupo donde esten ambos no cuenta como chat privado.
        for (ConversacionParticipante p : participanteRepository.findAllByUsuario_Id(usuarioAId)) {
            Conversacion conversacion = p.getConversacion();
            if (!conversacion.isGrupal() && participanteDe(conversacion.getId(), usuarioBId).isPresent()) {
                return conversacion;
            }
        }

        Conversacion conversacion = new Conversacion();
        conversacion.setTipo(Conversacion.Tipo.DIRECTA);
        conversacion = conversacionRepository.save(conversacion);
        agregar(conversacion, List.of(usuarioAId, usuarioBId));
        return conversacion;
    }

    @Transactional
    public Conversacion crearGrupo(String nombre, Usuario creador, Collection<Long> participanteIds) {
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        if (nombreLimpio.isEmpty()) {
            throw new BusinessException("El grupo necesita un nombre.");
        }
        if (nombreLimpio.length() > NOMBRE_GRUPO_MAX) {
            throw new BusinessException("El nombre del grupo no puede superar los " + NOMBRE_GRUPO_MAX + " caracteres.");
        }
        List<Long> invitados = participanteIds == null ? List.of()
                : participanteIds.stream().filter(id -> !id.equals(creador.getId())).distinct().toList();
        if (invitados.isEmpty()) {
            throw new BusinessException("Elegi al menos un integrante para el grupo.");
        }

        Conversacion grupo = new Conversacion();
        grupo.setTipo(Conversacion.Tipo.GRUPAL);
        grupo.setNombre(nombreLimpio);
        grupo.setCreadoPor(creador);
        grupo = conversacionRepository.save(grupo);

        agregar(grupo, List.of(creador.getId()));
        agregar(grupo, invitados);
        avisarAgregados(grupo, creador, invitados);
        return grupo;
    }

    @Transactional
    public void agregarParticipantes(Long conversacionId, Collection<Long> usuarioIds, Usuario solicitante) {
        Conversacion grupo = grupoAdministrable(conversacionId, solicitante);
        List<Long> nuevos = usuarioIds == null ? List.of() : usuarioIds.stream()
                .distinct()
                .filter(id -> participanteDe(conversacionId, id).isEmpty())
                .toList();
        agregar(grupo, nuevos);
        avisarAgregados(grupo, solicitante, nuevos);
    }

    /** El creador puede quitar a cualquiera; cualquier integrante puede quitarse a si mismo (salir del grupo). */
    @Transactional
    public void quitarParticipante(Long conversacionId, Long usuarioId, Usuario solicitante) {
        if (!usuarioId.equals(solicitante.getId())) {
            grupoAdministrable(conversacionId, solicitante);
        } else if (!obtener(conversacionId).isGrupal()) {
            throw new BusinessException("Solo se puede salir de un grupo.");
        }
        participanteDe(conversacionId, usuarioId).ifPresent(participanteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<Usuario> participantes(Long conversacionId) {
        return participanteRepository.findAllByConversacion_Id(conversacionId).stream()
                .map(ConversacionParticipante::getUsuario)
                .sorted(Comparator.comparing(Usuario::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Devuelve null si no habia nada para enviar (ni texto ni archivo). */
    @Transactional
    public Mensaje enviarMensaje(Long conversacionId, Usuario remitente, String contenido, MultipartFile adjunto) {
        Conversacion conversacion = obtener(conversacionId);
        String texto = contenido == null ? "" : contenido.trim();
        boolean tieneAdjunto = adjunto != null && !adjunto.isEmpty();
        if (texto.isEmpty() && !tieneAdjunto) {
            return null;
        }
        if (texto.length() > CONTENIDO_MAX) {
            throw new BusinessException("El mensaje no puede superar los " + CONTENIDO_MAX + " caracteres.");
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(texto);
        if (tieneAdjunto) {
            validarExtension(adjunto.getOriginalFilename());
            var guardado = almacenamientoService.guardar(adjunto, "mensajes", ADJUNTO_MAX_MB);
            mensaje.setAdjuntoPath(guardado.pathRelativo());
            mensaje.setAdjuntoNombre(guardado.nombreOriginal());
        }
        mensaje = mensajeRepository.save(mensaje);

        avisarMensajeNuevo(conversacion, mensaje);
        return mensaje;
    }

    @Transactional(readOnly = true)
    public List<Mensaje> mensajesDe(Long conversacionId) {
        return mensajeRepository.findAllByConversacion_IdOrderByEnviadoEn(conversacionId);
    }

    @Transactional(readOnly = true)
    public boolean esParticipante(Long conversacionId, Long usuarioId) {
        return participanteDe(conversacionId, usuarioId).isPresent();
    }

    @Transactional(readOnly = true)
    public Conversacion obtener(Long conversacionId) {
        return conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> NotFoundException.of("Conversacion", conversacionId));
    }

    private void avisarMensajeNuevo(Conversacion conversacion, Mensaje mensaje) {
        String remitente = mensaje.getRemitente().getNombreCompleto();
        String titulo = conversacion.isGrupal()
                ? "Mensaje nuevo en " + conversacion.getNombre()
                : "Mensaje nuevo de " + remitente;
        String contenido = vistaPrevia(conversacion, mensaje);
        for (ConversacionParticipante p : participanteRepository.findAllByConversacion_Id(conversacion.getId())) {
            if (!p.getUsuario().getId().equals(mensaje.getRemitente().getId())) {
                notificacionService.notificarAgrupando(p.getUsuario().getId(), "MENSAJE_NUEVO", titulo, contenido,
                        NotificacionService.ENTIDAD_CONVERSACION, conversacion.getId(), "/mensajes/" + conversacion.getId());
            }
        }
    }

    private void avisarAgregados(Conversacion grupo, Usuario quienAgrega, List<Long> usuarioIds) {
        for (Long usuarioId : usuarioIds) {
            notificacionService.notificar(usuarioId, "AGREGADO_A_GRUPO", "Te agregaron al grupo " + grupo.getNombre(),
                    quienAgrega.getNombreCompleto() + " te agrego al grupo \"" + grupo.getNombre() + "\".",
                    NotificacionService.ENTIDAD_CONVERSACION, grupo.getId(), "/mensajes/" + grupo.getId());
        }
    }

    private void agregar(Conversacion conversacion, List<Long> usuarioIds) {
        for (Long usuarioId : usuarioIds) {
            ConversacionParticipante participante = new ConversacionParticipante();
            participante.setConversacion(conversacion);
            participante.setUsuario(usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> NotFoundException.of("Usuario", usuarioId)));
            participanteRepository.save(participante);
        }
    }

    private Conversacion grupoAdministrable(Long conversacionId, Usuario solicitante) {
        Conversacion conversacion = obtener(conversacionId);
        if (!conversacion.isGrupal()) {
            throw new BusinessException("Solo los grupos tienen integrantes editables.");
        }
        if (conversacion.getCreadoPor() == null || !conversacion.getCreadoPor().getId().equals(solicitante.getId())) {
            throw new BusinessException("Solo quien creo el grupo puede modificar sus integrantes.");
        }
        return conversacion;
    }

    private java.util.Optional<ConversacionParticipante> participanteDe(Long conversacionId, Long usuarioId) {
        return participanteRepository.findAllByConversacion_Id(conversacionId).stream()
                .filter(p -> p.getUsuario().getId().equals(usuarioId))
                .findFirst();
    }

    private static void validarExtension(String nombreArchivo) {
        String nombre = nombreArchivo == null ? "" : nombreArchivo;
        int punto = nombre.lastIndexOf('.');
        String extension = punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new BusinessException("Tipo de archivo no permitido. Formatos aceptados: PDF, imagenes (JPG, PNG, GIF), "
                    + "Word, Excel, PowerPoint y TXT.");
        }
    }
}
