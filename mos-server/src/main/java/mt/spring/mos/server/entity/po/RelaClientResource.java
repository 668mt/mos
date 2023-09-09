//package mt.spring.mos.server.entity.po;
//
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import mt.common.annotation.ForeignKey;
//import mt.spring.mos.server.entity.BaseEntity;
//
//import javax.persistence.Id;
//import javax.persistence.Table;
//
///**
// * @Author Martin
// * @Date 2020/5/18
// */
//@Table(name = "mos_rela_client_resource")
//@Data
//@EqualsAndHashCode(callSuper = false)
//public class RelaClientResource extends BaseEntity {
//	private static final long serialVersionUID = 7473638179073719443L;
//
//	@Id
//	@ForeignKey(tableEntity = Resource.class, casecadeType = ForeignKey.CascadeType.ALL)
//	private Long resourceId;
//
//	@Id
//	@ForeignKey(tableEntity = Client.class, casecadeType = ForeignKey.CascadeType.ALL)
//	private Long clientId;
//}
