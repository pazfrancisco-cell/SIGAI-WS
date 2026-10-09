-- Apto fisico anual (seccion 5.7, extension): cada alumno debe presentar uno
-- por ciclo lectivo, con vencimiento fijo al 1 de abril del año del ciclo.
-- Mientras no lo presente (a partir de esa fecha) queda "SAF" (Sin Apto
-- Fisico): sigue yendo a la clase pero no puede hacer actividad fisica.
CREATE TABLE apto_fisico (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    alumno_id           BIGINT NOT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    fecha_presentacion  DATE NOT NULL,
    documento_path      NVARCHAR(400) NULL,
    cargado_por_id      BIGINT NOT NULL,
    creado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_apto_fisico_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT fk_apto_fisico_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_apto_fisico_usuario FOREIGN KEY (cargado_por_id) REFERENCES usuario(id),
    CONSTRAINT ux_apto_fisico UNIQUE (alumno_id, ciclo_lectivo_id)
);

-- SAF por un dia puntual (ej. lesion, seccion 5.7 extension): el docente marca
-- al alumno como SAF solo para esa sesion de prueba, sin tocar su apto fisico
-- anual. Un resultado es o bien un valor numerico, o bien "saf" (nunca ambos).
ALTER TABLE resultado_prueba_fisica ADD saf BIT NOT NULL DEFAULT 0;
ALTER TABLE resultado_prueba_fisica ALTER COLUMN valor DECIMAL(10,2) NULL;
GO

ALTER TABLE resultado_prueba_fisica ADD CONSTRAINT ck_resultado_saf_valor
    CHECK ((saf = 1 AND valor IS NULL) OR (saf = 0 AND valor IS NOT NULL));
