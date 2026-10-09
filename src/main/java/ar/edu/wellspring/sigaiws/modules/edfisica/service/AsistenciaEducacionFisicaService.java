package ar.edu.wellspring.sigaiws.modules.edfisica.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.AsistenciaEducacionFisica;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.GrupoEducacionFisica;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.TipoAsistenciaEducacionFisica;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.AsistenciaEducacionFisicaBatchForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.repository.AsistenciaEducacionFisicaRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.7 (extension): asistencia diaria por grupo de Educacion Fisica, con el estado SAF. */
@Service
@RequiredArgsConstructor
public class AsistenciaEducacionFisicaService {

    private static final String MODULO = "EDUCACION_FISICA";

    private final AsistenciaEducacionFisicaRepository asistenciaRepository;
    private final AlumnoRepository alumnoRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Map<Long, TipoAsistenciaEducacionFisica> estadoDelDia(Long grupoId, LocalDate fecha) {
        Map<Long, TipoAsistenciaEducacionFisica> mapa = new HashMap<>();
        for (var registro : asistenciaRepository.findAllByGrupo_IdAndFecha(grupoId, fecha)) {
            mapa.put(registro.getAlumno().getId(), registro.getTipo());
        }
        return mapa;
    }

    @Transactional
    public void registrar(AsistenciaEducacionFisicaBatchForm form, GrupoEducacionFisica grupo, Usuario registradoPor) {
        for (AsistenciaEducacionFisicaBatchForm.Item item : form.getItems()) {
            var registro = asistenciaRepository
                    .findByAlumno_IdAndGrupo_IdAndFecha(item.getAlumnoId(), grupo.getId(), form.getFecha())
                    .orElseGet(AsistenciaEducacionFisica::new);
            registro.setAlumno(alumnoRepository.findById(item.getAlumnoId())
                    .orElseThrow(() -> NotFoundException.of("Alumno", item.getAlumnoId())));
            registro.setGrupo(grupo);
            registro.setFecha(form.getFecha());
            registro.setTipo(item.getTipo());
            registro.setRegistradoPor(registradoPor);
            asistenciaRepository.save(registro);
        }
        auditService.registrar(MODULO, "REGISTRAR_ASISTENCIA_EF", "AsistenciaEducacionFisica", grupo.getId(), null,
                "grupo=" + grupo.getNombre() + ", fecha=" + form.getFecha());
    }
}
