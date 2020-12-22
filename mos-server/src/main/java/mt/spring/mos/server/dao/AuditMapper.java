package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.vo.audit.ChartBy;
import mt.spring.mos.server.entity.vo.audit.ChartVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Repository
public interface AuditMapper extends BaseMapper<Audit> {
	@Select("select IFNULL(sum(bytes),0) as bytes from mos_audit where bucket_id = #{bucketId} and bytes > 0 and created_date >= #{startDate} and type = #{type}")
	long findTotalFlowFromDate(@Param("bucketId") Long bucketId, @Param("type") Audit.Type type, @Param("startDate") String startDate);
	
	@Select("select count(0) as requests from mos_audit where bucket_id = #{bucketId} and created_date >= #{startDate} and type = #{type}")
	long findTotalRequestFromDate(@Param("bucketId") Long bucketId, @Param("type") Audit.Type type, @Param("startDate") String startDate);
	
	@Select("select \n" +
			"\t#{by} as chart_by,\n" +
			"\t${by} as x,\n" +
			"\tIFNULL(sum(bytes),0) as y\n" +
			"from (\n" +
			"\tselect \n" +
			"\t\t*,\n" +
			"\t\tDATE_FORMAT(created_date,'%Y-%m-%d %H:00') as hour,\n" +
			"\t\tDATE_FORMAT(created_date,'%Y-%m-%d') as day,\n" +
			"\t\tDATE_FORMAT(created_date,'%Y-%m') as month,\n" +
			"\t\tDATE_FORMAT(created_date,'%Y') as year\n" +
			"\tfrom mos_audit where bytes > 0 and bucket_id = #{bucketId} and type = #{type} and created_date >= #{startDate} and created_date <= #{endDate} \n" +
			") a\n" +
			"group by ${by}")
	List<ChartVo> findChartFlowDataList(@Param("bucketId") Long bucketId,
										@Param("startDate") String startDate,
										@Param("endDate") String endDate,
										@Param("type") Audit.Type type,
										@Param("by") ChartBy by);
	@Select("select \n" +
			"\t#{by} as chart_by,\n" +
			"\t${by} as x,\n" +
			"\tcount(0) as y\n" +
			"from (\n" +
			"\tselect \n" +
			"\t\t*,\n" +
			"\t\tDATE_FORMAT(created_date,'%Y-%m-%d %H:00') as hour,\n" +
			"\t\tDATE_FORMAT(created_date,'%Y-%m-%d') as day,\n" +
			"\t\tDATE_FORMAT(created_date,'%Y-%m') as month,\n" +
			"\t\tDATE_FORMAT(created_date,'%Y') as year\n" +
			"\tfrom mos_audit where bucket_id = #{bucketId} and type = #{type} and created_date >= #{startDate} and created_date <= #{endDate} \n" +
			") a\n" +
			"group by ${by}")
	List<ChartVo> findChartRequestDataList(@Param("bucketId") Long bucketId,
										   @Param("startDate") String startDate,
										   @Param("endDate") String endDate,
										   @Param("type") Audit.Type type,
										   @Param("by") ChartBy by);
}
