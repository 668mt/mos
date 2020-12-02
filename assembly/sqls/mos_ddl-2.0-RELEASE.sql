CREATE TABLE `mos_file_house` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `md5` varchar(100) DEFAULT NULL,
  `pathname` varchar(100) DEFAULT NULL,
  `chunks` int(11) DEFAULT NULL,
  `size_byte` bigint(20) DEFAULT NULL,
  `file_status` varchar(500) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `updated_date` datetime DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `md5_size` (`md5`,`size_byte`)
) ENGINE=InnoDB AUTO_INCREMENT=265 DEFAULT CHARSET=utf8;

CREATE TABLE `mos_file_house_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `chunk_index` int(11) DEFAULT NULL,
  `size_byte` bigint(20) DEFAULT NULL,
  `md5` varchar(100) DEFAULT NULL,
  `file_house_id` bigint(20) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `updated_date` datetime DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `PK_mos_file_house_item_file_house_id_mos_file_house_id` (`file_house_id`),
  CONSTRAINT `PK_mos_file_house_item_file_house_id_mos_file_house_id` FOREIGN KEY (`file_house_id`) REFERENCES `mos_file_house` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3039 DEFAULT CHARSET=utf8;

CREATE TABLE `mos_file_house_rela_client` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `client_id` varchar(100) DEFAULT NULL,
  `file_house_id` bigint(20) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `updated_date` datetime DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `PK_mos_file_house_rela_client_file_house_id_mos_file_house_id` (`file_house_id`),
  CONSTRAINT `PK_mos_file_house_rela_client_file_house_id_mos_file_house_id` FOREIGN KEY (`file_house_id`) REFERENCES `mos_file_house` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=295 DEFAULT CHARSET=utf8;

alter table mos_resource add file_house_id bigint;
alter table mos_resource add unique index pathname(pathname,dir_id);
alter table mos_resource add constraint fk_resource_file_house_id foreign key(file_house_id) references mos_file_house(id);
alter table mos_client add keep_space_byte bigint;
alter table mos_file_house add encode int default 0;