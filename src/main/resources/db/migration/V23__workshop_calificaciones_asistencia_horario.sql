-- Fase 4 (seccion 5.6): calificaciones, asistencia y horario para workshops.
-- Se modelan en tablas propias (no en evaluacion/nota/asistencia_clase/horario_slot)
-- porque un workshop no tiene curso_division fijo: su "curso" es el conjunto de
-- alumnos que lo eligieron ese cuatrimestre (alumno_ciclo_lectivo.workshop_c1_id/
-- workshop_c2_id), y su evaluacion es un texto libre por docente, no una nota 1-10.

-- Calificaciones (evaluacion distinta: texto libre, sin tipo ni promedio).
CREATE TABLE workshop_evaluacion (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    workshop_id         BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    cuatrimestre        TINYINT NOT NULL CHECK (cuatrimestre IN (1,2)),
    titulo              NVARCHAR(160) NOT NULL,
    fecha               DATE NOT NULL,
    creado_por_id       BIGINT NOT NULL,
    creado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_workshop_eval_workshop FOREIGN KEY (workshop_id) REFERENCES workshop(id),
    CONSTRAINT fk_workshop_eval_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_workshop_eval_usuario FOREIGN KEY (creado_por_id) REFERENCES usuario(id)
);

CREATE TABLE workshop_nota (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    workshop_evaluacion_id  BIGINT NOT NULL,
    alumno_id               BIGINT NOT NULL,
    texto                   NVARCHAR(2000) NOT NULL,
    cargado_por_id          BIGINT NOT NULL,
    cargado_en              DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_workshop_nota_eval FOREIGN KEY (workshop_evaluacion_id) REFERENCES workshop_evaluacion(id),
    CONSTRAINT fk_workshop_nota_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_workshop_nota_usuario FOREIGN KEY (cargado_por_id) REFERENCES usuario(id),
    CONSTRAINT ux_workshop_nota UNIQUE (workshop_evaluacion_id, alumno_id)
);

-- Asistencia por clase de workshop (mismo patron plano que asistencia_clase).
CREATE TABLE workshop_asistencia (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id           BIGINT NOT NULL,
    workshop_id         BIGINT NOT NULL,
    fecha               DATE NOT NULL,
    tipo                VARCHAR(30) NOT NULL CHECK (tipo IN ('PRESENTE','AUSENTE','LLEGADA_TARDE')),
    observacion         NVARCHAR(300) NULL,
    registrado_por_id   BIGINT NOT NULL,
    registrado_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_workshop_asis_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_workshop_asis_workshop FOREIGN KEY (workshop_id) REFERENCES workshop(id),
    CONSTRAINT fk_workshop_asis_usuario FOREIGN KEY (registrado_por_id) REFERENCES usuario(id),
    CONSTRAINT ux_workshop_asistencia UNIQUE (alumno_id, workshop_id, fecha)
);

-- Horario fijo semanal del workshop (tabla propia: un workshop no tiene curso_division).
CREATE TABLE workshop_horario_slot (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    workshop_id         BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    dia_semana          TINYINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 5),
    periodo_orden       INT NOT NULL,
    aula_id             BIGINT NULL,
    CONSTRAINT fk_workshop_slot_workshop FOREIGN KEY (workshop_id) REFERENCES workshop(id),
    CONSTRAINT fk_workshop_slot_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_workshop_slot_periodo FOREIGN KEY (periodo_orden) REFERENCES periodo_horario(orden),
    CONSTRAINT fk_workshop_slot_aula FOREIGN KEY (aula_id) REFERENCES aula(id),
    CONSTRAINT ux_workshop_horario_slot UNIQUE (workshop_id, ciclo_lectivo_id, dia_semana, periodo_orden)
);

CREATE INDEX ix_workshop_horario_slot_aula ON workshop_horario_slot(aula_id, ciclo_lectivo_id, dia_semana, periodo_orden);
