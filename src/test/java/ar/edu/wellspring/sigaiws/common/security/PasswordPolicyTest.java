package ar.edu.wellspring.sigaiws.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

/** Seccion 7.1: minimo 8 caracteres, 1 mayuscula, 1 numero, 1 caracter especial. */
class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(8);

    @Test
    void aceptaUnaContraseñaQueCumpleTodasLasReglas() {
        assertThatCode(() -> policy.validar("Wellspring#2026")).doesNotThrowAnyException();
    }

    @Test
    void rechazaContraseñaMuyCorta() {
        assertThatThrownBy(() -> policy.validar("Ab1#"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void rechazaContraseñaSinMayuscula() {
        assertThatThrownBy(() -> policy.validar("wellspring#2026"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mayuscula");
    }

    @Test
    void rechazaContraseñaSinNumero() {
        assertThatThrownBy(() -> policy.validar("Wellspring#Anio"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("numero");
    }

    @Test
    void rechazaContraseñaSinCaracterEspecial() {
        assertThatThrownBy(() -> policy.validar("Wellspring2026"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("especial");
    }
}
