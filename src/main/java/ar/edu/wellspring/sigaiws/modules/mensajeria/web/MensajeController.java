package ar.edu.wellspring.sigaiws.modules.mensajeria.web;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.Mensaje;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.MensajeService;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Seccion 5.10.3 (Bandeja de Mensajes). Acceso: todos los roles. Crear grupos
 * queda reservado al personal; los padres pueden integrar un grupo pero no crearlo.
 */
@Controller
@RequestMapping("/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private static final String ROLES_PERSONAL = "hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR','DOCENTE')";

    private final MensajeService mensajeService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public String bandeja(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        model.addAttribute("conversaciones", mensajeService.misConversaciones(principal.getId()));
        model.addAttribute("usuarios", otrosUsuarios(principal.getId()));
        return "mensajes/bandeja";
    }

    @PostMapping("/nueva")
    public String iniciarConversacion(@RequestParam Long destinatarioId, @AuthenticationPrincipal SigaiUserPrincipal principal) {
        var conversacion = mensajeService.obtenerOCrearDirecta(principal.getId(), destinatarioId);
        return "redirect:/mensajes/" + conversacion.getId();
    }

    @PostMapping("/grupos")
    @PreAuthorize(ROLES_PERSONAL)
    public String crearGrupo(@RequestParam String nombre, @RequestParam(required = false) List<Long> participanteIds,
                             @AuthenticationPrincipal SigaiUserPrincipal principal, RedirectAttributes redirectAttributes) {
        try {
            var grupo = mensajeService.crearGrupo(nombre, principal.getUsuario(), participanteIds);
            redirectAttributes.addFlashAttribute("mensajeExito", "Grupo creado.");
            return "redirect:/mensajes/" + grupo.getId();
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
            return "redirect:/mensajes";
        }
    }

    @GetMapping("/{id}")
    public String verConversacion(@PathVariable Long id, @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        validarParticipante(id, principal.getId());
        mensajeService.marcarLeido(id, principal.getId());

        var conversacion = mensajeService.obtener(id);
        List<Mensaje> mensajes = mensajeService.mensajesDe(id);
        List<Usuario> participantes = mensajeService.participantes(id);
        Set<Long> participanteIds = participantes.stream().map(Usuario::getId).collect(Collectors.toSet());

        model.addAttribute("conversacionId", id);
        model.addAttribute("conversacion", conversacion);
        model.addAttribute("tituloConversacion", mensajeService.titulo(conversacion, principal.getId()));
        model.addAttribute("participantes", participantes);
        model.addAttribute("otrosParticipantes", participantes.size() - 1);
        model.addAttribute("esCreador", conversacion.getCreadoPor() != null && conversacion.getCreadoPor().getId().equals(principal.getId()));
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("vistos", mensajeService.vistosPorMensaje(id, principal.getId(), mensajes));
        model.addAttribute("conversaciones", mensajeService.misConversaciones(principal.getId()));
        model.addAttribute("usuarioActualId", principal.getId());
        model.addAttribute("usuarios", otrosUsuarios(principal.getId()));
        model.addAttribute("usuariosParaAgregar", otrosUsuarios(principal.getId()).stream()
                .filter(u -> !participanteIds.contains(u.getId())).toList());
        return "mensajes/bandeja";
    }

    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable Long id, @RequestParam(required = false) String contenido,
                         @RequestParam(required = false) MultipartFile adjunto,
                         @AuthenticationPrincipal SigaiUserPrincipal principal, RedirectAttributes redirectAttributes) {
        validarParticipante(id, principal.getId());
        try {
            mensajeService.enviarMensaje(id, principal.getUsuario(), contenido, adjunto);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
        }
        return "redirect:/mensajes/" + id;
    }

    @PostMapping("/{id}/participantes")
    public String agregarParticipantes(@PathVariable Long id, @RequestParam(required = false) List<Long> usuarioIds,
                                       @AuthenticationPrincipal SigaiUserPrincipal principal, RedirectAttributes redirectAttributes) {
        validarParticipante(id, principal.getId());
        try {
            mensajeService.agregarParticipantes(id, usuarioIds, principal.getUsuario());
            redirectAttributes.addFlashAttribute("mensajeExito", "Integrantes agregados.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
        }
        return "redirect:/mensajes/" + id;
    }

    @PostMapping("/{id}/participantes/{usuarioId}/quitar")
    public String quitarParticipante(@PathVariable Long id, @PathVariable Long usuarioId,
                                     @AuthenticationPrincipal SigaiUserPrincipal principal, RedirectAttributes redirectAttributes) {
        validarParticipante(id, principal.getId());
        try {
            mensajeService.quitarParticipante(id, usuarioId, principal.getUsuario());
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
            return "redirect:/mensajes/" + id;
        }
        if (usuarioId.equals(principal.getId())) {
            redirectAttributes.addFlashAttribute("mensajeExito", "Saliste del grupo.");
            return "redirect:/mensajes";
        }
        redirectAttributes.addFlashAttribute("mensajeExito", "Integrante quitado del grupo.");
        return "redirect:/mensajes/" + id;
    }

    private void validarParticipante(Long conversacionId, Long usuarioId) {
        if (!mensajeService.esParticipante(conversacionId, usuarioId)) {
            throw new BusinessException("No tenes acceso a esta conversacion.");
        }
    }

    private List<Usuario> otrosUsuarios(Long usuarioId) {
        return usuarioRepository.buscar(null, true, null, PageRequest.of(0, 200)).getContent()
                .stream().filter(u -> !u.getId().equals(usuarioId)).toList();
    }
}
