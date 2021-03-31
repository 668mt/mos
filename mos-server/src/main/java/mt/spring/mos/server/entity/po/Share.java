//package mt.spring.mos.server.entity.po;
//
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import mt.common.annotation.ForeignKey;
//import mt.spring.mos.server.entity.BaseEntity;
//import tk.mybatis.mapper.annotation.KeySql;
//
//import javax.persistence.Id;
//import javax.persistence.Table;
//import java.util.Date;
//
///**
// * @Author Martin
// * @Date 2021/2/25
// */
//@Data
////@Table(name = "mos_share")
//@EqualsAndHashCode(callSuper = false)
//public class Share extends BaseEntity {
//	@Id
//	@KeySql(useGeneratedKeys = true)
//	private Long id;
//
//	private String name;
//
//	@ForeignKey(tableEntity = Bucket.class, casecadeType = ForeignKey.CascadeType.ALL)
//	private Long bucketId;
//
//	private Date expiredDate;
//}
