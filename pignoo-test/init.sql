/*
 * DataBase for testing pignoo-spring
 */
CREATE TABLE `pignoo`.`test01` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NULL,
  `name` varchar(255) NULL,
  `money` decimal(32, 8) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `pignoo`.`test02_for_mix_camel` (
  `test02_for_mix_camel_id` bigint NOT NULL AUTO_INCREMENT,
  `name_for_mix_camel` varchar(255) NULL,
  PRIMARY KEY (`test02_for_mix_camel_id`)
);
CREATE TABLE `pignoo`.`Test02ForMastSame` (
  `id` bigint NOT NULL,
  `nameForMastSame` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `pignoo`.`test03_data_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nbyte` tinyint NULL,
  `nshort` smallint NULL,
  `ninteger` int NULL,
  `nlong` bigint NULL,
  `nfloat` float NULL,
  `ndouble` double NULL,
  `nchar` char NULL,
  `nboolean` tinyint NULL,
  `xbyte` tinyint NULL,
  `xshort` smallint NULL,
  `xinteger` int NULL,
  `xlong` bigint NULL,
  `xfloat` float NULL,
  `xdouble` double NULL,
  `xchar` char NULL,
  `xboolean` tinyint NULL,
  `string` varchar(255) NULL,
  `big_integer` bigint NULL,
  `big_decimal` decimal(65, 30) NULL,
  `date` datetime NULL,
  `local_date` date NULL,
  `local_time` time NULL,
  `local_date_time` datetime NULL,
  `instant` datetime NULL,
  `zoned_date_time` datetime NULL,
  `offset_time` time NULL,
  `bytes` longblob NULL,
  `enums` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `pignoo`.`test03_setter1` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin` tinyint NULL,
  `isadmin` tinyint NULL,
  `is_admin` tinyint NULL,
  `is` tinyint NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `pignoo`.`test03_setter2` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `is_admin` tinyint NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `pignoo`.`test04` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  `code` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `pignoo`.`test05` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
/*
 * DataBase for testing pignoo-spring
 */
CREATE TABLE `pignoo`.`spring_test01` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  `code` varchar(255) NULL,
  PRIMARY KEY (`id`)
);