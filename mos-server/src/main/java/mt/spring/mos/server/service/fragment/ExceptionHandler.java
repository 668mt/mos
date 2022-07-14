package mt.spring.mos.server.service.fragment;

public interface ExceptionHandler<T> {
	void handleException(T task, Exception e);
}