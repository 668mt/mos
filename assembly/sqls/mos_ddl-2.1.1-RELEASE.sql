alter table mos_dir add unique index path_bucket_id(path,bucket_id);

ALTER TABLE mos_rela_client_resource DROP FOREIGN KEY PK_mos_rela_client_resource_client_id_mos_client_client_id;
ALTER TABLE mos_client DROP PRIMARY KEY;
alter table mos_client add id bigint primary key auto_increment;
alter table mos_client add unique index unique_client_id(client_id);
ALTER TABLE mos_client MODIFY COLUMN ip varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;
ALTER TABLE mos_client MODIFY COLUMN port int(11) NOT NULL;

update mos_client_work_log wl set client_id = (select c.id from mos_client c where c.client_id = wl.client_id) where 1=1;
alter table mos_client_work_log modify column client_id bigint;
alter table mos_client_work_log add constraint fk_work_log_client_id foreign key(client_id) references mos_client(id);

update mos_file_house_rela_client wl set client_id = (select c.id from mos_client c where c.client_id = wl.client_id) where 1=1;
alter table mos_file_house_rela_client modify column client_id bigint;
alter table mos_file_house_rela_client add constraint fk_mos_file_house_rela_client_client_id foreign key(client_id) references mos_client(id);

update mos_rela_client_resource wl set client_id = (select c.id from mos_client c where c.client_id = wl.client_id) where 1=1;
alter table mos_rela_client_resource modify column client_id bigint;
alter table mos_rela_client_resource add constraint fk_mos_rela_client_resource_client_id foreign key(client_id) references mos_client(id);
ALTER TABLE mos_client CHANGE client_id name varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;

create table `mos_audit` (
     `id`  bigint AUTO_INCREMENT  ,
     `bucket_id`  bigint   ,
     `user_id`  bigint   ,
     `open_id`  bigint   ,
     `target`  text   ,
     `type`  varchar(100)   ,
     `bytes`  bigint   ,
     `action`  varchar(100)   ,
     `remark`  text  ,
     `created_date`  datetime   ,
     `created_by`  varchar(100)   ,
     `updated_date`  datetime   ,
     `updated_by`  varchar(100)   ,
     primary key(`id`)
);
ALTER TABLE `mos_audit` ADD CONSTRAINT `PK_mos_audit_bucket_id_mos_bucket_id` FOREIGN KEY(`bucket_id`) REFERENCES `mos_bucket`(`id`)  ON DELETE CASCADE ON UPDATE CASCADE;
alter table mos_resource add visits bigint default 0;
alter table mos_audit add index audit_user_id(user_id);
alter table mos_audit add index audit_open_id(open_id);
alter table mos_audit add index audit_target(open_id);
alter table mos_audit add index audit_type(type);
alter table mos_audit add index audit_action(action);
alter table mos_audit add ip varchar(100);
