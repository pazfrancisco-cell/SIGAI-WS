-- Modulo 5.13: cada alerta puede quedar vinculada a lo que la genero (una
-- solicitud de correccion, una materia, un cuatrimestre). Permite no
-- duplicar alertas y cerrar automaticamente las que dejan de aplicar.
ALTER TABLE alerta ADD entidad_relacionada VARCHAR(60) NULL;
ALTER TABLE alerta ADD entidad_relacionada_id BIGINT NULL;
GO

CREATE INDEX ix_alerta_entidad ON alerta(tipo, entidad_relacionada, entidad_relacionada_id, estado);

INSERT INTO plantilla_email (tipo, descripcion, asunto, cuerpo) VALUES
('BOLETIN_PUBLICADO', 'Publicacion del boletin de calificaciones (seccion 5.13.2)',
 'Boletin publicado - {nombre_alumno}',
 '<p>Se publico el boletin de calificaciones del cuatrimestre {cuatrimestre} de {nombre_alumno}.</p><p>Podes verlo y descargarlo desde el Portal de Padres de SIGAI-WS.</p>');
