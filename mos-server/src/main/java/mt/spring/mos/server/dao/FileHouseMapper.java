package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.FileHouse;
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
}
