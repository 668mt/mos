package mt.spring.mos.plugin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.plugin.config.UploadProperties;
import mt.spring.mos.plugin.config.YamlData;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2021/1/8
 */
@Slf4j
public class PluginMain {
	private UploadProperties uploadProperties;
	
	public PluginMain() {
		String currentPath = System.getProperty("user.dir");
		String configFile = System.getProperty("configFile");
		if (StringUtils.isNotEmpty(configFile)) {
			loadProperties(new File(configFile));
		}
		loadProperties(new File(currentPath, "mos.yaml"));
		loadProperties(new File(currentPath, "conf/mos.yaml"));
		if (uploadProperties == null) {
			uploadProperties = new UploadProperties();
		}
		
		String host = System.getProperty("host");
		if (StringUtils.isNotBlank(host)) {
			uploadProperties.setHost(host);
		}
		String openId = System.getProperty("openId");
		if (StringUtils.isNotBlank(openId)) {
			uploadProperties.setOpenId(Long.parseLong(openId));
		}
		String bucketName = System.getProperty("bucketName");
		if (StringUtils.isNotBlank(bucketName)) {
			uploadProperties.setBucketName(bucketName);
		}
		String secretKey = System.getProperty("secretKey");
		if (StringUtils.isNotBlank(secretKey)) {
			uploadProperties.setSecretKey(secretKey);
		}
		
		String cover = System.getProperty("cover");
		if (StringUtils.isNotBlank(cover)) {
			uploadProperties.setCover(Boolean.parseBoolean(cover));
		}
		String srcFile = System.getProperty("srcFile");
		if (StringUtils.isNotBlank(srcFile)) {
			uploadProperties.setSrcFile(srcFile);
		}
		String desPath = System.getProperty("desPath");
		if (StringUtils.isNotBlank(desPath)) {
			uploadProperties.setDesPath(desPath);
		}
		String desName = System.getProperty("desName");
		if (StringUtils.isNotBlank(desName)) {
			uploadProperties.setDesName(desName);
		}
	}
	
	private void loadProperties(File file) {
		if (uploadProperties != null) {
			return;
		}
		if (!file.exists() && file.isFile()) {
			return;
		}
		Yaml yaml = new Yaml();
		try (InputStream inputStream = new FileInputStream(file)) {
			YamlData yamlData = yaml.loadAs(inputStream, YamlData.class);
			uploadProperties = yamlData.getMos();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
	
	public void doUpload() throws IOException {
		MosSdk mosSdk = null;
		try {
			Long openId = uploadProperties.getOpenId();
			String host = uploadProperties.getHost();
			String bucketName = uploadProperties.getBucketName();
			String secretKey = uploadProperties.getSecretKey();
			String srcFile = uploadProperties.getSrcFile();
			Assert.state(openId != null && openId > 0, "未配置openId");
			Assert.notBlank(host, "未配置：host");
			Assert.notBlank(bucketName, "未配置：bucketName");
			Assert.notBlank(secretKey, "未配置：secretKey");
			Assert.notBlank(srcFile, "未配置：srcFile");
			String desPath = uploadProperties.getDesPath();
			if (desPath == null) {
				desPath = "";
			}
			if (desPath.endsWith("/")) {
				desPath = desPath.substring(0, desPath.length() - 1);
			}
			String desName = uploadProperties.getDesName();
			File file = new File(srcFile);
			Assert.state(file.exists(), "文件不存在:" + srcFile);
			if (StringUtils.isBlank(desName)) {
				desName = file.getName();
			}
			String pathname = desPath + "/" + desName;
			mosSdk = new MosSdk(host, openId, bucketName, secretKey);
			log.info("开始上传：" + srcFile + ",pathname=" + pathname);
			mosSdk.uploadFile(file, new UploadInfo(pathname, uploadProperties.isCover()));
			log.info(srcFile + "上传完毕！");
		} finally {
			if (mosSdk != null) {
				mosSdk.shutdown();
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			loggerContext.getLogger("root").setLevel(Level.INFO);
			loggerContext.getLogger("mt.spring.mos").setLevel(Level.DEBUG);
			new PluginMain().doUpload();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
