-- Seccion 5.7 (extension): toma de asistencia diaria para Educacion Fisica,
-- por grupo (no existia ningun mecanismo de asistencia para este modulo hasta
-- ahora). SAF cuenta como presente a los efectos de la asistencia (el alumno
-- va a clase igual), pero queda registrado como un estado distinto.
CREATE TABLE asistencia_educacion_fisica (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id           BIGINT NOT NULL,
    grupo_id            BIGINT NOT NULL,
    fecha               DATE NOT NULL,
    tipo                VARCHAR(20) NOT NULL CHECK (tipo IN ('PRESENTE','AUSENTE','LLEGADA_TARDE','SAF')),
    registrado_por_id   BIGINT NOT NULL,
    registrado_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_asistencia_ef_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_asistencia_ef_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_educacion_fisica(id),
    CONSTRAINT fk_asistencia_ef_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuario(id),
    CONSTRAINT ux_asistencia_ef UNIQUE (alumno_id, grupo_id, fecha)
);
