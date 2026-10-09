-- Modulo 5.5 (Horarios y Asignacion Docente).
CREATE TABLE asignacion_docente (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_docente_id  BIGINT NOT NULL,
    materia_id          BIGINT NOT NULL,
    curso_division_id   BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    CONSTRAINT fk_asig_docente_usuario FOREIGN KEY (usuario_docente_id) REFERENCES usuario(id),
    CONSTRAINT fk_asig_docente_materia FOREIGN KEY (materia_id) REFERENCES materia(id),
    CONSTRAINT fk_asig_docente_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT fk_asig_docente_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT ux_asignacion_docente UNIQUE (usuario_docente_id, materia_id, curso_division_id, ciclo_lectivo_id)
);

-- Grid semanal (seccion 5.5.3): 5 dias x 5 periodos por division y ciclo lectivo.
CREATE TABLE horario_slot (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    curso_division_id   BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    dia_semana          TINYINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 5), -- 1=Lunes .. 5=Viernes
    periodo_orden       INT NOT NULL,
    materia_id          BIGINT NULL,
    docente_id          BIGINT NULL,
    aula_id             BIGINT NULL,
    CONSTRAINT fk_slot_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT fk_slot_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_slot_materia FOREIGN KEY (materia_id) REFERENCES materia(id),
    CONSTRAINT fk_slot_docente FOREIGN KEY (docente_id) REFERENCES usuario(id),
    CONSTRAINT fk_slot_aula FOREIGN KEY (aula_id) REFERENCES aula(id),
    CONSTRAINT fk_slot_periodo FOREIGN KEY (periodo_orden) REFERENCES periodo_horario(orden),
    CONSTRAINT ux_horario_slot UNIQUE (curso_division_id, ciclo_lectivo_id, dia_semana, periodo_orden)
);

CREATE INDEX ix_horario_slot_docente ON horario_slot(docente_id, ciclo_lectivo_id, dia_semana, periodo_orden);
CREATE INDEX ix_horario_slot_aula ON horario_slot(aula_id, ciclo_lectivo_id, dia_semana, periodo_orden);
