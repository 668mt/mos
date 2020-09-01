package mt.common.mybatis.exception;

public abstract class MyBatisException extends RuntimeException {

	private static final long serialVersionUID = -1935877005909023796L;

	public MyBatisException(){
		
	}
	
	public MyBatisException(String message){
		super(message);
	}
}
