-- Materias adeudadas y mesas de examen (diciembre, febrero y julio).
-- Una materia con promedio anual desaprobado queda adeudada; se rinde en las mesas
-- hasta aprobarla. Si despues de febrero el alumno debe mas de 2 materias (sumando
-- previas de años anteriores), repite. Si repite, las del año que recursa se anulan.

CREATE TABLE materia_adeudada (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id           BIGINT NOT NULL,
    materia_id          BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    curso_division_id   BIGINT NOT NULL,
    promedio_anual      DECIMAL(4,2) NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','APROBADA','RECURSADA')),
    nota_aprobacion     DECIMAL(3,1) NULL,
    aprobada_en         DATE NULL,
    turno_aprobacion    VARCHAR(20) NULL,
    creado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_adeudada_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_adeudada_materia FOREIGN KEY (materia_id) REFERENCES materia(id),
    CONSTRAINT fk_adeudada_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_adeudada_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT ux_materia_adeudada UNIQUE (alumno_id, materia_id, ciclo_lectivo_id)
);
CREATE INDEX ix_materia_adeudada_estado ON materia_adeudada (estado, materia_id);

CREATE TABLE nota_mesa (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    materia_adeudada_id  BIGINT NOT NULL,
    turno                VARCHAR(20) NOT NULL CHECK (turno IN ('DICIEMBRE','FEBRERO','JULIO')),
    anio                 INT NOT NULL,
    fecha                DATE NOT NULL,
    nota                 DECIMAL(3,1) NULL CHECK (nota BETWEEN 1 AND 10),
    ausente              BIT NOT NULL DEFAULT 0,
    cargado_por_id       BIGINT NOT NULL,
    cargado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_nota_mesa_adeudada FOREIGN KEY (materia_adeudada_id) REFERENCES materia_adeudada(id),
    CONSTRAINT fk_nota_mesa_usuario FOREIGN KEY (cargado_por_id) REFERENCES usuario(id),
    CONSTRAINT ux_nota_mesa UNIQUE (materia_adeudada_id, turno, anio)
);
