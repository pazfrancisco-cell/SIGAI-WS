package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.ParametroAcademico;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.ParametroAcademicoForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.ParametroAcademicoRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fila unica (sembrada por V13). Se cachea a nivel de request via el repositorio
 * de JPA (first-level cache); los modulos de Asistencia/Calificaciones que
 * consuman estos umbrales deben inyectar este service en lugar de hardcodear
 * los valores por defecto de la seccion 5.2.3/5.3.2.
 */
@Service
@RequiredArgsConstructor
public class ParametroAcademicoService {

    private static final String MODULO = "CONFIGURACION";

    private final ParametroAcademicoRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ParametroAcademico obtener() {
        return repository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("parametro_academico no sembrado (V13 no se aplico)"));
    }

    @Transactional
    public ParametroAcademico actualizar(ParametroAcademicoForm form) {
        ParametroAcademico parametro = obtener();
        parametro.setUmbralFalta1(form.getUmbralFalta1());
        parametro.setUmbralFalta2(form.getUmbralFalta2());
        parametro.setUmbralFalta3(form.getUmbralFalta3());
        parametro.setUmbralFalta4CondicionLibre(form.getUmbralFalta4CondicionLibre());
        parametro.setNotaMinimaAprobacion(form.getNotaMinimaAprobacion());
        parametro.setUmbralBajoRendimiento(form.getUmbralBajoRendimiento());
        parametro.setPorcentajeAsistenciaMateria(form.getPorcentajeAsistenciaMateria());
        parametro.setActualizadoEn(LocalDateTime.now());
        parametro = repository.save(parametro);
        auditService.registrar(MODULO, "ACTUALIZAR_PARAMETROS", "ParametroAcademico", parametro.getId(), null, null);
        return parametro;
    }
}
