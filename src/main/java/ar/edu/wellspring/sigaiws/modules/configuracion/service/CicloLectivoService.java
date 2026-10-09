package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.CicloLectivoForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.event.CicloLectivoActivadoEvent;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.14.2: "activar el ciclo lectivo actual (solo puede haber uno
 * activo)". La activacion de un nuevo ciclo archiva el anterior y publica
 * {@link CicloLectivoActivadoEvent} para que el modulo Alumnos reinicie los
 * contadores de asistencia/disciplina de cada alumno activo.
 */
@Service
@RequiredArgsConstructor
public class CicloLectivoService {

    private static final String MODULO = "CONFIGURACION";

    private final CicloLectivoRepository cicloLectivoRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CicloLectivo> listar() {
        return cicloLectivoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CicloLectivo obtener(Long id) {
        return cicloLectivoRepository.findById(id).orElseThrow(() -> NotFoundException.of("Ciclo lectivo", id));
    }

    @Transactional(readOnly = true)
    public Optional<CicloLectivo> obtenerActivo() {
        return cicloLectivoRepository.findByActivoTrue();
    }

    @Transactional
    public CicloLectivo crear(CicloLectivoForm form) {
        if (cicloLectivoRepository.existsByAnio(form.getAnio())) {
            throw new BusinessException("Ya existe un ciclo lectivo para el año " + form.getAnio() + ".");
        }
        if (!form.getFechaInicioC1().isBefore(form.getFechaFinC1())
                || !form.getFechaInicioC2().isBefore(form.getFechaFinC2())
                || !form.getFechaFinC1().isBefore(form.getFechaInicioC2())) {
            throw new BusinessException("Las fechas de los cuatrimestres no son consistentes.");
        }

        CicloLectivo ciclo = new CicloLectivo();
        ciclo.setAnio(form.getAnio());
        ciclo.setFechaInicioC1(form.getFechaInicioC1());
        ciclo.setFechaFinC1(form.getFechaFinC1());
        ciclo.setFechaInicioC2(form.getFechaInicioC2());
        ciclo.setFechaFinC2(form.getFechaFinC2());
        ciclo.setEstado(CicloLectivo.Estado.PLANIFICADO);
        ciclo.setActivo(false);

        ciclo = cicloLectivoRepository.save(ciclo);
        auditService.registrar(MODULO, "CREAR_CICLO_LECTIVO", "CicloLectivo", ciclo.getId(), null, ciclo.getAnio());
        return ciclo;
    }

    @Transactional
    public void activar(Long id) {
        CicloLectivo nuevo = obtener(id);
        if (nuevo.isActivo()) {
            return;
        }

        Long anteriorId = null;
        Optional<CicloLectivo> anteriorOpt = cicloLectivoRepository.findByActivoTrue();
        if (anteriorOpt.isPresent()) {
            CicloLectivo anterior = anteriorOpt.get();
            anterior.setActivo(false);
            anterior.setEstado(CicloLectivo.Estado.ARCHIVADO);
            // Flush antes de activar el nuevo: el indice unico ux_ciclo_lectivo_activo no admite dos activos ni un instante.
            cicloLectivoRepository.saveAndFlush(anterior);
            anteriorId = anterior.getId();
        }

        nuevo.setActivo(true);
        nuevo.setEstado(CicloLectivo.Estado.ACTIVO);
        nuevo.setCuatrimestreActivo(1);
        cicloLectivoRepository.save(nuevo);

        auditService.registrar(MODULO, "ACTIVAR_CICLO_LECTIVO", "CicloLectivo", nuevo.getId(), anteriorId, nuevo.getAnio());
        eventPublisher.publishEvent(new CicloLectivoActivadoEvent(anteriorId, nuevo.getId()));
    }

    @Transactional
    public void cambiarCuatrimestre(Long id, int cuatrimestre) {
        if (cuatrimestre != 1 && cuatrimestre != 2) {
            throw new BusinessException("El cuatrimestre debe ser 1 o 2.");
        }
        CicloLectivo ciclo = obtener(id);
        if (!ciclo.isActivo()) {
            throw new BusinessException("Solo se puede cambiar el cuatrimestre del ciclo lectivo activo.");
        }
        ciclo.setCuatrimestreActivo(cuatrimestre);
        cicloLectivoRepository.save(ciclo);
        auditService.registrar(MODULO, "CAMBIAR_CUATRIMESTRE", "CicloLectivo", ciclo.getId(), null, cuatrimestre);
    }
}
