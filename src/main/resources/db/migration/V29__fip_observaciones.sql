-- Modulo 5.8 (extension): la FIP (Ficha de Informacion Pedagogica) incluye un
-- campo de observaciones pedagogicas cargado a mano al momento de generarla.
ALTER TABLE reporte_generado ADD observaciones NVARCHAR(MAX) NULL;
