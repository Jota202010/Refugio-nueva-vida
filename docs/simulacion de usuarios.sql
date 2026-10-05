-- Usuarios de prueba para la base de datos empresa.
-- Ambos usan la contraseña: password

USE empresa;

INSERT INTO usuario (
  usuario,
  nombre,
  email,
  telefono,
  direccion,
  contrasena,
  rol
) VALUES
(
  'admin',
  'Administrador Nueva Vida',
  'admin@refugionuevavida.com',
  '3000000001',
  'Refugio Nueva Vida',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'administrador'
),
(
  'usuario1',
  'Usuario de Prueba',
  'usuario1@correo.com',
  '3000000002',
  'Bogotá, Colombia',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'usuario'
)
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre),
  telefono = VALUES(telefono),
  direccion = VALUES(direccion),
  contrasena = VALUES(contrasena),
  rol = VALUES(rol);
