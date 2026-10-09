package ar.edu.wellspring.sigaiws.modules.alumno.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.DuplicateResourceException;
import ar.edu.wellspring.sigaiws.common.util.HashUtil;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Tutor;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.TutorForm;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.TutorRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.dto.UsuarioForm;
import ar.edu.wellspring.sigaiws.modules.usuario.service.UsuarioService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.1.5: "Si el tutor ya existe en el sistema (por DNI), se autocompleta
 * su informacion y se vinculara sin duplicar." Seccion 6.1 paso 6: "Si el tutor
 * es nuevo, el sistema crea automaticamente su cuenta de acceso al portal y le
 * envia email con credenciales" - reutiliza UsuarioService para no duplicar la
 * logica de generacion de password temporal / envio de bienvenida.
 */
@Service
@RequiredArgsConstructor
public class TutorService {

    private static final String MODULO = "ALUMNOS";

    private final TutorRepository tutorRepository;
    private final UsuarioService usuarioService;
    private final AuditService auditService;

    /** Seccion 5.1.5: autocompletado en vivo al cargar el DNI de un tutor en el formulario de alumno. */
    @Transactional(readOnly = true)
    public java.util.Optional<Tutor> buscarPorDni(String dni) {
        return tutorRepository.findByDniHash(HashUtil.sha256(dni));
    }

    /** Busca por DNI; si existe, actualiza sus datos de contacto. Si no, crea el tutor y su cuenta de portal. */
    @Transactional
    public Tutor resolverOCrear(TutorForm form) {
        String dniHash = HashUtil.sha256(form.getDni());
        return tutorRepository.findByDniHash(dniHash)
                .map(existente -> actualizarContacto(existente, form))
                .orElseGet(() -> crearNuevo(form, dniHash));
    }

    private Tutor actualizarContacto(Tutor tutor, TutorForm form) {
        tutor.setTelefono(form.getTelefono());
        if (!tutor.getEmail().equalsIgnoreCase(form.getEmail())
                && tutorRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new DuplicateResourceException("email", "Ya existe otro tutor con ese email.");
        }
        tutor.setEmail(form.getEmail().toLowerCase());
        return tutorRepository.save(tutor);
    }

    private Tutor crearNuevo(TutorForm form, String dniHash) {
        if (tutorRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new DuplicateResourceException("email", "Ya existe un tutor con ese email.");
        }

        Tutor tutor = new Tutor();
        tutor.setApellido(form.getApellido());
        tutor.setNombre(form.getNombre());
        tutor.setDni(form.getDni());
        tutor.setDniHash(dniHash);
        tutor.setTelefono(form.getTelefono());
        tutor.setEmail(form.getEmail().toLowerCase());
        tutor = tutorRepository.save(tutor);

        UsuarioForm cuentaPortal = new UsuarioForm();
        cuentaPortal.setNombreCompleto(form.getApellido() + " " + form.getNombre());
        cuentaPortal.setEmail(form.getEmail());
        cuentaPortal.setRoles(Set.of(RolNombre.PADRE_TUTOR));
        Usuario usuarioPortal = usuarioService.crear(cuentaPortal);

        tutor.setUsuarioId(usuarioPortal.getId());
        tutor = tutorRepository.save(tutor);

        auditService.registrar(MODULO, "CREAR_TUTOR", "Tutor", tutor.getId(), null,
                "nombre=" + tutor.getNombreCompleto());
        return tutor;
    }
}
