-- Alternativa manual si Hibernate ddl-auto=update esta desactivado.
-- Ejecutar solo si estas columnas aun no existen.
ALTER TABLE cita
  ADD COLUMN acepta_evaluacion_ia BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN evaluacion_ia_estado VARCHAR(30) NULL,
  ADD COLUMN evaluacion_ia_motivo VARCHAR(1000) NULL,
  ADD COLUMN evaluacion_ia_fecha DATETIME NULL;
