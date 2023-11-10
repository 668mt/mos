//package mt.spring.mos.base.utils;
//
//import org.jetbrains.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import ws.schild.jave.*;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.concurrent.TimeUnit;
//import java.util.regex.Pattern;
//
///**
// * @Author Martin
// * @Date 2021/2/3
// */
//public class HlsEncoder {
//	private final FFMPEGLocator locator;
//
//	private static final Logger LOG = LoggerFactory.getLogger(HlsEncoder.class);
//	private static final Pattern SUCCESS_PATTERN = Pattern.compile("^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+subtitle\\:\\S+\\s+global headers\\:\\S+.*$", Pattern.CASE_INSENSITIVE);
//
//	public HlsEncoder() {
//		this.locator = new DefaultFFMPEGLocator();
//	}
//
//	public interface FfmpegWorker {
//		void addArguments(FFMPEGExecutor ffmpeg);
//	}
//
//	public void convertToHlsBySize(File source, File target, int segmentMB, @Nullable Integer minSegmentSeconds) {
//		if (minSegmentSeconds == null) {
//			minSegmentSeconds = 15;
//		}
//		//每个分片按10MB计算，但时长不能小于5s
//		MultimediaObject object = new MultimediaObject(source);
//		try {
//			long duration = object.getInfo().getDuration();
//			long length = source.length();
//			double sizeMb = Math.ceil(length * 1.0 / IOUtils.MB);
//			double perSecondMb = sizeMb / TimeUnit.MILLISECONDS.toSeconds(duration);
//			int segmentSeconds = (int) Math.ceil(segmentMB * 1.0 / perSecondMb);
//			if (segmentSeconds < minSegmentSeconds) {
//				segmentSeconds = minSegmentSeconds;
//			}
//			convertToHlsBySeconds(source, target, segmentSeconds);
//		} catch (EncoderException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	public void convertToHlsBySeconds(File source, File target, int segmentSeconds) {
//		File tsFile = new File(target.getParentFile(), target.getName() + ".ts");
//		convertTs(source, tsFile);
//		splitTs(tsFile, target, segmentSeconds);
//		tsFile.delete();
//	}
//
//	public void convertTs(File source, File target) {
//		LOG.info("转换为ts文件：{}", source);
//		File parentFile = target.getParentFile();
//		if (!parentFile.exists()) {
//			parentFile.mkdirs();
//		}
//		doFfmpegJob(ffmpeg -> {
////			ffmpeg -y -i "IMG_8308.MOV"  -vcodec copy -acodec copy -vbsf h264_mp4toannexb test.ts
//			ffmpeg.addArgument("-y");
//			ffmpeg.addArgument("-i");
//			ffmpeg.addArgument(source.getAbsolutePath());
//			ffmpeg.addArgument("-vcodec");
//			ffmpeg.addArgument("copy");
//			ffmpeg.addArgument("-acodec");
//			ffmpeg.addArgument("copy");
//			ffmpeg.addArgument("-vbsf");
//			ffmpeg.addArgument("h264_mp4toannexb");
//			ffmpeg.addArgument(target.getAbsolutePath());
//		});
//	}
//
//	public void splitTs(File source, File target, @Nullable Integer segmentSeconds) {
//		LOG.info("分割ts文件：{}", source);
//		target.getParentFile().mkdirs();
//		if (segmentSeconds == null) {
//			segmentSeconds = 30;
//		}
//		Integer finalSegmentSeconds = segmentSeconds;
//		doFfmpegJob(ffmpeg -> {
//			//ffmpeg -i test.ts -c copy -map 0 -f segment -segment_list test.m3u8 -segment_time 60 "60s_%3d.ts"
//			ffmpeg.addArgument("-i");
//			ffmpeg.addArgument(source.getAbsolutePath());
//			ffmpeg.addArgument("-c");
//			ffmpeg.addArgument("copy");
//			ffmpeg.addArgument("-map");
//			ffmpeg.addArgument("0");
//			ffmpeg.addArgument("-f");
//			ffmpeg.addArgument("segment");
//			ffmpeg.addArgument("-segment_list");
//			ffmpeg.addArgument(target.getAbsolutePath());
//			ffmpeg.addArgument("-segment_time");
//			ffmpeg.addArgument(finalSegmentSeconds + "");
//			ffmpeg.addArgument(new File(target.getParentFile(), "segment_%3d.ts").getAbsolutePath());
//		});
//	}
//
//	public void doFfmpegJob(FfmpegWorker ffmpegWorker) {
//		FFMPEGExecutor ffmpeg = locator.createExecutor();
//		ffmpegWorker.addArguments(ffmpeg);
//		try {
//			ffmpeg.execute();
//			try (RBufferedReader reader = new RBufferedReader(new InputStreamReader(ffmpeg.getErrorStream()))) {
//				String line;
//				ConversionOutputAnalyzer outputAnalyzer = new ConversionOutputAnalyzer(0, null);
//				while ((line = reader.readLine()) != null) {
//					outputAnalyzer.analyzeNewLine(line);
//				}
//				if (outputAnalyzer.getLastWarning() != null) {
//					String lastWarning = outputAnalyzer.getLastWarning();
//					if (!SUCCESS_PATTERN.matcher(lastWarning).matches()) {
//						throw new RuntimeException("No match for: " + SUCCESS_PATTERN + " in " + lastWarning);
//					}
//				}
//			}
//			int exitCode = ffmpeg.getProcessExitCode();
//			if (exitCode != 0) {
//				LOG.error("Process exit code: {}", exitCode);
//				throw new RuntimeException("Exit code of ffmpeg encoding run is " + exitCode);
//			}
//		} catch (IOException | ws.schild.jave.EncoderException e) {
//			throw new RuntimeException(e);
//		} finally {
//			ffmpeg.destroy();
//		}
//	}
//}
