alter table mos_resource add thumb_file_house_id bigint;
alter table mos_resource add constraint fk_mos_resource_thumb_file_house_id foreign key(thumb_file_house_id) references mos_file_house(id);
alter table mos_resource add suffix varchar(100);
update mos_resource set suffix = (LOWER(REVERSE(LEFT(REVERSE(pathname),INSTR(REVERSE(pathname),'.'))))) where 1=1;
alter table mos_resource add thumb_fails int default 0;