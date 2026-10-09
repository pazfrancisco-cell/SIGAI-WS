package ar.edu.wellspring.sigaiws.modules.alumno.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.DuplicateResourceException;
import ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService;
import ar.edu.wellspring.sigaiws.common.util.HashUtil;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.AlumnoForm;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.DocumentoAlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Seccion 5.1.5 (Formulario Nuevo Alumno) y 5.1 en general. */
@ExtendWith(MockitoExtension.class)
class AlumnoServiceTest {

    @Mock private AlumnoRepository alumnoRepository;
    @Mock private AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    @Mock private AlumnoTutorRepository alumnoTutorRepository;
    @Mock private DocumentoAlumnoRepository documentoAlumnoRepository;
    @Mock private CicloLectivoRepository cicloLectivoRepository;
    @Mock private CursoDivisionRepository cursoDivisionRepository;
    @Mock private TutorService tutorService;
    @Mock private AlmacenamientoService almacenamientoService;
    @Mock private AuditService auditService;

    private AlumnoService alumnoService;

    @BeforeEach
    void setUp() {
        alumnoService = new AlumnoService(alumnoRepository, alumnoCicloLectivoRepository, alumnoTutorRepository,
                documentoAlumnoRepository, cicloLectivoRepository, cursoDivisionRepository, tutorService,
                almacenamientoService, auditService);
    }

    @Test
    void rechazaFechaDeNacimientoFutura() {
        AlumnoForm form = formularioValido();
        form.setFechaNacimiento(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> alumnoService.crear(form))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("futura");
    }

    @Test
    void rechazaFechaDeNacimientoQueImplicaMasDe25Anios() {
        AlumnoForm form = formularioValido();
        form.setFechaNacimiento(LocalDate.now().minusYears(26));

        assertThatThrownBy(() -> alumnoService.crear(form))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("25");
    }

    @Test
    void rechazaAltaConDniYaRegistrado() {
        AlumnoForm form = formularioValido();
        when(alumnoRepository.existsByDniHash(HashUtil.sha256(form.getDni()))).thenReturn(true);

        assertThatThrownBy(() -> alumnoService.crear(form)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void creaElAlumnoYSuTrayectoriaDelCicloDeIngreso() {
        AlumnoForm form = formularioValido();
        CicloLectivo ciclo = new CicloLectivo();
        ciclo.setId(10L);
        ciclo.setAnio(2026);
        CursoDivision cursoDivision = new CursoDivision(5L, 1, "A");

        when(alumnoRepository.existsByDniHash(any())).thenReturn(false);
        when(cicloLectivoRepository.findById(10L)).thenReturn(Optional.of(ciclo));
        when(cursoDivisionRepository.findByAnioAndDivision(1, "A")).thenReturn(Optional.of(cursoDivision));
        lenient().when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> {
            Alumno a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        Alumno creado = alumnoService.crear(form);

        assertThat(creado.getApellido()).isEqualTo("Gonzalez");
        assertThat(creado.getEstadoGlobal()).isEqualTo(EstadoAlumno.ACTIVO);
        verify(alumnoCicloLectivoRepository).save(any());
        verify(auditService).registrar(anyString(), anyString(), anyString(), any(), any(), any());
    }

    private AlumnoForm formularioValido() {
        AlumnoForm form = new AlumnoForm();
        form.setApellido("Gonzalez");
        form.setNombre("Julian");
        form.setDni("30123456");
        form.setFechaNacimiento(LocalDate.now().minusYears(15));
        form.setNacionalidad("Argentina");
        form.setDomicilio("Calle Falsa 123");
        form.setLocalidad("Ciudad Autonoma de Buenos Aires");
        form.setAnio(1);
        form.setDivision("A");
        form.setEstado(EstadoAlumno.ACTIVO);
        form.setCicloLectivoIngresoId(10L);
        form.setTutores(new ArrayList<>());
        return form;
    }
}
