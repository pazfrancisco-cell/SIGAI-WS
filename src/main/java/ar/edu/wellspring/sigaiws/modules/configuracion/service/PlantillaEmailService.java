package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.PlantillaEmail;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.PlantillaEmailRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlantillaEmailService {

    public record Renderizada(String asunto, String cuerpo) {}

    private final PlantillaEmailRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PlantillaEmail> listar() {
        return repository.findAllByOrderByDescripcion();
    }

    @Transactional(readOnly = true)
    public PlantillaEmail obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("PlantillaEmail", id));
    }

    @Transactional
    public void guardar(Long id, String asunto, String cuerpo, boolean habilitada) {
        PlantillaEmail plantilla = obtener(id);
        plantilla.setAsunto(asunto);
        plantilla.setCuerpo(cuerpo);
        plantilla.setHabilitada(habilitada);
        plantilla.setActualizadoEn(LocalDateTime.now());
        repository.save(plantilla);
        auditService.registrar("CONFIGURACION", "EDITAR_PLANTILLA_EMAIL", "PlantillaEmail", id, null, plantilla.getTipo());
    }

    @Transactional(readOnly = true)
    public Optional<Renderizada> renderizar(String tipo, Map<String, String> variables) {
        return repository.findByTipo(tipo)
                .filter(PlantillaEmail::isHabilitada)
                .map(p -> new Renderizada(sustituir(p.getAsunto(), variables), sustituir(p.getCuerpo(), variables)));
    }

    private String sustituir(String texto, Map<String, String> variables) {
        String resultado = texto;
        for (var entrada : variables.entrySet()) {
            resultado = resultado.replace("{" + entrada.getKey() + "}", entrada.getValue() != null ? entrada.getValue() : "");
        }
        return resultado;
    }
}
