-- Seccion 5.6: las aulas de workshop se dividen en 1-3 y 4-6 año. Se agrega la
-- banda al catalogo para poder restringir que un alumno solo elija workshops
-- de su propia banda (antes no se validaba en absoluto).
ALTER TABLE workshop ADD banda VARCHAR(20) NULL;
GO

UPDATE workshop SET banda = CASE WHEN nombre LIKE N'%4%6%' THEN 'ANIOS_4_A_6' ELSE 'ANIOS_1_A_3' END;
GO

ALTER TABLE workshop ALTER COLUMN banda VARCHAR(20) NOT NULL;
ALTER TABLE workshop ADD CONSTRAINT ck_workshop_banda CHECK (banda IN ('ANIOS_1_A_3','ANIOS_4_A_6'));
