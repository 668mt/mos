package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.RelaClientResource;
import mt.spring.mos.server.entity.vo.BackVo;
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
	
	@Select("select resource_id,data_fragments_amount from (\n" +
			"\tselect resource_id,\n" +
			"\t\tcount(resource_id) as resource_id_count,\n" +
			"\t\t(select IFNULL(b.data_fragments_amount,1) from mos_resource r,mos_dir d,mos_bucket b\n" +
			"\t\twhere r.dir_id = d.id and d.bucket_id = b.id and r.id = rcr.resource_id) as data_fragments_amount\n" +
			"\tfrom mos_rela_client_resource rcr group by resource_id\n" +
			") a where a.resource_id_count < a.data_fragments_amount and a.resource_id_count < #{aliveCount}")
	List<BackVo> findNeedBackResourceIds(@Param("aliveCount") Integer aliveCount);
}
