CREATE TABLE configuracion_smtp (
    id                                BIGINT        NOT NULL PRIMARY KEY,
    usar_configuracion_personalizada  BIT           NOT NULL DEFAULT 0,
    host                              NVARCHAR(200) NULL,
    puerto                            INT           NULL,
    usuario                           NVARCHAR(200) NULL,
    password_cifrada                  NVARCHAR(256) NULL,
    remitente                        NVARCHAR(200) NULL,
    auth_habilitado                   BIT           NOT NULL DEFAULT 1,
    starttls_habilitado               BIT           NOT NULL DEFAULT 1
);
GO

INSERT INTO configuracion_smtp (id, usar_configuracion_personalizada) VALUES (1, 0);
GO

CREATE TABLE plantilla_email (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    tipo          NVARCHAR(60)   NOT NULL UNIQUE,
    descripcion   NVARCHAR(200)  NOT NULL,
    asunto        NVARCHAR(200)  NOT NULL,
    cuerpo        NVARCHAR(MAX)  NOT NULL,
    habilitada    BIT            NOT NULL DEFAULT 1,
    actualizado_en DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

INSERT INTO plantilla_email (tipo, descripcion, asunto, cuerpo) VALUES
('BIENVENIDA_USUARIO', 'Alta de un nuevo usuario (seccion 5.12.4)',
 'Bienvenido/a a SIGAI-WS',
 '<p>Hola {nombre},</p><p>Se creo tu cuenta en SIGAI-WS. Tu email de acceso es {email} y tu contrasena temporal es <b>{password_temporal}</b>. Debes cambiarla al iniciar sesion por primera vez.</p>'),
('RECUPERACION_PASSWORD', 'Solicitud de recuperacion de contrasena (seccion 7.1)',
 'Recuperacion de contrasena - SIGAI-WS',
 '<p>Recibimos una solicitud para restablecer tu contrasena.</p><p>Ingresa a este enlace (valido por 30 minutos): {link_restablecer}</p><p>Si no solicitaste esto, ignora este email.</p>'),
('UMBRAL_INASISTENCIAS', 'Umbral de inasistencias alcanzado (seccion 5.2.3/5.13.2)',
 'Aviso de inasistencias - {nombre_alumno}',
 '<p>{nombre_alumno} acumulo {cantidad_faltas} inasistencias en el ciclo lectivo.</p><p>{detalle}</p>'),
('FIRMA_DISCIPLINARIA', 'Generacion automatica de firma disciplinaria (seccion 5.4.3)',
 'Firma disciplinaria generada - {nombre_alumno}',
 '<p>{nombre_alumno} acumulo observaciones disciplinarias y se genero automaticamente la Firma {numero_firma}.</p>');
GO
