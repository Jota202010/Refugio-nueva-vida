-- Ejecutar una vez sobre la base empresa para habilitar varios profesionales
-- en el mismo turno y conservar la duración de reservas ya existentes.
USE empresa;

SET @tiene_indice_horario = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'horario_disponible'
    AND index_name = 'uq_horario_fecha_hora'
);
SET @sql_quitar_indice = IF(
  @tiene_indice_horario > 0,
  'ALTER TABLE horario_disponible DROP INDEX uq_horario_fecha_hora',
  'SELECT 1'
);
PREPARE quitar_indice FROM @sql_quitar_indice;
EXECUTE quitar_indice;
DEALLOCATE PREPARE quitar_indice;

SET @tiene_duracion = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'horario_disponible'
    AND column_name = 'duracion_minutos'
);
SET @sql_agregar_duracion = IF(
  @tiene_duracion = 0,
  'ALTER TABLE horario_disponible ADD COLUMN duracion_minutos INT NOT NULL DEFAULT 30',
  'SELECT 1'
);
PREPARE agregar_duracion FROM @sql_agregar_duracion;
EXECUTE agregar_duracion;
DEALLOCATE PREPARE agregar_duracion;

UPDATE horario_disponible
SET duracion_minutos = 30
WHERE duracion_minutos IS NULL OR duracion_minutos < 1;
