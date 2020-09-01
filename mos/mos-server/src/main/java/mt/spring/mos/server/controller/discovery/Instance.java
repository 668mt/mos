package mt.spring.mos.server.controller.discovery;

import lombok.Data;

@Data
public class Instance {
	private String clientId;
	private String ip;
	private Integer port;
	private Integer weight;
	private String remark;
	private String registPwd;
}