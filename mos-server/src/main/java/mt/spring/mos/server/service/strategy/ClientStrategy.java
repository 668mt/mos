package mt.spring.mos.server.service.strategy;

import mt.spring.mos.server.entity.po.Client;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/30
 */
public interface ClientStrategy {
	Client getClient(long freeSpace);
	
	Client getClient(long freeSpace, @Nullable List<Client> clients);
	
	String getName();
}
