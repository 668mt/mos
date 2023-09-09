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
	@Select("select f.* from mos_file_house f \n" +
			"left join mos_resource r1 on f.id = r1.file_house_id\n" +
			"left join mos_resource r2 on f.id = r2.thumb_file_house_id\n" +
			"where r1.id is null\n" +
			"and r2.id is null \n" +
			"and f.updated_date < date_sub(now(), interval #{dayExpression} day_second)"
	)
	List<FileHouse> findNotUsedFileHouseList(@Param("dayExpression") String dayExpression);
	
	@Select("select r.file_house_id,\n" +
			"b.data_fragments_amount as data_fragments_amount\n" +
			",fh.data_fragments_count as current_fragments_amount\n" +
			"from mos_resource r \n" +
			"join mos_file_house fh on fh.id = r.file_house_id and (fh.back_fails is null or fh.back_fails < 3) and fh.file_status = 'OK'\n" +
			"join mos_dir d on r.dir_id = d.id and r.is_delete = 0\n" +
			"join mos_bucket b on b.id = d.bucket_id\n" +
			"where fh.data_fragments_count < b.data_fragments_amount and fh.data_fragments_count < #{aliveCount}\n" +
			"limit #{limit}")
	List<BackVo> findNeedBackFileHouseIds(@Param("aliveCount") Integer aliveCount,@Param("limit") int limit);
	
	@Select("select r.thumb_file_house_id as file_house_id,\n" +
			"b.data_fragments_amount as data_fragments_amount\n" +
			",fh.data_fragments_count as current_fragments_amount\n" +
			"from mos_resource r \n" +
			"join mos_file_house fh on fh.id = r.thumb_file_house_id and (fh.back_fails is null or fh.back_fails < 3) and fh.file_status = 'OK'\n" +
			"join mos_dir d on r.dir_id = d.id and r.is_delete = 0\n" +
			"join mos_bucket b on b.id = d.bucket_id\n" +
			"where fh.data_fragments_count < b.data_fragments_amount and fh.data_fragments_count < #{aliveCount}\n" +
			"limit #{limit}")
	List<BackVo> findNeedBackThumbFileHouseIds(@Param("aliveCount") Integer aliveCount,@Param("limit") int limit);
}
