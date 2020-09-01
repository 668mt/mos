package mt.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;


/**
 * 固定线程池操作
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Company: </p>
 *
 * @author Martin
 * @date 2018-1-23 下午9:59:01
 */
@Slf4j
@Data
public abstract class MtExecutor<T> {
	
	public interface Event<T> {
		default void onError(MtExecutor<T> mtExecutor, Exception e, T task) {
			log.error(e.getMessage(), e);
		}
		
		default void onTaskFinished(MtExecutor<T> mtExecutor) {
			log.info("队列已完成！");
		}
	}
	
	private Event<T> event;
	
	public enum State {
		running, stop
	}
	
	private State state;
	
	public MtExecutor() {
	}
	
	public MtExecutor(int maxLine) {
		this.maxLine = maxLine;
	}
	
	public MtExecutor(int maxLine, int maxAcceptQueueSize) {
		this.maxLine = maxLine;
		this.maxAcceptQueueSize = maxAcceptQueueSize;
	}
	
	public MtExecutor(Collection<T> tasks) {
		addQueues(tasks);
	}
	
	public MtExecutor(Collection<T> tasks, int maxLine) {
		addQueues(tasks);
		this.maxLine = maxLine;
	}
	
	/**
	 * 最大线程数
	 */
	private int maxLine = 5;
	private long jobTimeout = 0L;
	private TimeUnit jobTimeoutTimeUnit = TimeUnit.MILLISECONDS;
	private ExecutorService executor;
	
	private BlockingQueue<T> queue;
	private List<T> runningJobs = Collections.synchronizedList(new ArrayList<>());
	/**
	 * 间隔多久开启下一次任务
	 */
	private long delay = 200;
	/**
	 * 执行序号
	 */
	private int count = 0;
	private boolean running;
	private int maxAcceptQueueSize = Integer.MAX_VALUE;
	/**
	 * 运行中的任务数
	 */
	private int runningTaskAmount;
	/**
	 * 开始运行时间
	 */
	private long startTime;
	private String name;
	
	synchronized void addTaskAmount() {
		this.runningTaskAmount++;
	}
	
	synchronized void downTaskAmount() {
		this.runningTaskAmount--;
	}
	
	/**
	 * 执行任务
	 *
	 * @param task
	 */
	public abstract void doJob(T task);
	
	public boolean contains(T task) {
		return queue.contains(task) || runningJobs.contains(task);
	}
	
	/**
	 * 新增任务
	 *
	 * @param task
	 */
	public void addQueue(T task) {
		queue.add(task);
	}
	
	/**
	 * 新增任务
	 *
	 * @param tasks
	 */
	public void addQueues(Collection<T> tasks) {
		queue.addAll(tasks);
	}
	
	/**
	 * 开启任务
	 */
	public void start() {
		new Thread(this::startSync).start();
	}
	
	private void init() {
		queue = new LinkedBlockingQueue<>(maxAcceptQueueSize);
		if (executor == null) {
			if (name != null) {
				executor = new ThreadPoolExecutor(maxLine, maxLine, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(maxAcceptQueueSize), r -> new Thread(r, name));
			} else {
				executor = new ThreadPoolExecutor(maxLine, maxLine, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(maxAcceptQueueSize));
			}
		}
	}
	
	public void startSync() {
		init();
		if (state == State.running) {
			return;
		}
		this.running = true;
		startTime = System.currentTimeMillis();
		
		while (queue.iterator().hasNext()) {
			state = State.running;
			execute();
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		done();
	}
	
	public void stop() {
		this.running = false;
	}
	
	public void startAlways() {
		running = true;
		startTime = System.currentTimeMillis();
		init();
		new Thread(() -> {
			if (state == State.running) {
				return;
			}
			
			while (running) {
				state = State.running;
				Iterator<T> iterator = queue.iterator();
				if (iterator.hasNext()) {
					execute();
				}
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			done();
		}).start();
	}
	
	private void done() {
		int lastAmount = 0;
		while (runningTaskAmount != 0) {
			if (lastAmount != runningTaskAmount) {
				lastAmount = runningTaskAmount;
				log.info("队列中还有 {} 个任务!!", runningTaskAmount);
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		state = State.stop;
		executor.shutdownNow();
		executor = null;
		running = false;
		if (event != null) {
			event.onTaskFinished(this);
		} else {
			log.info("任务全部执行完毕！");
		}
	}
	
	/**
	 * 从队列中获取一个任务
	 *
	 * @return
	 */
	public synchronized T getOne() {
		Iterator<T> iterator = queue.iterator();
		if (iterator.hasNext()) {
			//平均执行一个耗时,单位ms
			double d = (System.currentTimeMillis() - startTime) * 1.0 / count;
			double rest = d * queue.size();
			String restStr;
			DecimalFormat df = new DecimalFormat("0.00");
			if (rest < 60000) {
				restStr = df.format(rest / 1000) + "秒";
			} else if (rest < 3600000) {
				restStr = df.format(rest / 1000 / 60) + "分钟";
			} else {
				restStr = df.format(rest / 1000 / 60 / 60) + "小时";
			}
			log.info("队列中任务数：{}，预计还需要{}执行完", queue.size(), restStr);
			T next = iterator.next();
			queue.remove(next);
			log.info("执行第 " + (++count) + " 个任务...");
			return next;
		}
		return null;
	}
	
	private void execute() {
		executor.execute(new MtTimeoutExecutorTask<>(this));
	}
}
