package mt.spring.mos.base.entity;

import lombok.Data;

@Data
public class ClientInfo {
	private Boolean isEnableAutoImport;
	private SpaceInfo spaceInfo;
	private Boolean isHealth;
}
	
