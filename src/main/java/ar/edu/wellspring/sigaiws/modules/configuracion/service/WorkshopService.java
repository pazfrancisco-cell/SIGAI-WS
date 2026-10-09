package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Workshop;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.WorkshopForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.WorkshopRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogo base de workshops (seccion 5.14.2). La logica de asignacion de
 * alumnos y cambio obligatorio C1->C2 (seccion 5.6) se construye en la Fase 4.
 */
@Service
@RequiredArgsConstructor
public class WorkshopService {

    private static final String MODULO = "CONFIGURACION";

    private final WorkshopRepository workshopRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Workshop> listar() {
        return workshopRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Workshop obtener(Long id) {
        return workshopRepository.findById(id).orElseThrow(() -> NotFoundException.of("Workshop", id));
    }

    @Transactional
    public Workshop guardar(WorkshopForm form) {
        Workshop workshop = form.getId() != null ? obtener(form.getId()) : new Workshop();
        workshop.setNombre(form.getNombre());
        workshop.setDescripcion(form.getDescripcion());
        workshop.setBanda(form.getBanda());
        workshop.setActivo(form.isActivo());
        workshop = workshopRepository.save(workshop);
        auditService.registrar(MODULO, form.getId() == null ? "CREAR_WORKSHOP" : "EDITAR_WORKSHOP",
                "Workshop", workshop.getId(), null, workshop.getNombre());
        return workshop;
    }
}
