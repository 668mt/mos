package mt.spring.mos.sdk.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamePrefixThreadFactory implements ThreadFactory {
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	
	public NamePrefixThreadFactory(String namePrefix) {
		this.namePrefix = namePrefix + "-thread-";
	}
	
	public Thread newThread(@NotNull Runnable r) {
		Thread t = new Thread(r);
		t.setName(namePrefix + threadNumber.getAndIncrement());
		return t;
	}
}