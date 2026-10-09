package ar.edu.wellspring.sigaiws.modules.usuario.web;

import ar.edu.wellspring.sigaiws.common.audit.AuditLogRepository;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.deshacer.service.DeshacerService;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.dto.UsuarioForm;
import ar.edu.wellspring.sigaiws.modules.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Seccion 5.12.4: gestion de usuarios, acceso exclusivo Superadmin.
 * Vistas Thymeleaf server-side (seccion 3.3): el controller solo orquesta,
 * la logica de negocio vive en UsuarioService.
 */
@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final DeshacerService deshacerService;
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public String listar(@RequestParam(required = false) String texto,
                          @RequestParam(defaultValue = "false") boolean soloActivos,
                          @RequestParam(required = false) RolNombre rol,
                          @RequestParam(defaultValue = "0") int pagina,
                          Model model) {
        Page<Usuario> resultado = usuarioService.buscar(texto, soloActivos, rol, PageRequest.of(pagina, 25));
        model.addAttribute("usuarios", resultado);
        model.addAttribute("texto", texto);
        model.addAttribute("soloActivos", soloActivos);
        model.addAttribute("rolSeleccionado", rol);
        model.addAttribute("roles", RolNombre.values());
        return "usuarios/list";
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("usuarioForm", new UsuarioForm());
        return "usuarios/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute UsuarioForm usuarioForm, BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "usuarios/form";
        }
        Usuario creado = usuarioService.crear(usuarioForm);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Usuario " + creado.getNombreCompleto() + " creado correctamente. Se enviaron las credenciales por email.");
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtener(id);
        UsuarioForm form = new UsuarioForm();
        form.setId(usuario.getId());
        form.setNombreCompleto(usuario.getNombreCompleto());
        form.setEmail(usuario.getEmail());
        form.setActivo(usuario.isActivo());
        form.setRoles(usuario.getRoles().stream().map(r -> r.getNombre()).collect(java.util.stream.Collectors.toSet()));
        form.setEsDocenteEducacionFisica(usuario.isEsDocenteEducacionFisica());
        form.setEsDocenteWorkshop(usuario.isEsDocenteWorkshop());
        model.addAttribute("usuarioForm", form);
        return "usuarios/form";
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute UsuarioForm usuarioForm,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "usuarios/form";
        }
        usuarioService.editar(id, usuarioForm);
        redirectAttributes.addFlashAttribute("mensajeExito", "Usuario actualizado correctamente.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/resetear-password")
    public String resetearPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        usuarioService.resetearPassword(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Se genero una nueva contraseña temporal y se envio por email.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        usuarioService.activar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Usuario activado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirectAttributes,
                              @AuthenticationPrincipal SigaiUserPrincipal principal) {
        usuarioService.desactivar(id);
        String undoId = deshacerService.registrar(DeshacerService.Tipo.DESACTIVAR_USUARIO, id, principal.getId());
        redirectAttributes.addFlashAttribute("mensajeExito", "Usuario desactivado.");
        redirectAttributes.addFlashAttribute("undoId", undoId);
        return "redirect:/usuarios";
    }

    /** "Ver historial de accesos y acciones (logs)" - seccion 5.12.4. */
    @GetMapping("/{id}/accesos")
    public String verAccesos(@PathVariable Long id, @RequestParam(defaultValue = "0") int pagina, Model model) {
        Usuario usuario = usuarioService.obtener(id);
        model.addAttribute("usuario", usuario);
        model.addAttribute("registros", auditLogRepository.buscar(id, null, null, null, null, PageRequest.of(pagina, 25)));
        return "usuarios/accesos";
    }
}
