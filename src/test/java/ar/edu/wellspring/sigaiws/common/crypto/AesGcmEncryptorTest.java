package ar.edu.wellspring.sigaiws.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AesGcmEncryptorTest {

    private static final String CLAVE_VALIDA = "YzPrhiM6xTTOeBf9tJ+SqULGkV85MTPg9G+jMYZK0ww=";

    @Test
    void cifraYDescifraDevolviendoElValorOriginal() {
        AesGcmEncryptor encryptor = new AesGcmEncryptor(CLAVE_VALIDA);

        String cifrado = encryptor.encrypt("30123456");

        assertThat(cifrado).isNotEqualTo("30123456");
        assertThat(encryptor.decrypt(cifrado)).isEqualTo("30123456");
    }

    @Test
    void cifrarDosVecesElMismoValorProduceSalidasDistintas() {
        // El IV es aleatorio en cada llamada (seccion 7.2): dos DNIs iguales no
        // deben ser distinguibles a simple vista en la columna cifrada.
        AesGcmEncryptor encryptor = new AesGcmEncryptor(CLAVE_VALIDA);

        String primero = encryptor.encrypt("30123456");
        String segundo = encryptor.encrypt("30123456");

        assertThat(primero).isNotEqualTo(segundo);
        assertThat(encryptor.decrypt(primero)).isEqualTo(encryptor.decrypt(segundo));
    }

    @Test
    void valorNuloSePreservaComoNulo() {
        AesGcmEncryptor encryptor = new AesGcmEncryptor(CLAVE_VALIDA);

        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    void sinClaveConfiguradaFallaAlIntentarCifrar() {
        AesGcmEncryptor encryptor = new AesGcmEncryptor("");

        assertThat(encryptor.isConfigured()).isFalse();
        assertThatThrownBy(() -> encryptor.encrypt("dato"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SIGAI_AES_KEY");
    }
}
