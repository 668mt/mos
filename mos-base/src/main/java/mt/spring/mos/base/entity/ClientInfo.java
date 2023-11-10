package mt.spring.mos.base.entity;

import lombok.Data;

import java.util.List;

@Data
public class ClientInfo {
	private Boolean isEnableAutoImport;
	private SpaceInfo spaceInfo;
	private Boolean isHealth;
	private List<String> basePaths;
	private List<String> serverHosts;
}
	
