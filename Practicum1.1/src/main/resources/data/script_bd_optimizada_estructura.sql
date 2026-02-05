SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- =====================================================
-- Schema MovieDatabase (Base de datos MovieDatabase)
-- =====================================================
DROP SCHEMA IF EXISTS `MovieDatabase` ;

CREATE SCHEMA IF NOT EXISTS `MovieDatabase` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ;
USE `MovieDatabase` ;

-- =====================================================
-- Table `MovieDatabase`.`Idiomas`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Idiomas` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Idiomas` (
  `iso_639_1` CHAR(2) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`iso_639_1`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Idiomas` ADD INDEX `idx_idiomas_iso` (`iso_639_1` ASC);
ALTER TABLE `MovieDatabase`.`Idiomas` ADD INDEX `idx_idiomas_name` (`name`(30));

-- =====================================================
-- Table `MovieDatabase`.`Paises`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Paises` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Paises` (
  `iso_3166_1` CHAR(2) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`iso_3166_1`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Paises` ADD INDEX `idx_paises_iso` (`iso_3166_1` ASC);
ALTER TABLE `MovieDatabase`.`Paises` ADD INDEX `idx_paises_name` (`name`(50));

-- =====================================================
-- Table `MovieDatabase`.`Colecciones`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Colecciones` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Colecciones` (
  `idColeccion` INT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `poster_path` VARCHAR(255) NULL DEFAULT NULL,
  `backdrop_path` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`idColeccion`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Colecciones` ADD INDEX `idx_colecciones_id` (`idColeccion` ASC);
ALTER TABLE `MovieDatabase`.`Colecciones` ADD INDEX `idx_colecciones_name` (`name`(50));

-- =====================================================
-- Table `MovieDatabase`.`Peliculas`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Peliculas` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Peliculas` (
  `idPelicula` INT NOT NULL,
  `imdb_id` VARCHAR(20) NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `original_title` VARCHAR(150) NOT NULL,
  `overview` TEXT NULL DEFAULT NULL,
  `tagline` VARCHAR(255) NULL DEFAULT NULL,
  `adult` TINYINT(1) NOT NULL DEFAULT 0,
  `video` TINYINT(1) NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'Released',
  `release_date` DATE NOT NULL,
  `budget` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `revenue` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `runtime` INT NULL DEFAULT NULL,
  `popularity` DECIMAL(10,4) NULL DEFAULT 0.0000,
  `vote_average` DECIMAL(3,1) NULL DEFAULT 0.0,
  `vote_count` INT NULL DEFAULT 0,
  `homepage` VARCHAR(255) NULL DEFAULT NULL,
  `poster_path` VARCHAR(255) NULL DEFAULT NULL,
  `original_language` CHAR(2) NULL DEFAULT NULL,
  PRIMARY KEY (`idPelicula`),
  INDEX `fk_pelicula_original_language` (`original_language` ASC) ,
  CONSTRAINT `fk_pelicula_original_language`
    FOREIGN KEY (`original_language`)
    REFERENCES `MovieDatabase`.`Idiomas` (`iso_639_1`)
    ON DELETE SET NULL
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Peliculas` ADD INDEX `idx_peliculas_id` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Peliculas` ADD INDEX `idx_peliculas_title` (`title`(50));
ALTER TABLE `MovieDatabase`.`Peliculas` ADD INDEX `idx_peliculas_imdb` (`imdb_id`);

-- =====================================================
-- Table `MovieDatabase`.`Peliculas_Colecciones`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Peliculas_Colecciones` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Peliculas_Colecciones` (
  `idPelicula` INT NOT NULL,
  `idColeccion` INT NOT NULL,
  PRIMARY KEY (`idPelicula`, `idColeccion`),
  INDEX `fk_pelicula_coleccion_coleccion` (`idColeccion` ASC) ,
  CONSTRAINT `fk_pelicula_coleccion_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_pelicula_coleccion_coleccion`
    FOREIGN KEY (`idColeccion`)
    REFERENCES `MovieDatabase`.`Colecciones` (`idColeccion`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Peliculas_Colecciones` ADD INDEX `idx_pel_col_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Peliculas_Colecciones` ADD INDEX `idx_pel_col_colid` (`idColeccion` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Generos`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Generos` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Generos` (
  `idGenero` INT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`idGenero`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Generos` ADD INDEX `idx_generos_id` (`idGenero` ASC);
ALTER TABLE `MovieDatabase`.`Generos` ADD INDEX `idx_generos_name` (`name`(30));

-- =====================================================
-- Table `MovieDatabase`.`Peliculas_Generos`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Peliculas_Generos` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Peliculas_Generos` (
  `idPelicula` INT NOT NULL,
  `idGenero` INT NOT NULL,
  PRIMARY KEY (`idPelicula`, `idGenero`),
  INDEX `fk_pelicula_genero_genero` (`idGenero` ASC) ,
  CONSTRAINT `fk_pelicula_genero_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_pelicula_genero_genero`
    FOREIGN KEY (`idGenero`)
    REFERENCES `MovieDatabase`.`Generos` (`idGenero`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Peliculas_Generos` ADD INDEX `idx_pel_gen_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Peliculas_Generos` ADD INDEX `idx_pel_gen_genid` (`idGenero` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Companias`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Companias` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Companias` (
  `idCompania` INT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `origin_country` CHAR(2) NULL DEFAULT NULL,
  PRIMARY KEY (`idCompania`),
  INDEX `fk_compania_pais` (`origin_country` ASC) ,
  CONSTRAINT `fk_compania_pais`
    FOREIGN KEY (`origin_country`)
    REFERENCES `MovieDatabase`.`Paises` (`iso_3166_1`)
    ON DELETE SET NULL
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Companias` ADD INDEX `idx_companias_id` (`idCompania` ASC);
ALTER TABLE `MovieDatabase`.`Companias` ADD INDEX `idx_companias_name` (`name`(50));

-- =====================================================
-- Table `MovieDatabase`.`Peliculas_Companias`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Peliculas_Companias` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Peliculas_Companias` (
  `idPelicula` INT NOT NULL,
  `idCompania` INT NOT NULL,
  PRIMARY KEY (`idPelicula`, `idCompania`),
  INDEX `fk_pelicula_compania_compania` (`idCompania` ASC) ,
  CONSTRAINT `fk_pelicula_compania_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_pelicula_compania_compania`
    FOREIGN KEY (`idCompania`)
    REFERENCES `MovieDatabase`.`Companias` (`idCompania`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Peliculas_Companias` ADD INDEX `idx_pel_comp_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Peliculas_Companias` ADD INDEX `idx_pel_comp_compid` (`idCompania` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Peliculas_Paises`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Peliculas_Paises` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Peliculas_Paises` (
  `idPelicula` INT NOT NULL,
  `iso_3166_1` CHAR(2) NOT NULL,
  PRIMARY KEY (`idPelicula`, `iso_3166_1`),
  INDEX `fk_pelicula_pais_pais` (`iso_3166_1` ASC) ,
  CONSTRAINT `fk_pelicula_pais_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_pelicula_pais_pais`
    FOREIGN KEY (`iso_3166_1`)
    REFERENCES `MovieDatabase`.`Paises` (`iso_3166_1`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Peliculas_Paises` ADD INDEX `idx_pel_pais_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Peliculas_Paises` ADD INDEX `idx_pel_pais_isoid` (`iso_3166_1` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Peliculas_Idiomas`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Peliculas_Idiomas` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Peliculas_Idiomas` (
  `idPelicula` INT NOT NULL,
  `iso_639_1` CHAR(2) NOT NULL,
  PRIMARY KEY (`idPelicula`, `iso_639_1`),
  INDEX `fk_pelicula_idioma_idioma` (`iso_639_1` ASC) ,
  CONSTRAINT `fk_pelicula_idioma_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_pelicula_idioma_idioma`
    FOREIGN KEY (`iso_639_1`)
    REFERENCES `MovieDatabase`.`Idiomas` (`iso_639_1`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Peliculas_Idiomas` ADD INDEX `idx_pel_idiomas_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Peliculas_Idiomas` ADD INDEX `idx_pel_idiomas_isoid` (`iso_639_1` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Actores`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Actores` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Actores` (
  `idActor` INT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `gender` TINYINT NULL DEFAULT NULL,
  `profile_path` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`idActor`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Actores` ADD INDEX `idx_actores_id` (`idActor` ASC);
ALTER TABLE `MovieDatabase`.`Actores` ADD INDEX `idx_actores_name` (`name`(50));

-- =====================================================
-- Table `MovieDatabase`.`Asignaciones`
-- Permite multiples personajes del mismo actor en la misma pelicula
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Asignaciones` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Asignaciones` (
  `idCast` INT NOT NULL AUTO_INCREMENT,
  `idActor` INT NOT NULL,
  `idPelicula` INT NOT NULL,
  `character` VARCHAR(150) NULL DEFAULT NULL,
  `cast_order` INT NULL DEFAULT NULL,
  `credit_id` VARCHAR(50) NULL DEFAULT NULL,
  PRIMARY KEY (`idCast`),
  UNIQUE INDEX `uq_cast_credit_id` (`credit_id` ASC) ,
  INDEX `idx_actor_pelicula` (`idActor` ASC, `idPelicula` ASC) ,
  INDEX `fk_cast_pelicula` (`idPelicula` ASC) ,
  CONSTRAINT `fk_cast_actor`
    FOREIGN KEY (`idActor`)
    REFERENCES `MovieDatabase`.`Actores` (`idActor`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_cast_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Asignaciones` ADD INDEX `idx_asign_actorid` (`idActor` ASC);
ALTER TABLE `MovieDatabase`.`Asignaciones` ADD INDEX `idx_asign_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Asignaciones` ADD INDEX `idx_asign_compuesto` (`idPelicula` ASC, `idActor` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Personal`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Personal` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Personal` (
  `idPersonal` INT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `gender` TINYINT NULL DEFAULT NULL,
  `profile_path` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`idPersonal`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Personal` ADD INDEX `idx_personal_id` (`idPersonal` ASC);
ALTER TABLE `MovieDatabase`.`Personal` ADD INDEX `idx_personal_name` (`name`(50));

-- =====================================================
-- Table `MovieDatabase`.`Departamentos`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Departamentos` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Departamentos` (
  `idDepartamento` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL UNIQUE,
  PRIMARY KEY (`idDepartamento`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Departamentos` ADD INDEX `idx_departamentos_id` (`idDepartamento` ASC);
ALTER TABLE `MovieDatabase`.`Departamentos` ADD UNIQUE INDEX `idx_departamentos_name` (`name`(50));

-- =====================================================
-- Table `MovieDatabase`.`Trabajos`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Trabajos` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Trabajos` (
  `idTrabajo` INT NOT NULL AUTO_INCREMENT,
  `idPersonal` INT NOT NULL,
  `idPelicula` INT NOT NULL,
  `idDepartamento` INT NOT NULL,
  `job` VARCHAR(100) NOT NULL,
  `credit_id` VARCHAR(50) NULL DEFAULT NULL,
  PRIMARY KEY (`idTrabajo`),
  UNIQUE INDEX `uq_trabajo_credit_id` (`credit_id` ASC) ,
  INDEX `idx_personal_pelicula_job` (`idPersonal` ASC, `idPelicula` ASC, `job` ASC) ,
  INDEX `fk_trabajo_pelicula` (`idPelicula` ASC) ,
  INDEX `fk_trabajo_departamento` (`idDepartamento` ASC) ,
  CONSTRAINT `fk_trabajo_personal`
    FOREIGN KEY (`idPersonal`)
    REFERENCES `MovieDatabase`.`Personal` (`idPersonal`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_trabajo_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_trabajo_departamento`
    FOREIGN KEY (`idDepartamento`)
    REFERENCES `MovieDatabase`.`Departamentos` (`idDepartamento`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Trabajos` ADD INDEX `idx_trabajos_personalid` (`idPersonal` ASC);
ALTER TABLE `MovieDatabase`.`Trabajos` ADD INDEX `idx_trabajos_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Trabajos` ADD INDEX `idx_trabajos_departid` (`idDepartamento` ASC);
ALTER TABLE `MovieDatabase`.`Trabajos` ADD INDEX `idx_trabajos_compuesto` (`idPelicula` ASC, `idPersonal` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Keywords`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Keywords` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Keywords` (
  `idKeyword` INT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`idKeyword`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Keywords` ADD INDEX `idx_keywords_id` (`idKeyword` ASC);
ALTER TABLE `MovieDatabase`.`Keywords` ADD INDEX `idx_keywords_name` (`name`(50));

-- =====================================================
-- Table `MovieDatabase`.`Peliculas_Keywords`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Peliculas_Keywords` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Peliculas_Keywords` (
  `idPelicula` INT NOT NULL,
  `idKeyword` INT NOT NULL,
  PRIMARY KEY (`idPelicula`, `idKeyword`),
  INDEX `fk_pelicula_keyword_keyword` (`idKeyword` ASC) ,
  CONSTRAINT `fk_pelicula_keyword_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_pelicula_keyword_keyword`
    FOREIGN KEY (`idKeyword`)
    REFERENCES `MovieDatabase`.`Keywords` (`idKeyword`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Peliculas_Keywords` ADD INDEX `idx_pel_kw_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Peliculas_Keywords` ADD INDEX `idx_pel_kw_kwid` (`idKeyword` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Usuarios`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Usuarios` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Usuarios` (
  `userId` INT NOT NULL,
  PRIMARY KEY (`userId`))
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Usuarios` ADD INDEX `idx_usuarios_id` (`userId` ASC);

-- =====================================================
-- Table `MovieDatabase`.`Calificaciones`
-- =====================================================
DROP TABLE IF EXISTS `MovieDatabase`.`Calificaciones` ;

CREATE TABLE IF NOT EXISTS `MovieDatabase`.`Calificaciones` (
  `idCalificacion` INT NOT NULL AUTO_INCREMENT,
  `userId` INT NOT NULL,
  `idPelicula` INT NOT NULL,
  `rating` DECIMAL(3,1) NOT NULL,
  `timestamp` BIGINT NOT NULL,
  PRIMARY KEY (`idCalificacion`),
  UNIQUE INDEX `uq_calificacion_usuario_pelicula` (`userId` ASC, `idPelicula` ASC) ,
  INDEX `fk_calificacion_pelicula` (`idPelicula` ASC) ,
  INDEX `idx_rating_timestamp` (`rating` ASC, `timestamp` ASC) ,
  CONSTRAINT `fk_calificacion_usuario`
    FOREIGN KEY (`userId`)
    REFERENCES `MovieDatabase`.`Usuarios` (`userId`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_calificacion_pelicula`
    FOREIGN KEY (`idPelicula`)
    REFERENCES `MovieDatabase`.`Peliculas` (`idPelicula`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

-- ÍNDICES PARA OPTIMIZACIÓN
ALTER TABLE `MovieDatabase`.`Calificaciones` ADD INDEX `idx_califc_userid` (`userId` ASC);
ALTER TABLE `MovieDatabase`.`Calificaciones` ADD INDEX `idx_califc_pelid` (`idPelicula` ASC);
ALTER TABLE `MovieDatabase`.`Calificaciones` ADD INDEX `idx_califc_compuesto` (`userId` ASC, `idPelicula` ASC);

-- =====================================================
-- CONFIGURACIÓN MYSQL PARA OPTIMIZAR INSERCIONES
-- =====================================================
-- Recomendación: usar 50% de RAM disponible
-- NOTA: Si da error, significa que el buffer pool ya está inicializado
-- Se debe configurar en my.cnf/my.ini: innodb_buffer_pool_size=4G
SET GLOBAL innodb_buffer_pool_size=4294967296;

-- Modo de flush optimizado para carga masiva
-- 0 = no flush a disco (más rápido, menos seguro)
-- 1 = flush a cada transacción (ACID, más lento)
-- 2 = flush a cada segundo (recomendado) USAR ESTE
SET GLOBAL innodb_flush_log_at_trx_commit=2;

-- Aumentar timeout de conexión
SET GLOBAL wait_timeout=28800;
SET GLOBAL interactive_timeout=28800;

-- Aumentar máximo de conexiones
SET GLOBAL max_connections=1000;

-- Aumentar tamaño máximo de packet para batch inserts
SET GLOBAL max_allowed_packet=67108864;

-- Mejorar rendimiento de inserts múltiples
SET GLOBAL bulk_insert_buffer_size=16777216;

-- =====================================================
-- ANALIZAR TABLAS (IMPORTANTE PARA OPTIMIZACIÓN)
-- =====================================================

ANALYZE TABLE `MovieDatabase`.`Idiomas`;
ANALYZE TABLE `MovieDatabase`.`Paises`;
ANALYZE TABLE `MovieDatabase`.`Colecciones`;
ANALYZE TABLE `MovieDatabase`.`Peliculas`;
ANALYZE TABLE `MovieDatabase`.`Peliculas_Colecciones`;
ANALYZE TABLE `MovieDatabase`.`Generos`;
ANALYZE TABLE `MovieDatabase`.`Peliculas_Generos`;
ANALYZE TABLE `MovieDatabase`.`Companias`;
ANALYZE TABLE `MovieDatabase`.`Peliculas_Companias`;
ANALYZE TABLE `MovieDatabase`.`Peliculas_Paises`;
ANALYZE TABLE `MovieDatabase`.`Peliculas_Idiomas`;
ANALYZE TABLE `MovieDatabase`.`Actores`;
ANALYZE TABLE `MovieDatabase`.`Asignaciones`;
ANALYZE TABLE `MovieDatabase`.`Personal`;
ANALYZE TABLE `MovieDatabase`.`Departamentos`;
ANALYZE TABLE `MovieDatabase`.`Trabajos`;
ANALYZE TABLE `MovieDatabase`.`Keywords`;
ANALYZE TABLE `MovieDatabase`.`Peliculas_Keywords`;
ANALYZE TABLE `MovieDatabase`.`Usuarios`;
ANALYZE TABLE `MovieDatabase`.`Calificaciones`;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
