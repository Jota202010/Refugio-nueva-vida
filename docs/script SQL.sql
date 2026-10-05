CREATE DATABASE IF NOT EXISTS empresa
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE empresa;

CREATE TABLE usuario (
  id_usuario     INT NOT NULL AUTO_INCREMENT,
  usuario        VARCHAR(50) NOT NULL,
  nombre         VARCHAR(100) NOT NULL,
  email          VARCHAR(150) NOT NULL,
  telefono       VARCHAR(20) NULL,
  direccion      VARCHAR(255) NULL,
  contrasena     VARCHAR(255) NOT NULL,
  rol            ENUM('usuario','administrador') NOT NULL DEFAULT 'usuario',
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
  CONSTRAINT uq_usuario UNIQUE (usuario),
  CONSTRAINT uq_email UNIQUE (email)
);

CREATE TABLE perro (
  id_perro INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  edad VARCHAR(30) NULL,
  sexo ENUM('Macho','Hembra') NOT NULL,
  estado ENUM('RESCATADO','ABANDONADO','ACOGIDO') NOT NULL,
  esterilizado BOOLEAN NOT NULL DEFAULT FALSE,
  vacunado BOOLEAN NOT NULL DEFAULT FALSE,
  descripcion TEXT NULL,
  nivel_salud ENUM('SANO','ENFERMO','CRITICO') NOT NULL,
  sociabilidad ENUM('ALTA','MEDIA','BAJA') NOT NULL,
  estado_publicacion ENUM('EN_REFUGIO','PUBLICADO','EN_PROCESO','ADOPTADO','DEVUELTO') NOT NULL DEFAULT 'EN_REFUGIO',
  registro_medico TEXT NULL,
  fecha_ingreso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_perro PRIMARY KEY (id_perro)
);

CREATE TABLE foto_perro (
  id_foto INT NOT NULL AUTO_INCREMENT,
  id_perro INT NOT NULL,
  url_foto VARCHAR(500) NOT NULL,
  es_perfil BOOLEAN NOT NULL DEFAULT FALSE,
  orden INT NOT NULL DEFAULT 0,
  CONSTRAINT pk_foto PRIMARY KEY (id_foto),
  CONSTRAINT fk_foto_perro FOREIGN KEY (id_perro)
    REFERENCES perro(id_perro) ON DELETE CASCADE
);

CREATE TABLE cita (
  id_cita INT NOT NULL AUTO_INCREMENT,
  id_usuario INT NOT NULL,
  id_perro INT NOT NULL,
  id_admin INT NULL,
  estado ENUM('en_espera','pre_aprobada','confirmada','rechazada') NOT NULL DEFAULT 'en_espera',
  fecha_cita DATE NULL,
  hora_cita TIME NULL,
  sede VARCHAR(150) NULL,
  fecha_solicitud DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_decision DATETIME NULL,
  tipo_vivienda ENUM('casa','apartamento') NULL,
  propiedad ENUM('propia','alquilada') NULL,
  permiten_mascotas ENUM('si','no','no_aplica') NULL,
  num_personas INT NULL,
  todos_acuerdo BOOLEAN NULL,
  perros_antes BOOLEAN NULL,
  mascotas_actual BOOLEAN NULL,
  mascotas_anteriores TEXT NULL,
  horas_solo VARCHAR(30) NULL,
  puede_pasear BOOLEAN NULL,
  responsable VARCHAR(100) NULL,
  cubre_vet BOOLEAN NULL,
  cubre_emergencias BOOLEAN NULL,
  motivacion TEXT NULL,
  tipo_perro_buscado TEXT NULL,
  cond_no_abandono BOOLEAN NOT NULL DEFAULT FALSE,
  cond_seguimiento BOOLEAN NOT NULL DEFAULT FALSE,
  cond_evaluacion BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT pk_cita PRIMARY KEY (id_cita),
  CONSTRAINT fk_cita_usuario FOREIGN KEY (id_usuario)
    REFERENCES usuario(id_usuario) ON DELETE CASCADE,
  CONSTRAINT fk_cita_perro FOREIGN KEY (id_perro)
    REFERENCES perro(id_perro) ON DELETE CASCADE,
  CONSTRAINT fk_cita_admin FOREIGN KEY (id_admin)
    REFERENCES usuario(id_usuario) ON DELETE SET NULL
);

CREATE TABLE horario_disponible (
  id_horario INT NOT NULL AUTO_INCREMENT,
  fecha DATE NOT NULL,
  hora TIME NOT NULL,
  ocupado BOOLEAN NOT NULL DEFAULT FALSE,
  duracion_minutos INT NOT NULL DEFAULT 30,
  id_cita INT NULL,
  CONSTRAINT pk_horario PRIMARY KEY (id_horario),
  CONSTRAINT uq_horario_cita UNIQUE (id_cita),
  CONSTRAINT fk_horario_cita FOREIGN KEY (id_cita)
    REFERENCES cita(id_cita) ON DELETE SET NULL
);

CREATE TABLE configuracion_horario (
  id INT NOT NULL,
  duracion_minutos INT NOT NULL DEFAULT 30,
  profesionales_disponibles INT NOT NULL DEFAULT 1,
  CONSTRAINT pk_configuracion_horario PRIMARY KEY (id)
);

CREATE TABLE horario_dia_semana (
  id_dia INT NOT NULL AUTO_INCREMENT,
  dia_semana VARCHAR(16) NOT NULL,
  manana_inicio TIME NULL,
  manana_fin TIME NULL,
  tarde_inicio TIME NULL,
  tarde_fin TIME NULL,
  CONSTRAINT pk_horario_dia_semana PRIMARY KEY (id_dia),
  CONSTRAINT uq_horario_dia_semana UNIQUE (dia_semana)
);

CREATE TABLE horario_excepcion (
  id_excepcion INT NOT NULL AUTO_INCREMENT,
  fecha DATE NOT NULL,
  cerrado BOOLEAN NOT NULL DEFAULT FALSE,
  manana_inicio TIME NULL,
  manana_fin TIME NULL,
  tarde_inicio TIME NULL,
  tarde_fin TIME NULL,
  CONSTRAINT pk_horario_excepcion PRIMARY KEY (id_excepcion),
  CONSTRAINT uq_horario_excepcion_fecha UNIQUE (fecha)
);

CREATE TABLE historial_estado (
  id_historial INT NOT NULL AUTO_INCREMENT,
  id_perro INT NOT NULL,
  id_cita INT NULL,
  id_usuario INT NULL,
  estado_anterior ENUM('EN_REFUGIO','PUBLICADO','EN_PROCESO','ADOPTADO','DEVUELTO') NULL,
  estado_nuevo ENUM('EN_REFUGIO','PUBLICADO','EN_PROCESO','ADOPTADO','DEVUELTO') NOT NULL,
  fecha_cambio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  origen ENUM('ADMIN','SISTEMA') NOT NULL,
  CONSTRAINT pk_historial PRIMARY KEY (id_historial),
  CONSTRAINT uq_historial_cita UNIQUE (id_cita),
  CONSTRAINT fk_historial_perro FOREIGN KEY (id_perro)
    REFERENCES perro(id_perro) ON DELETE CASCADE,
  CONSTRAINT fk_historial_cita FOREIGN KEY (id_cita)
    REFERENCES cita(id_cita) ON DELETE SET NULL,
  CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario)
    REFERENCES usuario(id_usuario) ON DELETE SET NULL
);
