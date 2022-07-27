//package mt.spring.mos.server.controller;
//
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.IOUtils;
//import org.bytedeco.ffmpeg.avcodec.AVPacket;
//import org.bytedeco.ffmpeg.global.avcodec;
//import org.bytedeco.javacv.FFmpegFrameGrabber;
//import org.bytedeco.javacv.FFmpegFrameRecorder;
//import org.bytedeco.javacv.Frame;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.servlet.ServletOutputStream;
//import javax.servlet.http.HttpServletResponse;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//
///**
// * @Author Martin
// * @Date 2022/7/16
// */
//@RestController
//@RequestMapping("/test")
//@Slf4j
//public class TestController {
//
//	@GetMapping("/stream")
//	public void stream(HttpServletResponse response) throws Exception {
//		response.setContentType("video/mp4;charset=UTF-8");
////		response.setContentType("video/x-flv");
//		response.setHeader("Connection", "keep-alive");
//		response.setStatus(HttpServletResponse.SC_OK);
//		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("D:\\test\\1.mp4");
//		grabber.start();
//		System.out.println(grabber.getImageWidth());
//		System.out.println(grabber.getImageHeight());
//		// 来源视频H264格式,音频AAC格式
//		// 无须转码，更低的资源消耗，更低的延迟
//		ByteArrayOutputStream stream = new ByteArrayOutputStream();
//		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(stream, grabber.getImageWidth(), grabber.getImageHeight(),
//				0);
////		recorder.setInterleaved(true);
////		recorder.setVideoOption("preset", "ultrafast");
////		recorder.setVideoOption("tune", "zerolatency");
////		recorder.setVideoOption("crf", "25");
//		recorder.setFrameRate(grabber.getFrameRate());
////		recorder.setSampleRate(grabber.getSampleRate());
//		if (grabber.getAudioChannels() > 0) {
//			recorder.setAudioChannels(grabber.getAudioChannels());
//			recorder.setAudioBitrate(grabber.getAudioBitrate());
//			recorder.setAudioCodec(grabber.getAudioCodec());
//		}
////		recorder.setFormat("flv");
//		recorder.setFormat("mp4");
//		recorder.setVideoBitrate(grabber.getVideoBitrate());
//		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//		System.out.println(grabber.getVideoCodec());
////		recorder.setVideoCodec(grabber.getVideoCodec());
//		recorder.start(grabber.getFormatContext());
//		int nullNumber = 0;
//		ServletOutputStream outputStream = response.getOutputStream();
//		while (true) {
//			AVPacket k = grabber.grabPacket();
//			if (k != null) {
//				try {
//					recorder.recordPacket(k);
//				} catch (Exception e) {
//				}
//				if (stream.size() > 0) {
//					byte[] b = stream.toByteArray();
//					stream.reset();
//					outputStream.write(b);
//				}
//				avcodec.av_packet_unref(k);
//			} else {
//				nullNumber++;
//				if (nullNumber > 200) {
//					break;
//				}
//			}
//			Thread.sleep(5);
//		}
//
//
////		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
////		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(arrayOutputStream, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
////		recorder.setFormat("mp4");
////		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
////		recorder.setFrameRate(grabber.getFrameRate());
//////		recorder.start(grabber.getFormatContext());
////		recorder.start();
////		Frame frame = null;
////		while ((frame = grabber.grabFrame()) != null) {
////			recorder.record(frame);
////		}
//////		AVPacket avPacket = null;
//////		while ((avPacket = grabber.grabPacket()) != null) {
//////			recorder.recordPacket(avPacket);
//////		}
////		byte[] bytes = arrayOutputStream.toByteArray();
////		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
////		IOUtils.copyLarge(byteArrayInputStream, response.getOutputStream());
//		recorder.close();
//		grabber.close();
//		response.flushBuffer();
//	}
//
//	public static void main(String[] args) throws Exception {
//		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("D:\\test\\1.mp4");
//		grabber.start();
////		FFmpegFrameGrabber grabber2 = new FFmpegFrameGrabber("D:\\test\\3.mp4");
////		grabber2.start();
//		FFmpegFrameRecorder recorder = createRecorder(grabber, "D:\\test\\test.mp4");
////		recorder.setSampleRate(grabber.getSampleRate());
////		recorder.setSampleFormat(grabber.getSampleFormat());
////		recorder.start(grabber.getFormatContext());
//		recorder.setFormat("mp4");
//		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//		recorder.setFrameRate(grabber.getFrameRate());
//		recorder.start();
////		AVPacket grab;
//		Frame grab;
//		while ((grab = grabber.grabFrame()) != null) {
//			recorder.record(grab);
////			recorder.recordPacket(grab);
//		}
////		recorder.setFrameRate(grabber2.getFrameRate());
////		while ((grab = grabber2.grabFrame()) != null) {
////			recorder.record(grab);
////		}
////		while ((grab = grabber2.grabPacket()) != null) {
////			recorder.recordPacket(grab);
////		}
////		recorder.setAudioChannels(grabber2.getAudioChannels());
////		while ((grab = grabber2.grabFrame()) != null) {
////			recorder.record(grab);
////		}
//		recorder.stop();
//		grabber.stop();
////		grabber2.stop();
//	}
//
//	@SneakyThrows
//	private static FFmpegFrameRecorder createRecorder(FFmpegFrameGrabber grabber, String file) {
//		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(file, grabber.getImageWidth(), grabber.getImageHeight());
//		if (grabber.getAudioChannels() > 0) {
//			recorder.setAudioChannels(grabber.getAudioChannels());
////			recorder.setAudioBitrate(grabber.getAudioBitrate());
////			recorder.setAudioCodec(grabber.getAudioCodec());
//		}
////		recorder.setInterleaved(true);
////		recorder.setVideoOption("preset", "ultrafast");
////		recorder.setVideoOption("tune", "zerolatency");
////		recorder.setVideoOption("crf", "25");
////		recorder.setFormat("mp4");
//		return recorder;
//	}
//}
