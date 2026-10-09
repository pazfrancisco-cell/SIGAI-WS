-- Modulo 5.13 (Alertas y Seguimiento Automatico). Catalogo de tipos en seccion 5.13.2.
CREATE TABLE alerta (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    tipo                VARCHAR(60) NOT NULL,
    prioridad           VARCHAR(10) NOT NULL CHECK (prioridad IN ('INFO','MEDIA','ALTA','CRITICA')),
    alumno_id           BIGINT NULL,
    descripcion         NVARCHAR(500) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'NUEVA' CHECK (estado IN ('NUEVA','EN_SEGUIMIENTO','RESUELTA')),
    generada_en         DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    resuelta_por_id     BIGINT NULL,
    resuelta_en         DATETIME2 NULL,
    nota_resolucion     NVARCHAR(500) NULL,
    CONSTRAINT fk_alerta_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_alerta_resolutor FOREIGN KEY (resuelta_por_id) REFERENCES usuario(id)
);

CREATE INDEX ix_alerta_estado_prioridad ON alerta(estado, prioridad);
CREATE INDEX ix_alerta_alumno ON alerta(alumno_id);
