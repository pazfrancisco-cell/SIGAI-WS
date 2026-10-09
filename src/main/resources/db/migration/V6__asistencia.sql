-- Modulo 5.2 (Asistencia). Dos tipos que coexisten (seccion 5.2.1): protocolar
-- diaria por jornada/turno (preceptor) y por clase/materia (docente, mas granular
-- para el calculo del 75% por materia, seccion 5.2.4).
CREATE TABLE asistencia_jornada (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id           BIGINT NOT NULL,
    fecha               DATE NOT NULL,
    turno               VARCHAR(10) NOT NULL CHECK (turno IN ('MANANA','TARDE')),
    tipo                VARCHAR(30) NOT NULL CHECK (tipo IN
                            ('PRESENTE','AUSENTE','AUSENTE_JUSTIFICADO','LLEGADA_TARDE','UNIFORME_INCOMPLETO','MEDIA_FALTA')),
    registrado_por_id   BIGINT NOT NULL,
    registrado_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_aj_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_aj_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuario(id),
    CONSTRAINT ux_asistencia_jornada UNIQUE (alumno_id, fecha, turno)
);

CREATE TABLE asistencia_clase (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id           BIGINT NOT NULL,
    materia_id          BIGINT NOT NULL,
    curso_division_id   BIGINT NOT NULL,
    horario_slot_id     BIGINT NULL,
    fecha               DATE NOT NULL,
    tipo                VARCHAR(30) NOT NULL CHECK (tipo IN ('PRESENTE','AUSENTE','LLEGADA_TARDE')),
    observacion         NVARCHAR(300) NULL,
    registrado_por_id   BIGINT NOT NULL,
    registrado_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_ac_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_ac_materia FOREIGN KEY (materia_id) REFERENCES materia(id),
    CONSTRAINT fk_ac_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT fk_ac_slot FOREIGN KEY (horario_slot_id) REFERENCES horario_slot(id),
    CONSTRAINT fk_ac_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuario(id),
    CONSTRAINT ux_asistencia_clase UNIQUE (alumno_id, materia_id, fecha)
);

-- Justificacion (seccion 5.2.8): no elimina la falta, cambia su tipo a AUSENTE_JUSTIFICADO.
CREATE TABLE justificacion_inasistencia (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    asistencia_jornada_id   BIGINT NOT NULL UNIQUE,
    tipo                    VARCHAR(20) NOT NULL CHECK (tipo IN ('MEDICA','FAMILIAR','INSTITUCIONAL','OTRA')),
    descripcion             NVARCHAR(200) NOT NULL,
    documento_path          NVARCHAR(400) NULL,
    justificado_por_id      BIGINT NOT NULL,
    justificado_en          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_justificacion_asistencia FOREIGN KEY (asistencia_jornada_id) REFERENCES asistencia_jornada(id),
    CONSTRAINT fk_justificacion_usuario FOREIGN KEY (justificado_por_id) REFERENCES usuario(id)
);

CREATE INDEX ix_asistencia_jornada_fecha ON asistencia_jornada(fecha);
CREATE INDEX ix_asistencia_clase_fecha ON asistencia_clase(fecha);
CREATE INDEX ix_asistencia_clase_materia ON asistencia_clase(materia_id, curso_division_id);
