-- Modulo 5.10 (Comunicacion y Mensajeria Interna).
CREATE TABLE conversacion (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    tipo        VARCHAR(20) NOT NULL CHECK (tipo IN ('DIRECTA','GRUPAL')),
    creada_en   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE conversacion_participante (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    conversacion_id     BIGINT NOT NULL,
    usuario_id          BIGINT NOT NULL,
    CONSTRAINT fk_cp_conversacion FOREIGN KEY (conversacion_id) REFERENCES conversacion(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT ux_conversacion_participante UNIQUE (conversacion_id, usuario_id)
);

CREATE TABLE mensaje (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    conversacion_id BIGINT NOT NULL,
    remitente_id    BIGINT NOT NULL,
    contenido       NVARCHAR(2000) NOT NULL,
    adjunto_path    NVARCHAR(400) NULL,
    enviado_en      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_mensaje_conversacion FOREIGN KEY (conversacion_id) REFERENCES conversacion(id),
    CONSTRAINT fk_mensaje_remitente FOREIGN KEY (remitente_id) REFERENCES usuario(id)
);

CREATE TABLE mensaje_lectura (
    mensaje_id  BIGINT NOT NULL,
    usuario_id  BIGINT NOT NULL,
    leido_en    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    PRIMARY KEY (mensaje_id, usuario_id),
    CONSTRAINT fk_lectura_mensaje FOREIGN KEY (mensaje_id) REFERENCES mensaje(id) ON DELETE CASCADE,
    CONSTRAINT fk_lectura_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

-- Comunicados institucionales: difusion unidireccional, no admiten respuesta (seccion 5.10.2).
CREATE TABLE comunicado_institucional (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    asunto          NVARCHAR(120) NOT NULL,
    contenido       NVARCHAR(MAX) NOT NULL,
    autor_id        BIGINT NOT NULL,
    enviar_email    BIT NOT NULL DEFAULT 0,
    creado_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_comunicado_autor FOREIGN KEY (autor_id) REFERENCES usuario(id)
);

CREATE TABLE comunicado_destinatario (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    comunicado_id       BIGINT NOT NULL,
    tipo_destinatario   VARCHAR(30) NOT NULL CHECK (tipo_destinatario IN ('TODOS_PADRES','TODOS_DOCENTES','CURSO','ALUMNO')),
    curso_division_id   BIGINT NULL,
    alumno_id           BIGINT NULL,
    CONSTRAINT fk_comdest_comunicado FOREIGN KEY (comunicado_id) REFERENCES comunicado_institucional(id) ON DELETE CASCADE,
    CONSTRAINT fk_comdest_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT fk_comdest_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id)
);

CREATE TABLE comunicado_adjunto (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    comunicado_id   BIGINT NOT NULL,
    nombre_archivo  NVARCHAR(260) NOT NULL,
    path_archivo    NVARCHAR(400) NOT NULL,
    CONSTRAINT fk_comadj_comunicado FOREIGN KEY (comunicado_id) REFERENCES comunicado_institucional(id) ON DELETE CASCADE
);

-- Notificaciones automaticas del sistema (seccion 5.10.2 / 5.11.4): no respondibles.
CREATE TABLE notificacion_sistema (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id              BIGINT NOT NULL,
    tipo                    VARCHAR(60) NOT NULL,
    titulo                  NVARCHAR(200) NOT NULL,
    contenido               NVARCHAR(1000) NOT NULL,
    leida                   BIT NOT NULL DEFAULT 0,
    enviado_email           BIT NOT NULL DEFAULT 0,
    entidad_relacionada     VARCHAR(60) NULL,
    entidad_relacionada_id  BIGINT NULL,
    creado_en               DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX ix_notificacion_usuario_leida ON notificacion_sistema(usuario_id, leida);
CREATE INDEX ix_mensaje_conversacion ON mensaje(conversacion_id, enviado_en);
