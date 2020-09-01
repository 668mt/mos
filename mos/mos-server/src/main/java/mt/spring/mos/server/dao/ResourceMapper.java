package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.Resource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Repository
public interface ResourceMapper extends BaseMapper<Resource> {
	
	@Select("select * from mos_resource r,mos_dir d where r.dir_id = d.id and d.bucket_id = #{bucketId} and r.pathname = #{pathname}")
	Resource findResourceByPathnameAndBucketId(@Param("pathname") String pathname, @Param("bucketId") Long bucketId);
}
