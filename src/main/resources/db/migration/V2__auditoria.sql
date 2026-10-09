-- Seccion 3.5: tabla de auditoria institucional. Sin FK hacia usuario a proposito:
-- el historial de auditoria debe sobrevivir aunque la cuenta de usuario cambie de
-- estado; se guarda ademas un snapshot de texto (usuario_email) para trazabilidad
-- legible sin necesidad de join.
CREATE TABLE audit_log (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id          BIGINT NULL,
    usuario_email       NVARCHAR(180) NULL,
    [timestamp]         DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ip_address          VARCHAR(64) NULL,
    modulo              VARCHAR(60) NOT NULL,
    accion              VARCHAR(60) NOT NULL,
    entidad             VARCHAR(120) NULL,
    registro_id         VARCHAR(60) NULL,
    valores_anteriores  NVARCHAR(MAX) NULL,
    valores_nuevos      NVARCHAR(MAX) NULL
);

CREATE INDEX ix_audit_log_timestamp ON audit_log([timestamp] DESC);
CREATE INDEX ix_audit_log_usuario ON audit_log(usuario_id);
CREATE INDEX ix_audit_log_modulo ON audit_log(modulo);
