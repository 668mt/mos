package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.Dir;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Repository
public interface DirMapper extends BaseMapper<Dir> {
//	@Update("update mos_dir set parent_id = #{desDirId} where parent_id = #{srcDirId}")
//	int changeDir(@Param("srcDirId") Long srcDirId, @Param("desDirId") Long desDirId);
}
