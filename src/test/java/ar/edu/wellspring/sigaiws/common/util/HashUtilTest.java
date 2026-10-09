package ar.edu.wellspring.sigaiws.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashUtilTest {

    @Test
    void elMismoValorSiempreProduceElMismoHash() {
        assertThat(HashUtil.sha256("30123456")).isEqualTo(HashUtil.sha256("30123456"));
    }

    @Test
    void valoresDistintosProducenHashesDistintos() {
        assertThat(HashUtil.sha256("30123456")).isNotEqualTo(HashUtil.sha256("30123457"));
    }

    @Test
    void esInsensibleAMayusculasYEspaciosParaPermitirBusquedaConsistente() {
        assertThat(HashUtil.sha256(" 30123456 ")).isEqualTo(HashUtil.sha256("30123456"));
    }

    @Test
    void nuncaDevuelveElValorOriginalEnElHash() {
        String hash = HashUtil.sha256("30123456");
        assertThat(hash).doesNotContain("30123456");
        assertThat(hash).hasSize(64); // SHA-256 en hexadecimal
    }
}
