package mt.spring.mos.sdk.upload;

import mt.spring.mos.sdk.interfaces.RecordFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @Author Martin
 * @Date 2020/12/4
 */
public class PropertiesRecordFile implements RecordFile {
	private final Properties record;
	private final File recordFile;
	private final String recordPath;
	
	public PropertiesRecordFile(String pathname, String lastModified, int chunks) {
		recordPath = FileUtils.getTempDirectoryPath() + "/mos/download/recordFiles-" + DigestUtils.md5Hex(pathname);
		String recordPathName = recordPath + "/" + chunks + "-" + DigestUtils.md5Hex(lastModified);
		recordFile = new File(recordPathName);
		File parentFile = recordFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		record = new Properties();
		if (recordFile.exists()) {
			try (FileInputStream inputStream = new FileInputStream(recordFile)) {
				record.load(inputStream);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public void finish(int chunkIndex) {
		record.setProperty("chunk" + chunkIndex, "true");
		store();
	}
	
	@Override
	public boolean hasDownload(int chunkIndex) {
		return "true".equalsIgnoreCase(record.getProperty("chunk" + chunkIndex));
	}
	
	@Override
	public synchronized void store() {
		try (FileOutputStream outputStream = new FileOutputStream(recordFile)) {
			record.store(outputStream, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void clear() {
		File path = new File(recordPath);
		if (path.exists()) {
			try {
				FileUtils.deleteDirectory(path);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
