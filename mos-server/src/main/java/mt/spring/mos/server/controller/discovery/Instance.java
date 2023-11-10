package mt.spring.mos.server.controller.discovery;

import lombok.Data;
import mt.spring.mos.server.entity.po.Client;

@Data
public class Instance {
	private String name;
	private String ip;
	private Integer port;
	private Integer weight;
	private String remark;
	private String registPwd;
	private Integer minAvaliableSpaceGB;
	private Client.ClientStatus status;
}