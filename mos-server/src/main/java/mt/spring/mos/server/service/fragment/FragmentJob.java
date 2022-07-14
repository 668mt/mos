package mt.spring.mos.server.service.fragment;

public interface FragmentJob<T> {
	void doJob(T task);
}