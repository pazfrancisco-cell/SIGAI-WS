-- Modulo 5.4 (Disciplina). Escala: Observacion -> Firma (auto, cada 5 obs) ->
-- Acta de Acuerdo (manual, 3 firmas) -> Acta de Matriculacion (manual, 5 firmas) ->
-- No Rematricula (decision directiva). Ver seccion 5.4.2/5.4.3.
CREATE TABLE observacion_disciplinaria (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id               BIGINT NOT NULL,
    ciclo_lectivo_id        BIGINT NOT NULL,
    fecha                   DATE NOT NULL,
    descripcion             NVARCHAR(500) NOT NULL,
    categoria               VARCHAR(40) NULL,
    firmantes_adicionales   NVARCHAR(200) NULL,
    registrado_por_id       BIGINT NOT NULL,
    registrado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_obs_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_obs_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_obs_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuario(id)
);

CREATE TABLE firma_disciplinaria (
    id                              BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id                       BIGINT NOT NULL,
    ciclo_lectivo_id                BIGINT NOT NULL,
    numero                          INT NOT NULL, -- 1, 2, 3... (multiplo de 5 observaciones)
    observacion_disparadora_id      BIGINT NOT NULL,
    generada_en                     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_firma_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_firma_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_firma_observacion FOREIGN KEY (observacion_disparadora_id) REFERENCES observacion_disciplinaria(id),
    CONSTRAINT ux_firma_alumno_ciclo_numero UNIQUE (alumno_id, ciclo_lectivo_id, numero)
);

CREATE TABLE acta_disciplinaria (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id           BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    tipo                VARCHAR(20) NOT NULL CHECK (tipo IN ('ACUERDO','MATRICULACION')),
    contenido           NVARCHAR(MAX) NOT NULL,
    generada_por_id     BIGINT NOT NULL,
    generada_en         DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_acta_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_acta_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_acta_usuario FOREIGN KEY (generada_por_id) REFERENCES usuario(id)
);

CREATE TABLE no_rematricula (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id           BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    motivo              NVARCHAR(500) NOT NULL,
    confirmado_por_id   BIGINT NOT NULL,
    confirmado_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_norematricula_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_norematricula_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_norematricula_usuario FOREIGN KEY (confirmado_por_id) REFERENCES usuario(id)
);

CREATE INDEX ix_observacion_alumno_ciclo ON observacion_disciplinaria(alumno_id, ciclo_lectivo_id);
