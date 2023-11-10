package mt.spring.mos.server.utils;

import lombok.Data;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.util.concurrent.TimeUnit;

/**
 * 系统监控
 */
public class SystemMonitor {
	private static SystemInfo systemInfo = new SystemInfo();
	
	/**
	 * 是否有指定百分比的 CPU 空闲
	 *
	 * @param percent 0.5 表示 50%
	 * @return true 表示有
	 * @throws InterruptedException 中断异常
	 */
	public static boolean hasCpuFreePercent(double percent) throws InterruptedException {
		CpuInfo cpuInfo = getCpuInfo();
		return cpuInfo.idlePercent >= percent;
	}
	
	/**
	 * 打印 CPU 信息
	 */
	public static CpuInfo getCpuInfo() throws InterruptedException {
		CentralProcessor processor = systemInfo.getHardware().getProcessor();
		long[] prevTicks = processor.getSystemCpuLoadTicks();
		// 睡眠1s
		TimeUnit.SECONDS.sleep(1);
		long[] ticks = processor.getSystemCpuLoadTicks();
		long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
			- prevTicks[CentralProcessor.TickType.NICE.getIndex()];
		long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
			- prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
		long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
			- prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
		long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
			- prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
		long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
			- prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
		long user = ticks[CentralProcessor.TickType.USER.getIndex()]
			- prevTicks[CentralProcessor.TickType.USER.getIndex()];
		long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
			- prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
		long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
			- prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
		long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
		CpuInfo cpuInfo = new CpuInfo();
		cpuInfo.core = processor.getLogicalProcessorCount();
		cpuInfo.systemUsePercent = cSys * 1.0 / totalCpu;
		cpuInfo.userUsePercent = user * 1.0 / totalCpu;
		cpuInfo.waitPercent = iowait * 1.0 / totalCpu;
		cpuInfo.idlePercent = idle * 1.0 / totalCpu;
		cpuInfo.loadBetweenTicksPercent = processor.getSystemCpuLoadBetweenTicks();
		cpuInfo.loadPercent = processor.getSystemCpuLoad();
		return cpuInfo;
	}
	
	@Data
	public static class CpuInfo {
		/**
		 * cpu核数
		 */
		private int core;
		private double systemUsePercent;
		private double userUsePercent;
		private double waitPercent;
		private double idlePercent;
		private double loadBetweenTicksPercent;
		private double loadPercent;
		
	}
}