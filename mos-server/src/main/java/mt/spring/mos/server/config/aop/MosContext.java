package mt.spring.mos.server.config.aop;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Data
public class MosContext {
	private String pathname;
	private Long bucketId;
	private String bucketName;
	private Long currentUserId;
	private Long openId;
	private Long expireSeconds;
	
	private static ThreadLocal<MosContext> context = new ThreadLocal<>();
	
	public static void setContext(MosContext mosContext) {
		context.set(mosContext);
	}
	
	public static MosContext getContext() {
		return context.get();
	}
	
	public static void clear() {
		context.remove();
	}
}
