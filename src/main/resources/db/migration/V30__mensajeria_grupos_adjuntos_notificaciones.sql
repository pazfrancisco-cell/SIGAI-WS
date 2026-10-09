-- Modulo 5.10: chat grupal (nombre y creador del grupo), nombre original de los
-- adjuntos en mensajes, y enlace de destino de cada notificacion del sistema.
ALTER TABLE conversacion ADD nombre NVARCHAR(120) NULL;
ALTER TABLE conversacion ADD creado_por_id BIGINT NULL;
ALTER TABLE mensaje ADD adjunto_nombre NVARCHAR(260) NULL;
ALTER TABLE notificacion_sistema ADD enlace NVARCHAR(300) NULL;
GO

ALTER TABLE conversacion ADD CONSTRAINT fk_conversacion_creador FOREIGN KEY (creado_por_id) REFERENCES usuario(id);
CREATE INDEX ix_notificacion_entidad ON notificacion_sistema(usuario_id, entidad_relacionada, entidad_relacionada_id, leida);
