package mt.spring.mos.server.listener;

import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import org.springframework.context.ApplicationEvent;

/**
 * @Author Martin
 * @Date 2020/10/23
 */
public class ClientWorkLogEvent extends ApplicationEvent {
	
	public ClientWorkLogEvent(Object source, ClientWorkLog.Action action, ClientWorkLog.ExeStatus exeStatus, Long clientId, Object... args) {
		super(source);
		Assert.notNull(action, "操作不能为空");
		Assert.notNull(exeStatus, "执行状态不能为空");
		Assert.notNull(clientId, "客户端id不能为空");
		this.action = action;
		this.exeStatus = exeStatus;
		this.clientId = clientId;
		this.args = args;
	}
	
	private ClientWorkLog.ExeStatus exeStatus;
	private Long clientId;
	private ClientWorkLog.Action action;
	private Object[] args;
	
	public ClientWorkLog.ExeStatus getExeStatus() {
		return exeStatus;
	}
	
	public void setExeStatus(ClientWorkLog.ExeStatus exeStatus) {
		this.exeStatus = exeStatus;
	}
	
	public Long getClientId() {
		return clientId;
	}
	
	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}
	
	public ClientWorkLog.Action getAction() {
		return action;
	}
	
	public void setAction(ClientWorkLog.Action action) {
		this.action = action;
	}
	
	public Object[] getArgs() {
		return args;
	}
	
	public void setArgs(Object[] args) {
		this.args = args;
	}
}
