package ar.edu.wellspring.sigaiws.modules.deshacer.web;

import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.deshacer.service.DeshacerService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeshacerController {

    private final DeshacerService deshacerService;

    @PostMapping("/deshacer/{uuid}")
    @ResponseBody
    public Map<String, Boolean> deshacer(@PathVariable String uuid, @AuthenticationPrincipal SigaiUserPrincipal principal) {
        boolean ok = deshacerService.deshacer(uuid, principal.getId());
        return Map.of("ok", ok);
    }
}
