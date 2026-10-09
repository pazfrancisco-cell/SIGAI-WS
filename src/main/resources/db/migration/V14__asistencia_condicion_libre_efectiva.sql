-- Seccion 5.2.3: al llegar a 20 faltas, el sistema evalua si >=50% estan
-- justificadas. Si es asi, habilita 8 adicionales (Condicion Libre a las 28).
-- Si no, la PROXIMA falta (21) activa la Condicion Libre directamente. Esta
-- decision es individual por alumno y, una vez tomada, debe persistir (no
-- reevaluarse en cada falta posterior) -> se guarda el umbral efectivo aqui.
-- Ver docs/DECISIONS.md ADR-008.
ALTER TABLE alumno_ciclo_lectivo ADD umbral_condicion_libre_efectivo INT NULL;
