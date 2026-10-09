package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.MateriaForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.14.2 ("Gestion de materias: lista de materias activas por año/curso, agregar/editar/desactivar"). */
@Service
@RequiredArgsConstructor
public class MateriaService {

    private static final String MODULO = "CONFIGURACION";

    private final MateriaRepository materiaRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Materia> listar() {
        return materiaRepository.findAllByOrderByNombre();
    }

    @Transactional(readOnly = true)
    public Materia obtener(Long id) {
        return materiaRepository.findById(id).orElseThrow(() -> NotFoundException.of("Materia", id));
    }

    @Transactional
    public Materia guardar(MateriaForm form) {
        Materia materia = form.getId() != null ? obtener(form.getId()) : new Materia();
        materia.setNombre(form.getNombre());
        materia.setActiva(form.isActiva());
        materia.setCursosDivisiones(resolverCursos(form.getCursoDivisionIds()));
        materia = materiaRepository.save(materia);
        auditService.registrar(MODULO, form.getId() == null ? "CREAR_MATERIA" : "EDITAR_MATERIA",
                "Materia", materia.getId(), null, materia.getNombre());
        return materia;
    }

    private Set<CursoDivision> resolverCursos(Set<Long> ids) {
        Set<CursoDivision> resultado = new HashSet<>();
        for (Long id : ids) {
            resultado.add(cursoDivisionRepository.findById(id)
                    .orElseThrow(() -> NotFoundException.of("CursoDivision", id)));
        }
        return resultado;
    }
}
