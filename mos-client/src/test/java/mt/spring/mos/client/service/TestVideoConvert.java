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
//		Encoder encoder = new Encoder();
//		String[] supportedDecodingFormats = encoder.getSupportedDecodingFormats();
//		System.out.println(Arrays.toString(supportedDecodingFormats));
//		File source = new File("E:\\BaiduNetdiskDownload\\2021.1.21 李鹏辉 包晨 精剪.mp4");
//		File target = new File("E:\\BaiduNetdiskDownload\\test.m3u8");
//		FfmpegUtils.compressionVideo(source, target, "hls");
//		MultimediaObject object = new MultimediaObject(source);
		
		Encoder encoder = new Encoder();
		DefaultFFMPEGLocator defaultFFMPEGLocator = new DefaultFFMPEGLocator();
		FFMPEGExecutor executor = defaultFFMPEGLocator.createExecutor();
		String ffmpegExecutablePath = defaultFFMPEGLocator.getFFMPEGExecutablePath();
		System.out.println(ffmpegExecutablePath);
	}
}
