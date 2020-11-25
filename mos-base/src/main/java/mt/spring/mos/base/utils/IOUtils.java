package mt.spring.mos.base.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.stream.BoundedInputStream;
import mt.spring.mos.base.stream.ByteBufferInputStream;
import mt.spring.mos.base.stream.RepeatableBoundedFileInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import sun.misc.Cleaner;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static mt.spring.mos.base.utils.ReflectUtils.getValue;

/**
 * @Author Martin
 * @Date 2020/11/21
 */
@Slf4j
public class IOUtils {
	public static final int MB = 1024 * 1024;
	
	@Data
	public static class UploadPart {
		private InputStream inputStream;
		private long length;
	}
	
	@Data
	public static class SplitResult {
		private int chunks;
		private long partSize;
		private long totalSize;
		private String totalMd5;
		private List<UploadPart> uploadParts;
	}
	
	/**
	 * 切分文件
	 *
	 * @param file         文件
	 * @param minPartSize  最小的分片大小，单位字节
	 * @param maxPartSize  最大的分片大小，单位字节
	 * @param expectChunks 期望的分片数
	 * @return 分片结果
	 * @throws IOException IO异常
	 */
	public static SplitResult splitFile(File file, long minPartSize, long maxPartSize, int expectChunks) throws IOException {
		SplitResult splitResult = new SplitResult();
		long totalSize = file.length();
		splitResult.setTotalSize(totalSize);
		long partSize = BigDecimal.valueOf(totalSize).divide(BigDecimal.valueOf(expectChunks), 0, RoundingMode.UP).intValue();
		if (partSize < minPartSize) {
			partSize = MB;
		} else if (partSize > maxPartSize) {
			partSize = maxPartSize;
		}
		
		long length = file.length();
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			String totalMd5 = DigestUtils.md5Hex(fileInputStream);
			splitResult.setTotalMd5(totalMd5);
		}
		int chunks = (int) (length % partSize == 0 ? length / partSize : length / partSize + 1);
		List<UploadPart> uploadParts = new ArrayList<>();
		for (int i = 0; i < chunks; i++) {
			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.skip(partSize * i);
			InputStream inputStream = newRepeatableInputStream(new BoundedInputStream(fileInputStream, partSize));
			UploadPart uploadPart = new UploadPart();
			uploadPart.setInputStream(inputStream);
			if (i == chunks - 1) {
				uploadPart.setLength(file.length() - partSize * i);
			} else {
				uploadPart.setLength(partSize);
			}
			uploadParts.add(uploadPart);
		}
		splitResult.setChunks(chunks);
		splitResult.setPartSize(partSize);
		splitResult.setUploadParts(uploadParts);
		return splitResult;
	}
	
	public static InputStream newRepeatableInputStream(final BoundedInputStream original) throws IOException {
		InputStream repeatable;
		if (!original.markSupported()) {
			if (original.getWrappedInputStream() instanceof FileInputStream) {
				repeatable = new RepeatableBoundedFileInputStream(original);
			} else {
				repeatable = new BufferedInputStream(original, 512 * 1024);
			}
		} else {
			repeatable = original;
		}
		return repeatable;
	}
	
	public interface ConvertCallback {
		void onConvertedChunk(ByteBufferInputStream byteBufferInputStream, int index);
	}
	
	public static int convertStreamToByteBufferStream(InputStream inputStream, ConvertCallback convertCallback) throws IOException {
		try {
			int read;
			int i = 0;
			byte[] buffer = new byte[4096];
			int partSize = 2 * MB;
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(partSize);
			
			Cleaner cleaner;
			try {
				cleaner = (Cleaner) getValue(byteBuffer, "cleaner");
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			}
			while ((read = inputStream.read(buffer, 0, Math.min(byteBuffer.remaining(), buffer.length))) != -1) {
				byteBuffer.put(buffer, 0, read);
				if (!byteBuffer.hasRemaining()) {
					try (ByteBufferInputStream byteBufferInputStream = new ByteBufferInputStream(byteBuffer)) {
						convertCallback.onConvertedChunk(byteBufferInputStream, i);
					}
					i++;
				}
			}
			try (ByteBufferInputStream byteBufferInputStream = new ByteBufferInputStream(byteBuffer)) {
				convertCallback.onConvertedChunk(byteBufferInputStream, i);
			}
			cleaner.clean();
			return i + 1;
		} finally {
			inputStream.close();
		}
	}
}
