package mt.spring.mos.server.service.fragment;

public interface FragmentIdFunction<T> {
	long getFragmentId(T task);
}