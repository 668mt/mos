package mt.common.utils;

/**
 * @Author Martin
 * @Date 2020/6/29
 */
public class BeanUtilsException extends RuntimeException {
	private static final long serialVersionUID = -4295571853320969320L;
	
	public BeanUtilsException(Exception e) {
		super(e);
	}
}
