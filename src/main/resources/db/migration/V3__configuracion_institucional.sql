-- Modulo 5.14 (Configuracion Institucional). Base estructural usada por el resto
-- de los modulos academicos (alumnos, horarios, calificaciones, etc.).

CREATE TABLE ciclo_lectivo (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    anio                INT NOT NULL UNIQUE,
    fecha_inicio_c1     DATE NOT NULL,
    fecha_fin_c1        DATE NOT NULL,
    fecha_inicio_c2     DATE NOT NULL,
    fecha_fin_c2        DATE NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADO'
                            CHECK (estado IN ('PLANIFICADO','ACTIVO','CERRADO','ARCHIVADO')),
    activo              BIT NOT NULL DEFAULT 0,
    cuatrimestre_activo TINYINT NULL CHECK (cuatrimestre_activo IN (1,2)),
    creado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    actualizado_en      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    creado_por          NVARCHAR(120) NULL,
    actualizado_por     NVARCHAR(120) NULL
);

-- "Solo puede haber uno activo" (seccion 5.14.2): indice unico filtrado, la BD
-- garantiza la invariante incluso si la capa de aplicacion tuviera un bug.
CREATE UNIQUE INDEX ux_ciclo_lectivo_activo ON ciclo_lectivo(activo) WHERE activo = 1;

-- 12 divisiones: 6 años x 2 divisiones (seccion 5.5.3 "navegar entre las 12 divisiones").
CREATE TABLE curso_division (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    anio        INT NOT NULL CHECK (anio BETWEEN 1 AND 6),
    division    CHAR(1) NOT NULL CHECK (division IN ('A','B')),
    CONSTRAINT ux_curso_division UNIQUE (anio, division)
);

CREATE TABLE materia (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre      NVARCHAR(120) NOT NULL,
    activa      BIT NOT NULL DEFAULT 1,
    creado_en   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

-- Una materia puede dictarse en varios cursos/divisiones (relacion M:N).
CREATE TABLE materia_curso_division (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    materia_id          BIGINT NOT NULL,
    curso_division_id   BIGINT NOT NULL,
    CONSTRAINT fk_mcd_materia FOREIGN KEY (materia_id) REFERENCES materia(id) ON DELETE CASCADE,
    CONSTRAINT fk_mcd_curso_division FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT ux_materia_curso_division UNIQUE (materia_id, curso_division_id)
);

CREATE TABLE aula (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre      NVARCHAR(60) NOT NULL UNIQUE,
    capacidad   INT NULL,
    activa      BIT NOT NULL DEFAULT 1
);

-- Catalogo de workshops (seccion 5.6/5.14.2). La logica de asignacion de alumnos
-- vive en el modulo alumno (alumno_ciclo_lectivo.workshop_c1_id/workshop_c2_id).
CREATE TABLE workshop (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre      NVARCHAR(120) NOT NULL,
    descripcion NVARCHAR(500) NULL,
    activo      BIT NOT NULL DEFAULT 1
);

-- Un docente esta asignado a un unico workshop (seccion 5.6.2): UNIQUE en usuario_docente_id.
CREATE TABLE workshop_docente (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    workshop_id         BIGINT NOT NULL,
    usuario_docente_id  BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_workshop_docente_workshop FOREIGN KEY (workshop_id) REFERENCES workshop(id),
    CONSTRAINT fk_workshop_docente_usuario FOREIGN KEY (usuario_docente_id) REFERENCES usuario(id)
);

-- Parametros academicos configurables (seccion 5.14.2), fila unica (id=1).
CREATE TABLE parametro_academico (
    id                              BIGINT IDENTITY(1,1) PRIMARY KEY,
    umbral_falta_1                  INT NOT NULL DEFAULT 10,
    umbral_falta_2                  INT NOT NULL DEFAULT 15,
    umbral_falta_3                  INT NOT NULL DEFAULT 20,
    umbral_falta_4_condicion_libre  INT NOT NULL DEFAULT 28,
    nota_minima_aprobacion          DECIMAL(3,1) NOT NULL DEFAULT 7.0,
    umbral_bajo_rendimiento         DECIMAL(3,1) NOT NULL DEFAULT 6.0,
    porcentaje_asistencia_materia   DECIMAL(5,2) NOT NULL DEFAULT 75.00,
    actualizado_en                  DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    actualizado_por                 NVARCHAR(120) NULL
);

-- Configuracion SMTP institucional (fila unica, id=1). El password se guarda cifrado
-- a nivel aplicacion (ver EncryptedStringConverter), nunca en texto plano.
CREATE TABLE configuracion_smtp (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    host                NVARCHAR(200) NOT NULL,
    puerto              INT NOT NULL DEFAULT 587,
    usuario             NVARCHAR(200) NULL,
    password_cifrado    NVARCHAR(512) NULL,
    usa_auth            BIT NOT NULL DEFAULT 1,
    usa_starttls        BIT NOT NULL DEFAULT 1,
    actualizado_en      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    actualizado_por     NVARCHAR(120) NULL
);

-- Plantillas de notificaciones automaticas con variables dinamicas (seccion 5.14.2).
CREATE TABLE plantilla_email (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    codigo      VARCHAR(60) NOT NULL UNIQUE, -- ej: UMBRAL_10_FALTAS, FIRMA_GENERADA
    asunto      NVARCHAR(200) NOT NULL,
    cuerpo      NVARCHAR(MAX) NOT NULL,
    activa      BIT NOT NULL DEFAULT 1
);

-- Estructura del dia escolar (seccion 5.5.2): periodos, recreos, almuerzo, configurable.
CREATE TABLE periodo_horario (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    orden       INT NOT NULL UNIQUE,
    nombre      NVARCHAR(40) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin    TIME NOT NULL,
    tipo        VARCHAR(20) NOT NULL CHECK (tipo IN ('CLASE','RECREO','ALMUERZO'))
);
