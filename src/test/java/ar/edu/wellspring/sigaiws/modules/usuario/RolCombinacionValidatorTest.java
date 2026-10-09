package ar.edu.wellspring.sigaiws.modules.usuario;

import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.ADMINISTRATIVO;
import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.DIRECTIVO;
import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.DOCENTE;
import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.PADRE_TUTOR;
import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.PRECEPTOR;
import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.SUPERADMIN;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.usuario.service.RolCombinacionValidator;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Valida la regla de negocio de la seccion 5.12.3 ("Preceptor + Administrativo,
 * Administrativo + Preceptor, Directivo + Administrativo, Directivo + Preceptor.
 * Un docente no puede combinarse con otros roles operativos por diseño") tal
 * como fue interpretada en docs/DECISIONS.md ADR-005.
 */
class RolCombinacionValidatorTest {

    private final RolCombinacionValidator validator = new RolCombinacionValidator();

    @Test
    void unUnicoRolSiempreEsValido() {
        assertThatCode(() -> validator.validar(Set.of(DOCENTE))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validar(Set.of(SUPERADMIN))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validar(Set.of(PADRE_TUTOR))).doesNotThrowAnyException();
    }

    @Test
    void lasTresCombinacionesDocumentadasSonValidas() {
        assertThatCode(() -> validator.validar(Set.of(PRECEPTOR, ADMINISTRATIVO))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validar(Set.of(DIRECTIVO, ADMINISTRATIVO))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validar(Set.of(DIRECTIVO, PRECEPTOR))).doesNotThrowAnyException();
    }

    @Test
    void docenteNoPuedeCombinarseConNingunOtroRol() {
        assertThatThrownBy(() -> validator.validar(Set.of(DOCENTE, PRECEPTOR)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void superadminNoPuedeCombinarseConNingunOtroRol() {
        assertThatThrownBy(() -> validator.validar(Set.of(SUPERADMIN, ADMINISTRATIVO)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void combinacionDeLosTresRolesOperativosJuntosNoEstaDocumentadaYSeRechaza() {
        assertThatThrownBy(() -> validator.validar(Set.of(DIRECTIVO, ADMINISTRATIVO, PRECEPTOR)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void conjuntoVacioSeRechaza() {
        assertThatThrownBy(() -> validator.validar(Set.of()))
                .isInstanceOf(BusinessException.class);
    }
}
