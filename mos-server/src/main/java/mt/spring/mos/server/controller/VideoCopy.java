//package mt.spring.mos.server.controller;
//
//import lombok.SneakyThrows;
//import org.bytedeco.ffmpeg.avcodec.AVPacket;
//import org.bytedeco.ffmpeg.global.avcodec;
//import org.bytedeco.javacv.FFmpegFrameGrabber;
//import org.bytedeco.javacv.FFmpegFrameRecorder;
//import org.bytedeco.javacv.FFmpegLogCallback;
//
//import java.io.FileOutputStream;
//import java.io.OutputStream;
//
///**
// * @Author Martin
// * @Date 2022/7/24
// */
//public class VideoCopy {
//	@SneakyThrows
//	public static void main(String[] args) {
//		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("D:/test/1.mp4");
//		grabber.setOption("safe", "0");
//		//自动转换码率，支持的值：1-自动转换，0-不转换，默认1
//		grabber.setOption("auto_convert", "1");
//		grabber.start();
////		OutputStream outputStream = new ByteArrayOutputStream();
//		String file = "D:/test/test2.flv";
//		OutputStream outputStream = new FileOutputStream(file);
//		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputStream, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
//		recorder.setVideoCodec(grabber.getVideoCodec());
////		recorder.setAudioCodec(grabber.getAudioCodec());
//		recorder.setAudioChannels(0);
//		recorder.setFormat("flv");
//		recorder.start(grabber.getFormatContext());
//		AVPacket avPacket = null;
//		while ((avPacket = grabber.grabPacket()) != null) {
//			recorder.recordPacket(avPacket);
//		}
//		recorder.close();
//		grabber.close();
////		byte[] bytes = outputStream.toByteArray();
////		IOUtils.copyLarge(new ByteArrayInputStream(bytes), new FileOutputStream(file));
//	}
//}
