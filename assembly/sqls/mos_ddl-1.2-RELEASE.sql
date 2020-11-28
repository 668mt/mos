alter table mos_resource add is_public int default 0;
alter table mos_user add name varchar(50);
alter table mos_bucket add default_is_public int default 0;
alter table mos_bucket add data_fragments_amount int default 1;

ALTER TABLE mos_access_control MODIFY COLUMN updated_by varchar(100) NULL;
ALTER TABLE mos_access_control MODIFY COLUMN created_by varchar(100) NULL;
ALTER TABLE mos_bucket MODIFY COLUMN updated_by varchar(100) NULL;
ALTER TABLE mos_bucket MODIFY COLUMN created_by varchar(100) NULL;

ALTER TABLE mos_bucket_grant MODIFY COLUMN updated_by varchar(100) NULL;
ALTER TABLE mos_bucket_grant MODIFY COLUMN created_by varchar(100) NULL;

ALTER TABLE mos_client MODIFY COLUMN updated_by varchar(100) NULL;
ALTER TABLE mos_client MODIFY COLUMN created_by varchar(100) NULL;

ALTER TABLE mos_dir MODIFY COLUMN updated_by varchar(100) NULL;
ALTER TABLE mos_dir MODIFY COLUMN created_by varchar(100) NULL;

ALTER TABLE mos_rela_client_resource MODIFY COLUMN updated_by varchar(100) NULL;
ALTER TABLE mos_rela_client_resource MODIFY COLUMN created_by varchar(100) NULL;

ALTER TABLE mos_resource MODIFY COLUMN updated_by varchar(100) NULL;
ALTER TABLE mos_resource MODIFY COLUMN created_by varchar(100) NULL;

ALTER TABLE mos_user MODIFY COLUMN updated_by varchar(100) NULL;
ALTER TABLE mos_user MODIFY COLUMN created_by varchar(100) NULL;

create index bucket_bucketName on mos_bucket(bucket_name);
create index dir_path on mos_dir(path);
create index resource_pathname on mos_resource(pathname);
alter table mos_user add failures int default 0;
alter table mos_user add locked int default 0;
alter table mos_access_control add secret_key varchar(200) not null;
alter table mos_access_control drop public_key;
alter table mos_access_control drop private_key;