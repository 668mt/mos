package mt.spring.mos.server.config.log;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static mt.spring.mos.server.config.log.TraceContext.getOrCreate;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {
	@NotNull
	@Override
	protected ScheduledExecutorService createExecutor(int poolSize, @NotNull ThreadFactory threadFactory, @NotNull RejectedExecutionHandler rejectedExecutionHandler) {
		ScheduledExecutorService executor = super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
		return new TraceScheduledExecutorService(executor);
	}
	
	public static class TraceScheduledExecutorService implements ScheduledExecutorService {
		@Getter
		private final ScheduledExecutorService proxy;
		
		public TraceScheduledExecutorService(ScheduledExecutorService proxy) {
			if (proxy instanceof TraceScheduledExecutorService) {
				this.proxy = ((TraceScheduledExecutorService) proxy).getProxy();
			} else {
				this.proxy = proxy;
			}
		}
		
		@NotNull
		@Override
		public ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
			return proxy.schedule(new TraceRunnable(command, getOrCreate()), delay, unit);
		}
		
		@NotNull
		@Override
		public <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
			return proxy.schedule(new TraceCallable<>(callable, getOrCreate()), delay, unit);
		}
		
		@NotNull
		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
			return proxy.scheduleAtFixedRate(new TraceRunnable(command, getOrCreate()), initialDelay, period, unit);
		}
		
		@NotNull
		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long delay, @NotNull TimeUnit unit) {
			return proxy.scheduleWithFixedDelay(new TraceRunnable(command, getOrCreate()), initialDelay, delay, unit);
		}
		
		@Override
		public void shutdown() {
			proxy.shutdown();
		}
		
		@NotNull
		@Override
		public List<Runnable> shutdownNow() {
			return proxy.shutdownNow();
		}
		
		@Override
		public boolean isShutdown() {
			return proxy.isShutdown();
		}
		
		@Override
		public boolean isTerminated() {
			return proxy.isTerminated();
		}
		
		@Override
		public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
			return proxy.awaitTermination(timeout, unit);
		}
		
		@NotNull
		@Override
		public <T> Future<T> submit(@NotNull Callable<T> task) {
			return proxy.submit(new TraceCallable<>(task, getOrCreate()));
		}
		
		@NotNull
		@Override
		public <T> Future<T> submit(@NotNull Runnable task, T result) {
			return proxy.submit(new TraceRunnable(task, getOrCreate()), result);
		}
		
		@NotNull
		@Override
		public Future<?> submit(@NotNull Runnable task) {
			return proxy.submit(new TraceRunnable(task, getOrCreate()));
		}
		
		@NotNull
		@Override
		public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
			List<TraceCallable<T>> list = tasks.stream().map(t -> new TraceCallable<>(t, getOrCreate())).collect(Collectors.toList());
			return proxy.invokeAll(list);
		}
		
		@NotNull
		@Override
		public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
			List<TraceCallable<T>> list = tasks.stream().map(t -> new TraceCallable<>(t, getOrCreate())).collect(Collectors.toList());
			return proxy.invokeAll(list, timeout, unit);
		}
		
		@NotNull
		@Override
		public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
			List<TraceCallable<T>> list = tasks.stream().map(t -> new TraceCallable<>(t, getOrCreate())).collect(Collectors.toList());
			return proxy.invokeAny(list);
		}
		
		@Override
		public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			List<TraceCallable<T>> list = tasks.stream().map(t -> new TraceCallable<>(t, getOrCreate())).collect(Collectors.toList());
			return proxy.invokeAny(list, timeout, unit);
		}
		
		@Override
		public void execute(@NotNull Runnable command) {
			proxy.execute(new TraceRunnable(command, getOrCreate()));
		}
	}
}
