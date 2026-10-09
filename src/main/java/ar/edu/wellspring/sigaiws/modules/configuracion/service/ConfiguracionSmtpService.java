package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.ConfiguracionSmtp;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.ConfiguracionSmtpForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.ConfiguracionSmtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.14.2: configuracion SMTP institucional, fila unica (id=1). */
@Service
@RequiredArgsConstructor
public class ConfiguracionSmtpService {

    private static final long ID_UNICO = 1L;
    private static final String MODULO = "CONFIGURACION";

    private final ConfiguracionSmtpRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ConfiguracionSmtp obtener() {
        return repository.findById(ID_UNICO).orElseGet(() -> {
            ConfiguracionSmtp nueva = new ConfiguracionSmtp();
            nueva.setId(ID_UNICO);
            return nueva;
        });
    }

    @Transactional
    public void guardar(ConfiguracionSmtpForm form) {
        ConfiguracionSmtp config = obtener();
        config.setUsarConfiguracionPersonalizada(form.isUsarConfiguracionPersonalizada());
        config.setHost(form.getHost());
        config.setPuerto(form.getPuerto());
        config.setUsuario(form.getUsuario());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            config.setPassword(form.getPassword());
        }
        config.setRemitente(form.getRemitente());
        config.setAuthHabilitado(form.isAuthHabilitado());
        config.setStarttlsHabilitado(form.isStarttlsHabilitado());
        repository.save(config);
        auditService.registrar(MODULO, "EDITAR_CONFIGURACION_SMTP", "ConfiguracionSmtp", ID_UNICO, null, null);
    }
}
