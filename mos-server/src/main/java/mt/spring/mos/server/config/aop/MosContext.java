package mt.spring.mos.server.config.aop;

import lombok.Data;
import mt.spring.mos.sdk.type.EncryptContent;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Data
public class MosContext {
	private Long bucketId;
	private String bucketName;
	private Long currentUserId;
	private Long openId;
	private Long expireSeconds;
	private EncryptContent content;
	
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
