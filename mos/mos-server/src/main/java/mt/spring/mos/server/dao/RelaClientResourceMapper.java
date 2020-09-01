package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.RelaClientResource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Repository
public interface RelaClientResourceMapper extends BaseMapper<RelaClientResource> {
	@Select("select resource_id from (\n" +
			"select resource_id,count(resource_id) as resource_id_count from mos_rela_client_resource group by resource_id\n" +
			") a where a.resource_id_count < #{backTime}")
	List<Long> findNeedBackResourceIds(@Param("backTime") Integer backTime);
}
