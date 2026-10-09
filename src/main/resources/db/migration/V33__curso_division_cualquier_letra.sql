-- Seccion 5.14.1: desde la pantalla de Cursos y Divisiones se pueden crear divisiones
-- nuevas (1° C, 2° D...). V3 restringia la division a A o B con un CHECK sin nombre.
DECLARE @constraintName NVARCHAR(200);
SELECT @constraintName = cc.name FROM sys.check_constraints cc
JOIN sys.columns col ON col.object_id = cc.parent_object_id AND col.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID('curso_division') AND col.name = 'division';
EXEC('ALTER TABLE curso_division DROP CONSTRAINT ' + @constraintName);
ALTER TABLE curso_division ADD CONSTRAINT ck_curso_division_letra CHECK (division LIKE '[A-Z]');
