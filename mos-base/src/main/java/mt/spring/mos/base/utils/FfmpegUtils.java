package mt.spring.mos.base.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ws.schild.jave.*;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/12/10
 */
@Slf4j
public class FfmpegUtils {
	@Data
	@Slf4j
	public static class VideoParsedInfo {
		private long length;
		private long during;
		private VideoInfo videoInfo;
		
		public long getPerSecondsBytes() {
			return BigDecimal.valueOf(length).divide(BigDecimal.valueOf(TimeUnit.MILLISECONDS.toSeconds(during)), 0, RoundingMode.HALF_UP).longValue();
		}
		
		@Override
		public String toString() {
			return "大小：" + SizeUtils.getReadableSize(length) + "\n" +
					"长度：" + TimeUtils.getReadableTime(during) + "\n" +
					"平均每秒大小：" + SizeUtils.getReadableSize(getPerSecondsBytes()) + "\n" +
					"宽度：" + videoInfo.getSize().getWidth() + "\n" +
					"高度：" + videoInfo.getSize().getHeight() + "\n" +
					"FPS：" + videoInfo.getFrameRate() + "\n"
					;
		}
	}
	
	public static VideoParsedInfo getVideoInfo(File source) throws EncoderException {
		MultimediaObject object = new MultimediaObject(source);
		MultimediaInfo info = object.getInfo();
		VideoInfo video = info.getVideo();
		Assert.notNull(video, "video parsed error");
		VideoParsedInfo videoParsedInfo = new VideoParsedInfo();
		videoParsedInfo.setLength(source.length());
		videoParsedInfo.setDuring(info.getDuration());
		videoParsedInfo.setVideoInfo(video);
		return videoParsedInfo;
	}

//	public static void compressionVideo(File source, File target) throws Exception {
//		compressionVideo(source, target, "mp4");
//	}
	
//	public static void compressionVideo(File source, File target, String format) throws Exception {
//		MultimediaObject object = new MultimediaObject(source);
//		AudioInfo audioInfo = object.getInfo().getAudio();
//		// 根据视频大小来判断是否需要进行压缩,
//		int maxSize = 100;
//		double mb = Math.ceil(source.length() * 1.0 / IOUtils.MB);
//		int second = (int) object.getInfo().getDuration() / 1000;
//		BigDecimal bd = new BigDecimal(String.format("%.4f", mb / second));
//		log.info("开始压缩视频了--> 视频每秒平均 " + bd + " MB ");
//		// 视频 > 100MB, 或者每秒 > 0.5 MB 才做压缩， 不需要的话可以把判断去掉
//		boolean needCompress = mb > maxSize || bd.compareTo(new BigDecimal("0.5")) > 0;
//		if (!needCompress) {
//			return;
//		}
//		long time = System.currentTimeMillis();
//		int maxAudioBitRate = 128000;
//		int maxSamplingRate = 44100;
//		int maxVideoBitRate = 800000;
//		int maxFrameRate = 20;
//		int maxWidth = 1280;
//
//		AudioAttributes audio = new AudioAttributes();
//		// 设置通用编码格式
//		audio.setCodec("aac");
//		// 设置最大值：比特率越高，清晰度/音质越好
//		// 设置音频比特率,单位:b (比特率越高，清晰度/音质越好，当然文件也就越大 128000 = 182kb)
//		if (audioInfo.getBitRate() > maxAudioBitRate) {
//			audio.setBitRate(maxAudioBitRate);
//		}
//
//		// 设置重新编码的音频流中使用的声道数（1 =单声道，2 = 双声道（立体声））。如果未设置任何声道值，则编码器将选择默认值 0。
//		audio.setChannels(audioInfo.getChannels());
//		// 采样率越高声音的还原度越好，文件越大
//		// 设置音频采样率，单位：赫兹 hz
//		// 设置编码时候的音量值，未设置为0,如果256，则音量值不会改变
//		// audio.setVolume(256);
//		if (audioInfo.getSamplingRate() > maxSamplingRate) {
//			audio.setSamplingRate(maxSamplingRate);
//		}
//
//		VideoInfo videoInfo = object.getInfo().getVideo();
//		VideoAttributes videoAttributes = new VideoAttributes();
//		videoAttributes.setCodec("h264");
//		//设置音频比特率,单位:b (比特率越高，清晰度/音质越好，当然文件也就越大 800000 = 800kb)
//		if (videoInfo.getBitRate() > maxVideoBitRate) {
//			videoAttributes.setBitRate(maxVideoBitRate);
//		}
//
//		// 视频帧率：15 f / s  帧率越低，效果越差
//		// 设置视频帧率（帧率越低，视频会出现断层，越高让人感觉越连续），视频帧率（Frame rate）是用于测量显示帧数的量度。所谓的测量单位为每秒显示帧数(Frames per Second，简：FPS）或“赫兹”（Hz）。
//		if (videoInfo.getFrameRate() > maxFrameRate) {
//			videoAttributes.setFrameRate(maxFrameRate);
//		}
//
//		// 限制视频宽高
//		int width = videoInfo.getSize().getWidth();
//		int height = videoInfo.getSize().getHeight();
//		if (width > maxWidth) {
//			float rat = (float) width / maxWidth;
//			videoAttributes.setSize(new VideoSize(maxWidth, (int) (height / rat)));
//		}
//
//		EncodingAttributes attr = new EncodingAttributes();
//		if (StringUtils.isBlank(format)) {
//			format = "mp4";
//		}
//		attr.setFormat(format);
//		attr.setAudioAttributes(audio);
//		attr.setVideoAttributes(videoAttributes);
//
//		// 速度最快的压缩方式， 压缩速度 从快到慢： ultrafast, superfast, veryfast, faster, fast, medium,  slow, slower, veryslow and placebo.
////                attr.setPreset(PresetUtil.VERYFAST);
////                attr.setCrf(27);
////                // 设置线程数
//		attr.setEncodingThreads(Runtime.getRuntime().availableProcessors() / 2);
//		Encoder encoder = new Encoder();
//		encoder.encode(new MultimediaObject(source), target, attr);
//		log.info("压缩总耗时：{}秒", (System.currentTimeMillis() - time) / 1000);
//	}
	
	public static void screenShot(File srcFile, File desFile, int width, int seconds) throws Exception {
		MultimediaObject object = new MultimediaObject(srcFile);
		double maxSeconds = 0;
		if (seconds > 0) {
			long duration = object.getInfo().getDuration();
			maxSeconds = Math.floor(duration * 1.0 / 1000) - 1;
			if (maxSeconds < 0) {
				maxSeconds = 0;
			}
		}
		ScreenExtractor screenExtractor = new ScreenExtractor();
		VideoSize size = object.getInfo().getVideo().getSize();
		int height = (int) Math.ceil(width * size.getHeight() * 1.0 / size.getWidth());
		screenExtractor.render(object, width, height, (int) Math.min(maxSeconds, seconds), desFile, 1);
	}
	
	public static void compressImage(File srcFile, File desFile, int width) throws Exception {
		screenShot(srcFile, desFile, width, 0);
	}
}
