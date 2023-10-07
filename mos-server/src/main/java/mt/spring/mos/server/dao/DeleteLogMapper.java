package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.FileHouseDeleteLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.special.InsertListMapper;

import java.util.List;

/**
 * @Author Martin
 * @Date 2023/10/6
 */
@Repository
public interface DeleteLogMapper extends BaseMapper<FileHouseDeleteLog>, InsertListMapper<FileHouseDeleteLog> {
	
	@Select("select * from mos_file_house_delete_log where created_date < date_sub(now(), interval #{dayExpression} day_second) limit ${limit}")
	List<FileHouseDeleteLog> findListBeforeDaysLimit(@Param("dayExpression") String dayExpression, @Param("limit") int limit);
}
