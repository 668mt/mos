package mt.spring.mos.sdk.exception;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
public class UploadException extends RuntimeException {
	public UploadException(String message) {
		super(message);
	}
	
	public UploadException(Throwable e) {
		super(e);
	}
}
