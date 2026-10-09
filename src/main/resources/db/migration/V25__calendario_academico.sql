-- Calendario academico (idea propuesta, no forma parte de la Propuesta Tecnica
-- original): eventos institucionales o de un curso/division puntual, visibles
-- para todos los roles; la carga queda restringida a SUPERADMIN/DIRECTIVO/
-- ADMINISTRATIVO, igual que el resto de las pantallas institucionales.
CREATE TABLE evento_academico (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    titulo              NVARCHAR(160) NOT NULL,
    descripcion         NVARCHAR(500) NULL,
    tipo                VARCHAR(20) NOT NULL CHECK (tipo IN ('FERIADO','EXAMEN','ACTO','REUNION','OTRO')),
    fecha_inicio        DATE NOT NULL,
    fecha_fin           DATE NULL,
    curso_division_id   BIGINT NULL,
    ciclo_lectivo_id    BIGINT NOT NULL,
    creado_por_id       BIGINT NOT NULL,
    creado_en           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_evento_academico_curso FOREIGN KEY (curso_division_id) REFERENCES curso_division(id),
    CONSTRAINT fk_evento_academico_ciclo FOREIGN KEY (ciclo_lectivo_id) REFERENCES ciclo_lectivo(id),
    CONSTRAINT fk_evento_academico_usuario FOREIGN KEY (creado_por_id) REFERENCES usuario(id)
);

CREATE INDEX ix_evento_academico_ciclo ON evento_academico(ciclo_lectivo_id, fecha_inicio);
