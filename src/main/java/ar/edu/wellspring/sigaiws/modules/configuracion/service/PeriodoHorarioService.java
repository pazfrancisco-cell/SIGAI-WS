package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.PeriodoHorarioForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.PeriodoHorarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.14.2: estructura del dia escolar, editable por el Superadmin. */
@Service
@RequiredArgsConstructor
public class PeriodoHorarioService {

    private static final String MODULO = "CONFIGURACION";

    private final PeriodoHorarioRepository periodoHorarioRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PeriodoHorario> listar() {
        return periodoHorarioRepository.findAllByOrderByOrden();
    }

    @Transactional(readOnly = true)
    public PeriodoHorario obtener(Long id) {
        return periodoHorarioRepository.findById(id).orElseThrow(() -> NotFoundException.of("PeriodoHorario", id));
    }

    @Transactional
    public PeriodoHorario guardar(PeriodoHorarioForm form) {
        if (!form.getHoraFin().isAfter(form.getHoraInicio())) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        PeriodoHorario periodo = form.getId() != null ? obtener(form.getId()) : new PeriodoHorario();
        periodo.setOrden(form.getOrden());
        periodo.setNombre(form.getNombre());
        periodo.setHoraInicio(form.getHoraInicio());
        periodo.setHoraFin(form.getHoraFin());
        periodo.setTipo(form.getTipo());
        periodo = periodoHorarioRepository.save(periodo);
        auditService.registrar(MODULO, form.getId() == null ? "CREAR_PERIODO_HORARIO" : "EDITAR_PERIODO_HORARIO",
                "PeriodoHorario", periodo.getId(), null, periodo.getNombre());
        return periodo;
    }

    @Transactional
    public void eliminar(Long id) {
        PeriodoHorario periodo = obtener(id);
        periodoHorarioRepository.delete(periodo);
        auditService.registrar(MODULO, "ELIMINAR_PERIODO_HORARIO", "PeriodoHorario", id, periodo.getNombre(), null);
    }
}
