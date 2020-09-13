package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.vo.DirAndResourceVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Repository
public interface ResourceMapper extends BaseMapper<Resource> {
	
	@Select("select * from mos_resource r,mos_dir d where r.dir_id = d.id and d.bucket_id = #{bucketId} and r.pathname = #{pathname}")
	Resource findResourceByPathnameAndBucketId(@Param("pathname") String pathname, @Param("bucketId") Long bucketId);
	
//	@Select("select * from (\n" +
//			"select 1 as is_dir,d.id,d.`path` as path,null as size_byte,d.created_date,d.created_by,d.updated_date,d.updated_by from mos_dir d,mos_bucket b\n" +
//			"where d.bucket_id = b.id\n" +
//			"and parent_id = #{dirId}\n" +
//			"and b.user_id = #{userId}\n" +
//			"union all\n" +
//			"select 0 as is_dir,r.id,r.pathname,r.size_byte,r.created_date,r.created_by,r.updated_date,r.updated_by from mos_dir d,mos_bucket b,mos_resource r\n" +
//			"where d.bucket_id = b.id\n" +
//			"and d.id = r.dir_id\n" +
//			"and b.user_id = #{userId}\n" +
//			"and d.id = #{dirId}\n" +
//			") a\n" +
//			"order by is_dir desc,id desc ")
	List<DirAndResourceVo> findChildDirAndResourceList(@Param("keyWord") String keyWord, @Param("userId") Long userId, @Param("dirId") Long dirId);
}
