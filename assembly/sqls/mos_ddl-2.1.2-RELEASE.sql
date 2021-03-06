alter table mos_user add last_login_date timestamp;
alter table mos_user add login_times bigint default 0;

alter table mos_file_house_item add unique index unique_file_house_item_index(file_house_id,chunk_index);