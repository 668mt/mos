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
//
///**
// * @Author Martin
// * @Date 2021/2/25
// */
//@Data
////@Table(name = "mos_share_dir")
//@EqualsAndHashCode(callSuper = false)
//public class ShareDir extends BaseEntity {
//	@Id
//	@KeySql(useGeneratedKeys = true)
//	private Long id;
//
//	@ForeignKey(tableEntity = Share.class, casecadeType = ForeignKey.CascadeType.ALL)
//	private Long shareId;
//
//	@ForeignKey(tableEntity = Dir.class, casecadeType = ForeignKey.CascadeType.ALL)
//	private Long dirId;
//}
