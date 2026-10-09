-- Modulo 5.14: los cursos/divisiones se pueden desactivar (en vez de borrar,
-- cuando ya tienen historial) y los alumnos que terminan 6° pasan a EGRESADO
-- desde el asistente de promocion.
ALTER TABLE curso_division ADD activa BIT NOT NULL DEFAULT 1;
GO

DECLARE @constraintName NVARCHAR(200);
SELECT @constraintName = cc.name FROM sys.check_constraints cc
JOIN sys.columns col ON col.object_id = cc.parent_object_id AND col.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID('alumno') AND col.name = 'estado_global';
EXEC('ALTER TABLE alumno DROP CONSTRAINT ' + @constraintName);
ALTER TABLE alumno ADD CONSTRAINT ck_alumno_estado_global CHECK (estado_global IN
    ('ACTIVO','INACTIVO_PASE','INACTIVO_BAJA','EGRESADO'));
