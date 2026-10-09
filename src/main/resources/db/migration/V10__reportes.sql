-- Modulo 5.8 (Reportes Institucionales). Metadata de cada PDF generado; el
-- contenido en si vive en el filesystem (sigai.archivos.storage-path).
CREATE TABLE reporte_generado (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    tipo                VARCHAR(40) NOT NULL CHECK (tipo IN
                            ('BOLETIN','ASISTENCIA','FIP','RITE','DISCIPLINARIO_CURSO',
                             'RENDIMIENTO_MATERIA','ASISTENCIA_CURSO','CONDICION_LIBRE')),
    alumno_id           BIGINT NULL,
    curso_division_id   BIGINT NULL,
    ciclo_lectivo_id    BIGINT NULL,
    cuatrimestre        TINYINT NULL CHECK (cuatrimestre IN (1,2)),
    path_archivo        NVARCHAR(400) NOT NULL,
    generado_por_id     BIGINT NOT NULL,
    generado_en         DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_reporte_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_reporte_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT fk_reporte_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_reporte_usuario FOREIGN KEY (generado_por_id) REFERENCES usuario(id)
);

CREATE INDEX ix_reporte_tipo ON reporte_generado(tipo, generado_en DESC);
