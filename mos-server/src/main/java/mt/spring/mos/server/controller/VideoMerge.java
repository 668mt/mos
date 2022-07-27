//package mt.spring.mos.server.controller;
//
//import org.bytedeco.ffmpeg.avcodec.AVPacket;
//import org.bytedeco.ffmpeg.global.avcodec;
//import org.bytedeco.ffmpeg.global.avutil;
//import org.bytedeco.javacpp.Loader;
//import org.bytedeco.javacv.*;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Set;
//
///**
// * @Author Martin
// * @Date 2022/7/20
// */
//public class VideoMerge {
//
//
//	/**
//	 * 多个视频的合并
//	 *
//	 * @param videoAddrSet 地址集合
//	 * @param output       合并后的视频输出地址
//	 */
//	public static void videoMerge(Set<String> videoAddrSet, String output)
//			throws org.bytedeco.javacv.FrameRecorder.Exception, org.bytedeco.javacv.FrameGrabber.Exception {
//		List<String> videoList = new ArrayList<>(videoAddrSet);
//
//		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoList.get(0));
//		grabber.start();
//
//		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, grabber.getImageWidth(),
//				grabber.getImageHeight(), 0);
//		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
////		recorder.setAudioChannels(1);
////		recorder.setInterleaved(true);
//		recorder.setFormat("mp4");
//		recorder.setFrameRate(grabber.getFrameRate());
//		recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // yuv420p
//		int bitrate = grabber.getVideoBitrate();
//		if (bitrate == 0) {
//			bitrate = grabber.getAudioBitrate();
//		}
//		recorder.setVideoBitrate(bitrate);
//
//		recorder.start();
//		Frame frame = null;
//		for (; (frame = grabber.grabImage()) != null; ) {
//			// 封装/复用
//			recorder.record(frame);
//		}
//
//		for (int i = 1; i < videoList.size(); i++) {
//			FFmpegFrameGrabber grabberTemp = new FFmpegFrameGrabber(videoList.get(i));
//			grabberTemp.start();
//			for (; (frame = grabberTemp.grabImage()) != null; ) {
//				// 封装/复用
//				recorder.record(frame);
//			}
//			grabberTemp.close();
//		}
//
//
//		recorder.close();
//		grabber.close();
//	}
//
//	public static void main(String[] args) throws Exception {
////		Set<String> list = new LinkedHashSet<>();
////		list.add("D:/test/1.mp4");
////		list.add("D:/test/3.mp4");
////		videoMerge(list, "D:/test/test.mp4");
//		new MutiVideoConcatToSingleVideo().concatFromFileListTxt("D:\\test\\filelist.txt", "D:/test/test.mp4");
////		merge("D:/test/1.mp4", "D:/test/3.mp4", "D:/test/test.mp4");
//	}
//
//
//	/**
//	 * 多个视频分片拼接合成单个视频
//	 *
//	 * @author eguid
//	 */
//	public static class MutiVideoConcatToSingleVideo {
//
//		/**
//		 * 拼接合成视频（转复用模式，transMuxer），这种方式只支持相同格式，相同编码,相同分辨率的文件格式
//		 *
//		 * @param fileListTxt 包含文件列表的txt文件，可以接受多个视频源地址
//		 *                    <p>txt内部格式：</p>
//		 *                    <p>file 'D:\下载\audio.mp4'</p>
//		 *                    <p>file 'D:\下载\audio2.mp4'</p>
//		 * @param output
//		 */
//		public void concatFromFileListTxt(String fileListTxt, String output) throws IOException, Exception {
//
//			FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(fileListTxt);
//			grabber.setFormat("concat");
//
//			//是否检查文件路径，支持的值：0-不检查，1-检查（只接受相对路径，仅支持包含字母，数字，句点，下划线和连字符的字符串，且在开头没有句点，才认为该文件路径是安全的）
//			grabber.setOption("safe", "0");
//
//			//自动转换码率，支持的值：1-自动转换，0-不转换，默认1
//			grabber.setOption("auto_convert", "1");
//
//			//每个包都设置开始时间和持续时间元数据，默认0
//			grabber.setOption("segment_time_metadata", "1");
//			grabber.start();
//
//			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, grabber.getImageWidth(),
//					grabber.getImageHeight(), grabber.getAudioChannels());
//			recorder.start(grabber.getFormatContext());
//
//			AVPacket packet = null;
//
//			//解封装/解复用
//			for (; (packet = grabber.grabPacket()) != null; ) {
//				//封装/复用
//				recorder.recordPacket(packet);
//			}
//			recorder.stop();
//			grabber.stop();
//		}
//	}
//
//	public static void merge(String file1, String file2, String dst) {
//		List<String> command = new ArrayList<>();
//		//获取JavaCV中的ffmpeg本地库的调用路径
//		String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
//		command.add(ffmpeg);
//		command.add("-re");
//		command.add("-i");
//		command.add(file1);
//		command.add("-i");
//		command.add(file2);
//		command.add("-filter_complex");
//		command.add("amix");
//		command.add("-map");
//		command.add("0:v");
//		command.add("-map");
//		command.add("0:a");
//		command.add("-map");
//		command.add("1:a");
//		//-shortest会取视频或音频两者短的一个为准，多余部分则去除不合并
//		command.add("-shortest");
////
//		//可以推到 流媒体服务器上。 例如srs
//		//command.add("rtmp://XX.XXX.XXX.XXX:1935/live/livestream");
//		//文件夹需要自己创建。
//		//也可以推到其他的文件夹(相当于录像)
//		command.add(dst);
//
//
//		long start = System.currentTimeMillis();
//		execute(command);
//		System.out.println("用时:" + (System.currentTimeMillis() - start));
//	}
//
//	/**
//	 * 操作系统进程
//	 *
//	 * @return
//	 */
//	public static void execute(List<String> command) {
//
//		try {
//			String join = String.join(" ", command);
//			System.out.println(join);
//			ProcessBuilder process = new ProcessBuilder(command);
//			process.inheritIO().start().waitFor();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//	}
//
//
//}
