-- Modulo 5.3 (Calificaciones). Escala 1-10 con un decimal, aprobacion >= 7 (seccion 5.3.2).
CREATE TABLE evaluacion (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    materia_id          BIGINT NOT NULL,
    curso_division_id   BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    cuatrimestre        TINYINT NOT NULL CHECK (cuatrimestre IN (1,2)),
    tipo                VARCHAR(30) NOT NULL CHECK (tipo IN
                            ('PRUEBA_ESCRITA','EVALUACION_ORAL','TRABAJO_PRACTICO','EVALUACION_CONCEPTUAL',
                             'EVALUACION_DEPORTIVA','TRABAJO_PRACTICO_ESCRITO')),
    titulo              NVARCHAR(160) NOT NULL,
    fecha               DATE NOT NULL,
    creado_por_id       BIGINT NOT NULL,
    creado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_evaluacion_materia FOREIGN KEY (materia_id) REFERENCES materia(id),
    CONSTRAINT fk_evaluacion_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT fk_evaluacion_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_evaluacion_usuario FOREIGN KEY (creado_por_id) REFERENCES usuario(id)
);

CREATE TABLE nota (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    evaluacion_id   BIGINT NOT NULL,
    alumno_id       BIGINT NOT NULL,
    valor           DECIMAL(3,1) NOT NULL CHECK (valor BETWEEN 1 AND 10),
    cargado_por_id  BIGINT NOT NULL,
    cargado_en      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_nota_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluacion(id),
    CONSTRAINT fk_nota_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_nota_usuario FOREIGN KEY (cargado_por_id) REFERENCES usuario(id),
    CONSTRAINT ux_nota UNIQUE (evaluacion_id, alumno_id)
);

-- Flujo de correccion sobre periodo cerrado (seccion 5.3.6).
CREATE TABLE solicitud_correccion_nota (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    nota_id             BIGINT NOT NULL,
    nota_anterior       DECIMAL(3,1) NOT NULL,
    nota_propuesta      DECIMAL(3,1) NOT NULL CHECK (nota_propuesta BETWEEN 1 AND 10),
    justificacion       NVARCHAR(500) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA')),
    solicitado_por_id   BIGINT NOT NULL,
    solicitado_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    resuelto_por_id     BIGINT NULL,
    resuelto_en         DATETIME2 NULL,
    motivo_rechazo      NVARCHAR(300) NULL,
    CONSTRAINT fk_solicitud_nota FOREIGN KEY (nota_id) REFERENCES nota(id),
    CONSTRAINT fk_solicitud_solicitante FOREIGN KEY (solicitado_por_id) REFERENCES usuario(id),
    CONSTRAINT fk_solicitud_resolutor FOREIGN KEY (resuelto_por_id) REFERENCES usuario(id)
);

CREATE INDEX ix_evaluacion_materia_curso ON evaluacion(materia_id, curso_division_id, ciclo_lectivo_id, cuatrimestre);
CREATE INDEX ix_nota_alumno ON nota(alumno_id);
CREATE INDEX ix_solicitud_estado ON solicitud_correccion_nota(estado);
