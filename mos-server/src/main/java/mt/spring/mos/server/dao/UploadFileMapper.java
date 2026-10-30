package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.UploadFile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author Martin
 * @Date 2023/9/9
 */
@Repository
public interface UploadFileMapper extends BaseMapper<UploadFile> {
	@Select("select * from mos_upload_file where created_date < now() - make_interval(days => #{days})")
	List<UploadFile> findNotUsedFileHouseList(@Param("days") int days);
}
