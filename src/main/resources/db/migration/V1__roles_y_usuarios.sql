-- Modulo 5.12 (Usuarios, Roles y Permisos) + base de autenticacion (seccion 7.1).
-- Sesiones de servidor (ver docs/DECISIONS.md ADR-001): no se requiere tabla de
-- tokens JWT; Spring Security mantiene el estado de sesion via cookie HttpOnly.

CREATE TABLE rol (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre          VARCHAR(30)  NOT NULL UNIQUE, -- coincide con common.enums.RolNombre
    etiqueta        NVARCHAR(60) NOT NULL
);

CREATE TABLE usuario (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre_completo         NVARCHAR(160) NOT NULL,
    email                   VARCHAR(180)  NOT NULL UNIQUE, -- email institucional, es el login (seccion 3.1)
    password_hash           VARCHAR(100)  NOT NULL,        -- bcrypt, factor >=12 (seccion 7.1)
    debe_cambiar_password   BIT           NOT NULL DEFAULT 1,
    activo                  BIT           NOT NULL DEFAULT 1,
    intentos_fallidos       INT           NOT NULL DEFAULT 0,
    bloqueado_hasta         DATETIME2     NULL,
    ultimo_acceso           DATETIME2     NULL,
    creado_en               DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    actualizado_en          DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    creado_por              NVARCHAR(120) NULL,
    actualizado_por         NVARCHAR(120) NULL
);

-- Roles combinados (seccion 5.12.3): un usuario puede tener mas de un rol activo.
-- Las combinaciones validas se validan en UsuarioService, no aqui (una tabla de
-- asociacion generica no puede expresar "Docente no combina con otros roles"
-- sin triggers; se prefirio mantener esa regla en la capa de aplicacion).
CREATE TABLE usuario_rol (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id  BIGINT NOT NULL,
    rol_id      BIGINT NOT NULL,
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_rol_rol FOREIGN KEY (rol_id) REFERENCES rol(id),
    CONSTRAINT ux_usuario_rol UNIQUE (usuario_id, rol_id)
);

-- Recuperacion de contraseña (seccion 7.1): enlace temporal, 30 min, un solo uso.
CREATE TABLE password_reset_token (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id  BIGINT NOT NULL,
    token       VARCHAR(100) NOT NULL UNIQUE,
    expira_en   DATETIME2 NOT NULL,
    usado       BIT NOT NULL DEFAULT 0,
    creado_en   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_password_reset_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE INDEX ix_usuario_activo ON usuario(activo);
CREATE INDEX ix_password_reset_token_expira ON password_reset_token(expira_en);
