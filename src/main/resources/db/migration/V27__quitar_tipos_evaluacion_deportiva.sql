-- Se sacan EVALUACION_DEPORTIVA y TRABAJO_PRACTICO_ESCRITO del catalogo de tipos
-- de evaluacion de Calificaciones: quedan cubiertos por el modulo de Pruebas
-- Fisicas de Educacion Fisica, no por una evaluacion de materia comun. La unica
-- fila existente con TRABAJO_PRACTICO_ESCRITO se reasigna a TRABAJO_PRACTICO
-- para no perder el dato ya cargado.
UPDATE evaluacion SET tipo = 'TRABAJO_PRACTICO' WHERE tipo = 'TRABAJO_PRACTICO_ESCRITO';
GO

DECLARE @constraintName NVARCHAR(200);
SELECT @constraintName = cc.name
FROM sys.check_constraints cc
JOIN sys.columns col ON col.object_id = cc.parent_object_id AND col.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID('evaluacion') AND col.name = 'tipo';

EXEC('ALTER TABLE evaluacion DROP CONSTRAINT ' + @constraintName);

ALTER TABLE evaluacion ADD CONSTRAINT ck_evaluacion_tipo CHECK (tipo IN
    ('PRUEBA_ESCRITA','EVALUACION_ORAL','TRABAJO_PRACTICO','EVALUACION_CONCEPTUAL'));
