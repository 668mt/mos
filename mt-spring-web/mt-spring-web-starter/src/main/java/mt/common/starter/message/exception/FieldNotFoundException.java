package mt.common.starter.message.exception;

import javax.swing.plaf.metal.MetalMenuBarUI;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
public class FieldNotFoundException extends RuntimeException {
	
	public FieldNotFoundException() {
		super();
	}
	
	public FieldNotFoundException(String message) {
		super(message);
	}
}
