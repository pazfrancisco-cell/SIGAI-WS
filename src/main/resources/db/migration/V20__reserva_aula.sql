-- Reserva puntual de un aula por un docente, para una fecha y periodo concretos
-- (distinto del horario_slot semanal fijo): seccion 5.5.1, uso ad-hoc del aula
-- (ej. laboratorio, actividad especial, reunion) con un curso/division.
CREATE TABLE reserva_aula (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    aula_id             BIGINT NOT NULL,
    docente_id          BIGINT NOT NULL,
    curso_division_id   BIGINT NOT NULL,
    fecha               DATE NOT NULL,
    periodo_orden       INT NOT NULL,
    motivo              NVARCHAR(200) NOT NULL,
    creado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_reservaaula_aula FOREIGN KEY (aula_id) REFERENCES aula(id),
    CONSTRAINT fk_reservaaula_docente FOREIGN KEY (docente_id) REFERENCES usuario(id),
    CONSTRAINT fk_reservaaula_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT ux_reserva_aula UNIQUE (aula_id, fecha, periodo_orden)
);

CREATE INDEX ix_reserva_aula_docente ON reserva_aula(docente_id, fecha);
