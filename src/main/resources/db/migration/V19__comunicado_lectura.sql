-- Registro de lectura de comunicados institucionales (seccion 5.10.2): fecha y
-- hora en que cada destinatario (tipicamente un padre/tutor) abrio el
-- comunicado por primera vez. Un comunicado sin fila aca todavia no fue leido
-- por ese usuario.
CREATE TABLE comunicado_lectura (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    comunicado_id   BIGINT NOT NULL,
    usuario_id      BIGINT NOT NULL,
    leido_en        DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_comlectura_comunicado FOREIGN KEY (comunicado_id) REFERENCES comunicado_institucional(id) ON DELETE CASCADE,
    CONSTRAINT fk_comlectura_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT ux_comunicado_lectura UNIQUE (comunicado_id, usuario_id)
);

CREATE INDEX ix_comunicado_lectura_comunicado ON comunicado_lectura(comunicado_id);
