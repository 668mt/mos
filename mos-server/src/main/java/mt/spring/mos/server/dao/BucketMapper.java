package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.vo.BucketVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@Repository
public interface BucketMapper extends BaseMapper<Bucket> {
	@Select("select distinct b.* from mos_bucket b,mos_bucket_grant g \n" +
			"where b.id = g.bucket_id \n" +
			"and g.user_id = #{userId} \n" +
			"and b.bucket_name = #{bucketName}")
	Bucket findGrantBucketByUserIdAndBucketName(@Param("userId") Long userId, @Param("bucketName") String bucketName);
	
	@Select("select distinct b.* from mos_bucket b,mos_bucket_grant g \n" +
			"where b.id = g.bucket_id \n" +
			"and g.user_id = #{userId} \n" +
			"and b.id = #{bucketId}")
	Bucket findGrantBucketByUserIdAndBucketId(@Param("userId") Long userId, @Param("bucketId") Long bucketId);
	
	@Select("select * from (select 1 as is_own\n" +
			"\t\t\t,id\n" +
			"\t\t\t,bucket_name\n" +
			"\t\t\t,user_id\n" +
			"\t\t\t,created_date\n" +
			"\t\t\t,created_by\n" +
			"\t\t\t,updated_date\n" +
			"\t\t\t,updated_by \n" +
			"from mos_bucket where user_id = #{userId}\n" +
			"union all \n" +
			"select distinct 0 as is_own\n" +
			"\t\t\t,b.id\n" +
			"\t\t\t,b.bucket_name\n" +
			"\t\t\t,b.user_id\n" +
			"\t\t\t,b.created_date\n" +
			"\t\t\t,b.created_by\n" +
			"\t\t\t,b.updated_date\n" +
			"\t\t\t,b.updated_by \n" +
			"from mos_bucket b,mos_bucket_grant g  \n" +
			"where b.id = g.bucket_id  and g.user_id = #{userId} ) a")
	List<BucketVo> findBucketList(@Param("userId") Long userId);
}
