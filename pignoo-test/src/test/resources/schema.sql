-- 测试库建表脚本，随 mvn test 由 Spring 自动执行，全部幂等
CREATE TABLE IF NOT EXISTS `test01` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NULL,
  `name` varchar(255) NULL,
  `money` decimal(32, 8) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `test02_for_mix_camel` (
  `test02_for_mix_camel_id` bigint NOT NULL AUTO_INCREMENT,
  `name_for_mix_camel` varchar(255) NULL,
  PRIMARY KEY (`test02_for_mix_camel_id`)
);
CREATE TABLE IF NOT EXISTS `Test02ForMastSame` (
  `id` bigint NOT NULL,
  `nameForMastSame` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `test03_data_type` (
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
CREATE TABLE IF NOT EXISTS `test03_setter1` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin` tinyint NULL,
  `isadmin` tinyint NULL,
  `is_admin` tinyint NULL,
  `is` tinyint NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `test03_setter2` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `is_admin` tinyint NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `test04` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  `code` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `test05` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `spring_test01` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  `code` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
-- 夹具实体对应的表
CREATE TABLE IF NOT EXISTS `fx_plain` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  `code` varchar(255) NULL,
  `money` decimal(32, 8) NULL,
  `qty` int NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `fx_pk_auto` (
  `auto_key` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  PRIMARY KEY (`auto_key`)
);
CREATE TABLE IF NOT EXISTS `fx_pk_non_auto` (
  `biz_key` varchar(64) NOT NULL,
  `name` varchar(255) NULL,
  PRIMARY KEY (`biz_key`)
);
CREATE TABLE IF NOT EXISTS `fx_must` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `fx_bool` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin` tinyint NULL,
  `is_active` tinyint NULL,
  `isadmin` tinyint NULL,
  `is_admin` tinyint NULL,
  `is` tinyint NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `fx_all_types` (
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
