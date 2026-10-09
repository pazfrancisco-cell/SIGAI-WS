-- Bug preexistente: tutor.usuario_id (V4) se declaro NULL UNIQUE. En SQL Server,
-- a diferencia de otros motores, una UNIQUE CONSTRAINT comun solo permite UN
-- valor NULL por columna (lo trata como un valor mas, no lo excluye). Como el
-- diseño real es que la mayoria de los tutores NO tiene cuenta de portal
-- (usuario_id nulo hasta que se genera, ver comentario en V4), un segundo
-- tutor sin cuenta ya rompia el alta con "Violation of UNIQUE KEY constraint".
-- Se reemplaza por un indice unico FILTRADO (soportado desde SQL Server 2008)
-- que solo aplica unicidad cuando usuario_id no es nulo.

DECLARE @constraint_name NVARCHAR(128);
SELECT @constraint_name = kc.name
FROM sys.key_constraints kc
JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID('dbo.tutor') AND c.name = 'usuario_id' AND kc.type = 'UQ';

IF @constraint_name IS NOT NULL
BEGIN
    DECLARE @sql NVARCHAR(500) = N'ALTER TABLE dbo.tutor DROP CONSTRAINT ' + QUOTENAME(@constraint_name);
    EXEC sp_executesql @sql;
END
GO

CREATE UNIQUE INDEX ux_tutor_usuario_id ON dbo.tutor(usuario_id) WHERE usuario_id IS NOT NULL;
