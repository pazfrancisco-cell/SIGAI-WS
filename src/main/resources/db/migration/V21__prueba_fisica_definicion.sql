-- Modulo 5.7 (Educacion Fisica): catalogo reutilizable de pruebas fisicas
-- (nombre + unidad de medida libre + direccion de mejora), en reemplazo del
-- tipo de prueba en texto libre + metrica fija de V9. Seccion 5.7.4 pide que
-- el sistema compare "progreso individual con pruebas anteriores del mismo
-- tipo"; con texto libre esa comparacion se rompia si el tipo se escribia
-- distinto entre sesiones, y no permitia seguir al alumno a traves de
-- distintos grupos/ciclos lectivos. El catalogo se define una sola vez y se
-- reutiliza, permitiendo ademas graficar la evolucion de un alumno en una
-- misma actividad a lo largo de los años.

CREATE TABLE prueba_fisica_definicion (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre          NVARCHAR(120) NOT NULL,
    unidad          NVARCHAR(40) NOT NULL,
    mayor_es_mejor  BIT NOT NULL,
    activa          BIT NOT NULL DEFAULT 1,
    creado_por_id   BIGINT NOT NULL,
    creado_en       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_prueba_fisica_definicion_nombre UNIQUE (nombre),
    CONSTRAINT fk_prueba_def_usuario FOREIGN KEY (creado_por_id) REFERENCES usuario(id)
);
GO

-- Que pruebas del catalogo aplican a cada grupo (elegidas al crear/editar el grupo).
CREATE TABLE grupo_ef_prueba_definicion (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    grupo_id        BIGINT NOT NULL,
    definicion_id   BIGINT NOT NULL,
    CONSTRAINT fk_grupo_ef_def_grupo FOREIGN KEY (grupo_id) REFERENCES grupo_educacion_fisica(id),
    CONSTRAINT fk_grupo_ef_def_definicion FOREIGN KEY (definicion_id) REFERENCES prueba_fisica_definicion(id),
    CONSTRAINT ux_grupo_ef_prueba_definicion UNIQUE (grupo_id, definicion_id)
);
GO

-- Backfill: una definicion por cada combinacion distinta de tipo_prueba+metrica
-- ya cargada en prueba_fisica.
INSERT INTO prueba_fisica_definicion (nombre, unidad, mayor_es_mejor, creado_por_id)
SELECT tipo_prueba,
       CASE metrica
           WHEN 'TIEMPO_SEGUNDOS' THEN N'segundos'
           WHEN 'DISTANCIA_CM' THEN N'cm'
           WHEN 'DISTANCIA_METROS' THEN N'metros'
           WHEN 'REPETICIONES' THEN N'repeticiones'
       END,
       CASE WHEN metrica = 'TIEMPO_SEGUNDOS' THEN 0 ELSE 1 END,
       MIN(creado_por_id)
FROM prueba_fisica
GROUP BY tipo_prueba, metrica;
GO

ALTER TABLE prueba_fisica ADD definicion_id BIGINT NULL;
GO

UPDATE p
SET p.definicion_id = d.id
FROM prueba_fisica p
JOIN prueba_fisica_definicion d ON d.nombre = p.tipo_prueba;
GO

-- Cada grupo queda vinculado a las definiciones que ya venia usando.
INSERT INTO grupo_ef_prueba_definicion (grupo_id, definicion_id)
SELECT DISTINCT grupo_id, definicion_id FROM prueba_fisica;
GO

ALTER TABLE prueba_fisica ALTER COLUMN definicion_id BIGINT NOT NULL;
GO

ALTER TABLE prueba_fisica ADD CONSTRAINT fk_prueba_definicion FOREIGN KEY (definicion_id) REFERENCES prueba_fisica_definicion(id);
GO

-- Elimina el CHECK constraint autogenerado de "metrica" (sin nombre fijo en V9) antes de poder borrar la columna.
DECLARE @constraintName NVARCHAR(200);
SELECT @constraintName = cc.name
FROM sys.check_constraints cc
JOIN sys.columns col ON col.object_id = cc.parent_object_id AND col.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID('prueba_fisica') AND col.name = 'metrica';
IF @constraintName IS NOT NULL
    EXEC('ALTER TABLE prueba_fisica DROP CONSTRAINT ' + @constraintName);
GO

ALTER TABLE prueba_fisica DROP COLUMN metrica;
GO
ALTER TABLE prueba_fisica DROP COLUMN tipo_prueba;
GO
