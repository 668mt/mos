package mt.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @Author Martin
 * @Date 2019/9/5
 */
@Slf4j
public class MtTimeoutExecutorTask<T> implements Runnable {
	public MtTimeoutExecutorTask(MtExecutor<T> mtExecutor) {
		this.mtExecutor = mtExecutor;
	}
	
	private final MtExecutor<T> mtExecutor;
	
	@Override
	public void run() {
		T task = mtExecutor.getOne();
		if (task == null)
			return;
		mtExecutor.getRunningJobs().add(task);
		String name = Thread.currentThread().getName();
		ExecutorService excutor = Executors.newSingleThreadExecutor((r) -> {
			Thread thread = new Thread(r);
			thread.setName(name);
			return thread;
		});
		Future<?> future = excutor.submit(() -> mtExecutor.doJob(task));
		try {
			mtExecutor.addTaskAmount();
			if (mtExecutor.getJobTimeout() > 0) {
				future.get(mtExecutor.getJobTimeout(), mtExecutor.getJobTimeoutTimeUnit());
			} else {
				future.get();
			}
		} catch (Exception e) {
			if (mtExecutor.getEvent() != null) {
				mtExecutor.getEvent().onError(mtExecutor, e, task);
			} else {
				log.error(e.getMessage(), e);
			}
			future.cancel(true);
		} finally {
			mtExecutor.getRunningJobs().remove(task);
			mtExecutor.downTaskAmount();
			excutor.shutdownNow(); // 强制终止任务
		}
	}

//	public static void main(String[] args) {
//		MtExecutor<String> mtExecutor = new MtExecutor<String>() {
//			@Override
//			public void doJob(String task) {
//				System.out.println(task);
//				try {
//					Thread.sleep(1000);
//				} catch (InterruptedException e) {
//				}
//			}
//		};
//		for(int i=0;i<100;i++){
//			mtExecutor.addQueue(""+i);
//		}
//		mtExecutor.setEvent(new MtExecutor.Event<String>() {
//			@Override
//			public void onTaskFinished(MtExecutor<String> mtExecutor) {
//				System.out.println("all done");
//				System.exit(1);
//			}
//
//			@Override
//			public void onError(MtExecutor<String> mtExecutor, Exception e, String task) {
//
//			}
//		});
//		mtExecutor.setJobTimeout(500);
//		mtExecutor.start();
//	}
}
