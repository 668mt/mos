package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.FileHouse;
import mt.spring.mos.server.entity.vo.BackVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Repository
public interface FileHouseMapper extends BaseMapper<FileHouse> {
	@Select("select * from mos_file_house \n" +
			"where updated_date < date_sub(now(), interval #{dayExpression} day_second)\n" +
			"and id not in( select file_house_id from mos_resource where file_house_id is not null)")
	List<FileHouse> findNotUsedFileHouseList(@Param("dayExpression") String dayExpression);
	
	@Select("select * from(\n" +
			"select r.file_house_id,\n" +
			"max(b.data_fragments_amount) as data_fragments_amount,\n" +
			"(select count(0) from mos_file_house_rela_client fhrc where fhrc.file_house_id = r.file_house_id) as current_fragments_amount\n" +
			"from mos_resource r \n" +
			"join mos_dir d on r.file_house_id is not null and r.dir_id = d.id\n" +
			"join mos_bucket b on b.id = d.bucket_id\n" +
			"group by file_house_id\n" +
			") a where a.current_fragments_amount < a.data_fragments_amount and a.current_fragments_amount < #{aliveCount}\n" +
			";")
	List<BackVo> findNeedBackResourceIds(@Param("aliveCount") Integer aliveCount);
}
