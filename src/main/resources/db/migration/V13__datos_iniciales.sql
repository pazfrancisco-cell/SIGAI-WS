-- Datos de arranque. El usuario Superadministrador NO se crea aqui (evitar
-- password hash hardcodeado en el repositorio); lo crea BootstrapDataRunner
-- en el primer arranque, leyendo SIGAI_BOOTSTRAP_ADMIN_EMAIL/PASSWORD del entorno.

INSERT INTO rol (nombre, etiqueta) VALUES
    ('SUPERADMIN', 'Superadministrador'),
    ('DIRECTIVO', 'Directivo'),
    ('ADMINISTRATIVO', 'Administrativo'),
    ('PRECEPTOR', 'Preceptor'),
    ('DOCENTE', 'Docente'),
    ('PADRE_TUTOR', 'Padre/Tutor');

-- Estructura sugerida del dia escolar (seccion 5.5.2), editable luego desde
-- Configuracion Institucional > Horarios y Estructura del Dia.
INSERT INTO periodo_horario (orden, nombre, hora_inicio, hora_fin, tipo) VALUES
    (1, 'Periodo 1', '08:20', '09:55', 'CLASE'),
    (2, 'Recreo',    '09:55', '10:10', 'RECREO'),
    (3, 'Periodo 2', '10:10', '11:45', 'CLASE'),
    (4, 'Recreo',    '11:45', '12:00', 'RECREO'),
    (5, 'Periodo 3', '12:00', '13:00', 'CLASE'),
    (6, 'Almuerzo',  '13:00', '14:00', 'ALMUERZO'),
    (7, 'Periodo 4', '14:00', '15:05', 'CLASE'),
    (8, 'Recreo',    '15:05', '15:15', 'RECREO'),
    (9, 'Periodo 5', '15:15', '16:30', 'CLASE');

-- Parametros academicos por defecto (seccion 5.14.2), fila unica.
INSERT INTO parametro_academico
    (umbral_falta_1, umbral_falta_2, umbral_falta_3, umbral_falta_4_condicion_libre,
     nota_minima_aprobacion, umbral_bajo_rendimiento, porcentaje_asistencia_materia)
VALUES (10, 15, 20, 28, 7.0, 6.0, 75.00);

-- 12 divisiones: años 1 a 6, divisiones A y B (seccion 1.4 / 5.5.3).
INSERT INTO curso_division (anio, division)
SELECT anio, division
FROM (VALUES (1),(2),(3),(4),(5),(6)) AS anios(anio)
CROSS JOIN (VALUES ('A'),('B')) AS divisiones(division);
