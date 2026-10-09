-- Modulo 5.1 (Gestion de Alumnos). Los datos personales sensibles (DNI, fecha de
-- nacimiento, domicilio) se guardan cifrados (columna *_cifrado/*_cifrada, ver
-- EncryptedStringConverter). El DNI ademas guarda un hash SHA-256 determinista
-- (dni_hash) para poder validar unicidad y buscar sin descifrar todo el listado.
CREATE TABLE alumno (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    apellido                NVARCHAR(80) NOT NULL,
    nombre                  NVARCHAR(80) NOT NULL,
    dni_cifrado             NVARCHAR(256) NOT NULL,
    dni_hash                CHAR(64) NOT NULL UNIQUE,
    fecha_nacimiento_cifrada NVARCHAR(128) NOT NULL,
    genero                  VARCHAR(20) NULL CHECK (genero IN ('MASCULINO','FEMENINO','OTRO','PREFIERO_NO_DECIR')),
    nacionalidad            NVARCHAR(80) NOT NULL DEFAULT 'Argentina',
    domicilio_cifrado       NVARCHAR(512) NOT NULL,
    localidad               NVARCHAR(120) NOT NULL,
    foto_path               NVARCHAR(300) NULL,
    estado_global           VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
                                CHECK (estado_global IN ('ACTIVO','INACTIVO_PASE','INACTIVO_BAJA')),
    ciclo_lectivo_ingreso_id BIGINT NOT NULL,
    creado_en               DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    actualizado_en          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    creado_por              NVARCHAR(120) NULL,
    actualizado_por         NVARCHAR(120) NULL,
    CONSTRAINT fk_alumno_ciclo_ingreso FOREIGN KEY (ciclo_lectivo_ingreso_id) REFERENCES ciclo_lectivo(id)
);

-- Un registro por alumno y ciclo lectivo: curso/division cursado, workshop de cada
-- cuatrimestre y los contadores que se reinician cada año (asistencia, disciplina).
-- Esta tabla ES la "trayectoria" (seccion 5.1.4, tab Trayectoria) y a la vez el
-- mecanismo que garantiza el reinicio anual descripto en 5.14.2.
CREATE TABLE alumno_ciclo_lectivo (
    id                          BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id                   BIGINT NOT NULL,
    ciclo_lectivo_id            BIGINT NOT NULL,
    curso_division_id           BIGINT NOT NULL,
    repetidor                   BIT NOT NULL DEFAULT 0,
    workshop_c1_id              BIGINT NULL,
    workshop_c2_id              BIGINT NULL,
    inasistencias_acumuladas    DECIMAL(5,2) NOT NULL DEFAULT 0,
    observaciones_acumuladas    INT NOT NULL DEFAULT 0,
    firmas_acumuladas           INT NOT NULL DEFAULT 0,
    condicion_libre             BIT NOT NULL DEFAULT 0,
    condicion_libre_desde       DATETIME2 NULL,
    creado_en                   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    actualizado_en               DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_acl_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_acl_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_acl_curso_division FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT fk_acl_workshop_c1 FOREIGN KEY (workshop_c1_id) REFERENCES workshop(id),
    CONSTRAINT fk_acl_workshop_c2 FOREIGN KEY (workshop_c2_id) REFERENCES workshop(id),
    CONSTRAINT ux_alumno_ciclo UNIQUE (alumno_id, ciclo_lectivo_id)
);

-- Tutores/responsables. Pueden tener cuenta de acceso al Portal de Padres (5.11):
-- usuario_id es nullable hasta que se genera la cuenta (seccion 6.1, paso 6).
CREATE TABLE tutor (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    apellido            NVARCHAR(80) NOT NULL,
    nombre              NVARCHAR(80) NOT NULL,
    dni_cifrado         NVARCHAR(256) NOT NULL,
    dni_hash            CHAR(64) NOT NULL UNIQUE,
    telefono_cifrado    NVARCHAR(256) NOT NULL,
    email               VARCHAR(180) NOT NULL UNIQUE,
    usuario_id          BIGINT NULL UNIQUE,
    creado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    actualizado_en      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_tutor_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

-- Maximo 2 tutores por alumno (seccion 5.1.5): se valida en AlumnoService/TutorService,
-- no es expresable como constraint declarativo simple sin un trigger.
CREATE TABLE alumno_tutor (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id               BIGINT NOT NULL,
    tutor_id                BIGINT NOT NULL,
    vinculo                 VARCHAR(20) NOT NULL CHECK (vinculo IN ('PADRE','MADRE','TUTOR_LEGAL','OTRO')),
    recibe_notificaciones   BIT NOT NULL DEFAULT 1,
    CONSTRAINT fk_at_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id) ON DELETE CASCADE,
    CONSTRAINT fk_at_tutor FOREIGN KEY (tutor_id) REFERENCES tutor(id),
    CONSTRAINT ux_alumno_tutor UNIQUE (alumno_id, tutor_id)
);

-- Documentacion del legajo (seccion 5.1.5, seccion 4 tab "Documentos").
CREATE TABLE documento_alumno (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id       BIGINT NOT NULL,
    tipo            VARCHAR(40) NOT NULL,
    nombre_archivo  NVARCHAR(260) NOT NULL,
    -- Seccion 7.2: nombre de archivo aleatorio en disco (no predecible por enumeracion).
    path_archivo    NVARCHAR(400) NOT NULL,
    tamano_bytes    BIGINT NOT NULL,
    subido_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    subido_por      NVARCHAR(120) NULL,
    CONSTRAINT fk_documento_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id)
);

-- Historial de pases (seccion 5.1.8): genera un PDF con el historial academico vigente.
CREATE TABLE pase_alumno (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id               BIGINT NOT NULL,
    fecha                   DATE NOT NULL,
    institucion_destino     NVARCHAR(200) NOT NULL,
    pdf_path                NVARCHAR(400) NULL,
    generado_en             DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    generado_por            NVARCHAR(120) NULL,
    CONSTRAINT fk_pase_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id)
);

CREATE INDEX ix_alumno_estado ON alumno(estado_global);
CREATE INDEX ix_alumno_apellido_nombre ON alumno(apellido, nombre);
CREATE INDEX ix_acl_ciclo ON alumno_ciclo_lectivo(ciclo_lectivo_id);
CREATE INDEX ix_documento_alumno ON documento_alumno(alumno_id);
