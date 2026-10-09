package ar.edu.wellspring.sigaiws.integration;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prueba de extremo a extremo contra un SQL Server real (via Testcontainers):
 * valida que las migraciones Flyway (V1-V13) se aplican limpiamente sobre el
 * motor real (no solo sobre una base compatible como H2, cuyo dialecto T-SQL
 * difiere), que el mapeo JPA coincide con el esquema, y que el cifrado de
 * columnas sensibles (seccion 7.2) realmente oculta el dato en la fila cruda.
 *
 * Requiere Docker disponible localmente. El sufijo "IT" es intencional: el
 * patron de inclusion por defecto de Maven Surefire (**{@code /*Test.java})
 * NO incluye clases *IT.java, por lo que `mvn test` las omite automaticamente
 * y no exige Docker. Ver docs/DEV_GUIDE.md para como ejecutarlas explicitamente.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AlumnoPersistenciaIT {

    @Container
    static MSSQLServerContainer<?> sqlServer = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
    }

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private CicloLectivoRepository cicloLectivoRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void elDniSePersisteCifradoEnLaColumnaCrudaPeroSeLeeDescifradoAlAccederPorJpa() {
        CicloLectivo ciclo = new CicloLectivo();
        ciclo.setAnio(2026);
        ciclo.setFechaInicioC1(LocalDate.of(2026, 3, 1));
        ciclo.setFechaFinC1(LocalDate.of(2026, 7, 1));
        ciclo.setFechaInicioC2(LocalDate.of(2026, 8, 1));
        ciclo.setFechaFinC2(LocalDate.of(2026, 12, 1));
        ciclo = cicloLectivoRepository.save(ciclo);

        Alumno alumno = new Alumno();
        alumno.setApellido("Perez");
        alumno.setNombre("Ana");
        alumno.setDni("30999888");
        alumno.setDniHash(ar.edu.wellspring.sigaiws.common.util.HashUtil.sha256("30999888"));
        alumno.setFechaNacimiento(LocalDate.now().minusYears(14));
        alumno.setDomicilio("Av. Siempre Viva 742");
        alumno.setLocalidad("CABA");
        alumno.setCicloLectivoIngreso(ciclo);
        Alumno guardado = alumnoRepository.save(alumno);

        // 1) Via JPA (converter aplicado): se ve el valor en claro.
        Alumno recargado = alumnoRepository.findById(guardado.getId()).orElseThrow();
        assertThat(recargado.getDni()).isEqualTo("30999888");
        assertThat(recargado.getDomicilio()).isEqualTo("Av. Siempre Viva 742");

        // 2) Via SQL crudo (bypass de Hibernate): la columna NO debe contener el DNI en claro.
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String dniCrudo = jdbcTemplate.queryForObject(
                "SELECT dni_cifrado FROM alumno WHERE id = ?", String.class, guardado.getId());
        assertThat(dniCrudo).isNotEqualTo("30999888");
        assertThat(dniCrudo).doesNotContain("30999888");
    }
}
