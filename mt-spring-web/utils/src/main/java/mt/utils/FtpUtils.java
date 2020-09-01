package mt.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.SocketException;

/**
 * @Author Martin
 * @Date 2018/6/11
 */
@Slf4j
@Data
public class FtpUtils {
	
	private String host;
	private int port;
	private String username;
	private String password;
	private static String requestUrl = "";
	
	public FtpUtils() {
	}
	
	public FtpUtils(String host, int port, String username, String password, String requestUrl) {
		this.host = host;
		this.port = port;
		this.password = password;
		this.username = username;
		FtpUtils.this.requestUrl = requestUrl;
	}
	
	public static void setRequestUrl(String requestUrl) {
		FtpUtils.requestUrl = requestUrl;
	}
	
	public static String getRequestUrl() {
		return FtpUtils.requestUrl;
	}
	
	public void download(String pathname, OutputStream outputStream) {
		download(pathname, outputStream, host, port, username, password);
	}
	
	private static String dealPathname(String pathname) {
		Assert.notNull(pathname);
		if (pathname.indexOf("/") == -1) {
			pathname = "/" + pathname;
		}
		return pathname;
	}
	
	public static void download(String pathname, OutputStream outputStream, String host, int port, String username, String password) {
		pathname = dealPathname(pathname);
		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(host, port);
			ftpClient.login(username, password);
			ftpClient.setFileTransferMode(10);
			ftpClient.setFileType(2);
			ftpClient.enterLocalPassiveMode();
			Assert.state(FTPReply.isPositiveCompletion(ftpClient.getReplyCode()));
			String directory = StringUtils.substringBeforeLast(pathname, "/");
			String filename = StringUtils.substringAfterLast(pathname, "/");
			Assert.state(ftpClient.changeWorkingDirectory(directory));
			InputStream inputStream = ftpClient.retrieveFileStream(filename);
			Assert.notNull(inputStream);
			IOUtils.copy(inputStream, outputStream);
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
			ftpClient.logout();
		} catch (SocketException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e2) {
			throw new RuntimeException(e2.getMessage(), e2);
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
				}
			} catch (IOException e3) {
			}
			
		}
	}
	
	public interface GetInputStream {
		void handle(InputStream inputStream) throws IOException;
	}
	
	public void getInputStream(String pathname, GetInputStream get) {
		getInputStream(pathname, get, host, port, username, password);
	}
	
	public static void getInputStream(String pathname, GetInputStream get, String host, int port, String username, String password) {
		pathname = dealPathname(pathname);
		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(host, port);
			ftpClient.login(username, password);
			ftpClient.setFileTransferMode(10);
			ftpClient.setFileType(2);
			ftpClient.enterLocalPassiveMode();
			Assert.state(FTPReply.isPositiveCompletion(ftpClient.getReplyCode()));
			String directory = StringUtils.substringBeforeLast(pathname, "/");
			String filename = StringUtils.substringAfterLast(pathname, "/");
			Assert.state(ftpClient.changeWorkingDirectory(directory));
			InputStream inputStream = ftpClient.retrieveFileStream(filename);
			Assert.notNull(inputStream);
			get.handle(inputStream);
			IOUtils.closeQuietly(inputStream);
			ftpClient.logout();
		} catch (SocketException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e2) {
			throw new RuntimeException(e2.getMessage(), e2);
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
				}
			} catch (IOException e3) {
			}
			
		}
	}
	
	public String upload(String path, File file) {
		try {
			return upload(path, (InputStream) (new FileInputStream(file)));
		} catch (FileNotFoundException var3) {
			var3.printStackTrace();
			return null;
		}
	}
	
	public String upload(String path, InputStream is) {
		return upload(path, is, host, port, username, password);
	}
	
	
	public FTPClient getInstance() throws IOException {
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect(host, port);
		ftpClient.login(username, password);
		ftpClient.setFileTransferMode(10);
		ftpClient.setFileType(2);
		ftpClient.enterLocalPassiveMode();
		return ftpClient;
	}
	
	/**
	 * 上传文件
	 *
	 * @param path     上传文件路径+名称
	 * @param is       输入流
	 * @param host     FTP地址
	 * @param port     端口
	 * @param username 用户名
	 * @param password 密码
	 * @return 请求地址
	 */
	public static String upload(String path, InputStream is, String host, int port, String username, String password) {
		path = dealPathname(path);
		FTPClient ftpClient = new FTPClient();
		BufferedInputStream inputStream = null;
		
		try {
			inputStream = new BufferedInputStream(is);
			ftpClient.connect(host, port);
			ftpClient.login(username, password);
			ftpClient.setFileTransferMode(10);
			ftpClient.setFileType(2);
			ftpClient.enterLocalPassiveMode();
			Assert.state(FTPReply.isPositiveCompletion(ftpClient.getReplyCode()));
			String directory = StringUtils.substringBeforeLast(path, "/");
			String filename = StringUtils.substringAfterLast(path, "/");
			if (!ftpClient.changeWorkingDirectory(directory)) {
				String[] paths = StringUtils.split(directory, "/");
				String p = "/";
				ftpClient.changeWorkingDirectory(p);
				for (String s : paths) {
					p += s + "/";
					if (!ftpClient.changeWorkingDirectory(p)) {
						ftpClient.makeDirectory(s);
						ftpClient.changeWorkingDirectory(p);
					}
				}
			}
			Assert.state(ftpClient.storeFile(filename, inputStream));
			ftpClient.logout();
			String url = requestUrl + "/" + path;
			return url;
		} catch (SocketException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e2) {
			throw new RuntimeException(e2.getMessage(), e2);
		} finally {
			IOUtils.closeQuietly(inputStream);
			try {
				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
				}
			} catch (IOException e3) {
			}
			
		}
	}
	
	private void disconnect(FTPClient ftpClient) {
		try {
			if (ftpClient.isConnected()) {
				ftpClient.disconnect();
			}
		} catch (IOException e3) {
		}
	}
	
	/**
	 * 删除文件
	 *
	 * @param pathname
	 * @return
	 * @throws IOException
	 */
	public boolean delete(String pathname) throws IOException {
		FTPClient ftpClient = getInstance();
		boolean b = ftpClient.deleteFile(pathname);
		disconnect(ftpClient);
		return b;
	}
}
