package ar.edu.wellspring.sigaiws.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converter JPA para columnas cifradas. Uso: {@code @Convert(converter = EncryptedStringConverter.class)}
 * sobre el campo del entity. Al ser un {@code @Component}, Spring Boot lo registra en el
 * SpringBeanContainer de Hibernate y permite inyectar {@link AesGcmEncryptor} sin
 * necesidad de un ApplicationContextProvider estatico.
 *
 * IMPORTANTE: columnas cifradas con este converter no son indexables/buscables por
 * SQL directo (LIKE, =). Para busquedas (ej. DNI) se mantiene, ademas del valor
 * cifrado, un hash determinista (ver Alumno.dniHash) usado exclusivamente para
 * validar unicidad y filtrar, nunca para mostrar el dato en claro.
 */
@Converter(autoApply = false)
@Component
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesGcmEncryptor encryptor;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptor.decrypt(dbData);
    }
}
