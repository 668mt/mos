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
	
	List<DirAndResourceVo> findChildDirAndResourceList(@Param("keyWord") String keyWord, @Param("bucketId") Long bucketId, @Param("dirId") Long dirId);
	
	@Select("select distinct r.* from mos_resource r,mos_rela_client_resource cr,mos_client c\n" +
			"where r.id = cr.resource_id and cr.client_id = c.client_id\n" +
			"and\tr.file_house_id is null\n" +
			"and c.status = 'UP'\n")
	List<Resource> findNeedConvertToFileHouse();
	
	List<Resource> findNeedGenerateThumb(@Param("suffixs") List<String> suffixs);
}
