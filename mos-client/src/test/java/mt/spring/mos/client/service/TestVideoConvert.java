package mt.spring.mos.client.service;

import org.junit.Test;
import ws.schild.jave.DefaultFFMPEGLocator;
import ws.schild.jave.Encoder;
import ws.schild.jave.FFMPEGExecutor;

/**
 * @Author Martin
 * @Date 2021/1/31
 */
public class TestVideoConvert {
	@Test
	public void testConvert() throws Exception {
		
		Encoder encoder = new Encoder();
		DefaultFFMPEGLocator defaultFFMPEGLocator = new DefaultFFMPEGLocator();
		FFMPEGExecutor executor = defaultFFMPEGLocator.createExecutor();
		String ffmpegExecutablePath = defaultFFMPEGLocator.getFFMPEGExecutablePath();
		System.out.println(ffmpegExecutablePath);
	}
}
