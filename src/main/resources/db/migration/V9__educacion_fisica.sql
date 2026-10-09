-- Modulo 5.7 (Educacion Fisica). Grupos por genero, mezclando cursos (seccion 5.7.2).
CREATE TABLE grupo_educacion_fisica (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre              NVARCHAR(100) NOT NULL,
    genero              VARCHAR(20) NOT NULL CHECK (genero IN ('MASCULINO','FEMENINO')),
    docente_id          BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    activo              BIT NOT NULL DEFAULT 1,
    CONSTRAINT fk_grupo_ef_docente FOREIGN KEY (docente_id) REFERENCES usuario(id),
    CONSTRAINT fk_grupo_ef_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id)
);

CREATE TABLE grupo_ef_alumno (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    grupo_id        BIGINT NOT NULL,
    alumno_id       BIGINT NOT NULL,
    cuatrimestre    TINYINT NOT NULL CHECK (cuatrimestre IN (1,2)),
    CONSTRAINT fk_grupo_ef_alumno_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_educacion_fisica(id),
    CONSTRAINT fk_grupo_ef_alumno_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT ux_grupo_ef_alumno UNIQUE (grupo_id, alumno_id, cuatrimestre)
);

-- Pruebas fisicas configurables: tipo libre + metrica (seccion 5.7.4).
CREATE TABLE prueba_fisica (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    grupo_id        BIGINT NOT NULL,
    cuatrimestre    TINYINT NOT NULL CHECK (cuatrimestre IN (1,2)),
    tipo_prueba     NVARCHAR(120) NOT NULL,
    metrica         VARCHAR(30) NOT NULL CHECK (metrica IN ('TIEMPO_SEGUNDOS','DISTANCIA_CM','DISTANCIA_METROS','REPETICIONES')),
    fecha           DATE NOT NULL,
    creado_por_id   BIGINT NOT NULL,
    CONSTRAINT fk_prueba_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_educacion_fisica(id),
    CONSTRAINT fk_prueba_usuario FOREIGN KEY (creado_por_id) REFERENCES usuario(id)
);

CREATE TABLE resultado_prueba_fisica (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    prueba_id   BIGINT NOT NULL,
    alumno_id   BIGINT NOT NULL,
    valor       DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_resultado_prueba FOREIGN KEY (prueba_id) REFERENCES prueba_fisica(id),
    CONSTRAINT fk_resultado_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT ux_resultado_prueba_alumno UNIQUE (prueba_id, alumno_id)
);

-- Restricciones medicas (seccion 5.7.5). Descripcion cifrada por tratarse de datos de salud (seccion 7.2).
CREATE TABLE restriccion_medica (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id               BIGINT NOT NULL,
    descripcion_cifrada     NVARCHAR(2048) NOT NULL,
    fecha_inicio            DATE NOT NULL,
    fecha_fin               DATE NULL,
    documento_path          NVARCHAR(400) NULL,
    creado_por_id           BIGINT NOT NULL,
    creado_en               DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_restriccion_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_restriccion_usuario FOREIGN KEY (creado_por_id) REFERENCES usuario(id)
);

CREATE INDEX ix_restriccion_medica_alumno ON restriccion_medica(alumno_id);
