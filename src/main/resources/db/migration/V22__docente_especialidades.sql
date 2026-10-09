-- Especialidades del docente (seccion 5.12.4 / 5.7): permiten distinguir, sin
-- crear un rol nuevo, si un usuario con rol DOCENTE ademas dicta Educacion
-- Fisica y/o Workshops. Se usan para restringir el acceso a esas pantallas
-- solo a los docentes que efectivamente las dictan, en vez de a cualquier
-- Docente.
ALTER TABLE usuario ADD es_docente_educacion_fisica BIT NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD es_docente_workshop BIT NOT NULL DEFAULT 0;
GO

-- Backfill: un docente que ya tiene (o tuvo) un grupo de Educacion Fisica o un
-- workshop a cargo queda marcado automaticamente, para no perder acceso a su
-- propia pantalla el dia que se activa esta restriccion.
UPDATE usuario SET es_docente_educacion_fisica = 1
WHERE id IN (SELECT DISTINCT docente_id FROM grupo_educacion_fisica);

UPDATE usuario SET es_docente_workshop = 1
WHERE id IN (SELECT DISTINCT usuario_docente_id FROM workshop_docente);
