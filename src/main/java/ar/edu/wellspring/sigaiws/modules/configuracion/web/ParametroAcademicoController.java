package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.configuracion.dto.ParametroAcademicoForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.ParametroAcademicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion/parametros-academicos")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
public class ParametroAcademicoController {

    private final ParametroAcademicoService service;

    @GetMapping
    public String ver(Model model) {
        var parametro = service.obtener();
        ParametroAcademicoForm form = new ParametroAcademicoForm();
        form.setUmbralFalta1(parametro.getUmbralFalta1());
        form.setUmbralFalta2(parametro.getUmbralFalta2());
        form.setUmbralFalta3(parametro.getUmbralFalta3());
        form.setUmbralFalta4CondicionLibre(parametro.getUmbralFalta4CondicionLibre());
        form.setNotaMinimaAprobacion(parametro.getNotaMinimaAprobacion());
        form.setUmbralBajoRendimiento(parametro.getUmbralBajoRendimiento());
        form.setPorcentajeAsistenciaMateria(parametro.getPorcentajeAsistenciaMateria());
        model.addAttribute("parametroAcademicoForm", form);
        return "configuracion/parametros-academicos";
    }

    @PostMapping
    public String actualizar(@Valid @ModelAttribute ParametroAcademicoForm form, BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "configuracion/parametros-academicos";
        }
        service.actualizar(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Parametros academicos actualizados.");
        return "redirect:/configuracion/parametros-academicos";
    }
}
