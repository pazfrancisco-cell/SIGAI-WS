package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.DuplicateResourceException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Aula;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.AulaForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.AulaRepository;
import ar.edu.wellspring.sigaiws.modules.horario.repository.HorarioSlotRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.5.1 ("Administracion de aulas"): CRUD minimo inferido, ver docs/DECISIONS.md ADR-007. */
@Service
@RequiredArgsConstructor
public class AulaService {

    private static final String MODULO = "CONFIGURACION";

    private final AulaRepository aulaRepository;
    private final HorarioSlotRepository horarioSlotRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Aula> listar() {
        return aulaRepository.findAllByOrderByNombre();
    }

    @Transactional(readOnly = true)
    public Aula obtener(Long id) {
        return aulaRepository.findById(id).orElseThrow(() -> NotFoundException.of("Aula", id));
    }

    @Transactional
    public Aula guardar(AulaForm form) {
        Aula aula;
        if (form.getId() != null) {
            aula = obtener(form.getId());
            if (!aula.getNombre().equalsIgnoreCase(form.getNombre()) && aulaRepository.existsByNombreIgnoreCase(form.getNombre())) {
                throw new DuplicateResourceException("nombre", "Ya existe un aula con ese nombre.");
            }
        } else {
            if (aulaRepository.existsByNombreIgnoreCase(form.getNombre())) {
                throw new DuplicateResourceException("nombre", "Ya existe un aula con ese nombre.");
            }
            aula = new Aula();
        }
        aula.setNombre(form.getNombre());
        aula.setCapacidad(form.getCapacidad());
        aula.setActiva(form.isActiva());
        aula = aulaRepository.save(aula);
        auditService.registrar(MODULO, form.getId() == null ? "CREAR_AULA" : "EDITAR_AULA", "Aula", aula.getId(), null, aula.getNombre());
        return aula;
    }

    @Transactional
    public void eliminar(Long id) {
        Aula aula = obtener(id);
        if (horarioSlotRepository.existsByAula_Id(id)) {
            throw new BusinessException("No se puede eliminar el aula '" + aula.getNombre()
                    + "' porque tiene horarios asignados. Quitala de esos horarios primero, o desactivala en su lugar.");
        }
        aulaRepository.delete(aula);
        auditService.registrar(MODULO, "ELIMINAR_AULA", "Aula", id, aula.getNombre(), null);
    }
}
