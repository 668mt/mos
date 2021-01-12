package mt.spring.mos.server.service.clientapi;

import mt.spring.mos.base.entity.ClientInfo;
import mt.spring.mos.server.entity.dto.MergeFileResult;
import mt.spring.mos.server.entity.dto.Thumb;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public interface IClientApi {
	/**
	 * 删除文件
	 *
	 * @param pathname 路径名
	 */
	void deleteFile(String pathname);
	
	/**
	 * 删除文件夹
	 *
	 * @param path 路径
	 */
	void deleteDir(String path);
	
	/**
	 * 移动文件
	 *
	 * @param srcPathname 源文件
	 * @param desPathname 目标文件
	 */
	default void moveFile(String srcPathname, String desPathname) {
		moveFile(srcPathname, desPathname, false);
	}
	
	/**
	 * 移动文件
	 *
	 * @param srcPathname 源文件
	 * @param desPathname 目标文件
	 * @param cover       是否覆盖
	 */
	void moveFile(String srcPathname, String desPathname, boolean cover);
	
	/**
	 * 获取文件大小
	 *
	 * @param pathname 文件名
	 * @return 文件大小，文件不存在时返回-1
	 */
	long size(String pathname);
	
	/**
	 * 获取md5
	 *
	 * @param pathname 文件名
	 * @return md5
	 */
	String md5(String pathname);
	
	/**
	 * 文件是否存在
	 *
	 * @param pathname 文件
	 * @return 是否存在
	 */
	boolean isExists(String pathname);
	
	/**
	 * @return
	 */
	ClientInfo getInfo();
	
	boolean isEnableImport();
	
	/**
	 * 合并文件
	 *
	 * @param path        路径
	 * @param chunks      分片数
	 * @param desPathname 生成的文件
	 * @param getMd5      是否获取合成后的md5
	 * @param encode      是否加密
	 * @return 合并结果
	 */
	MergeFileResult mergeFiles(String path, int chunks, String desPathname, boolean getMd5, boolean encode);
	
	/**
	 * 生成等比例缩略图
	 *
	 * @param pathname  文件名
	 * @param encodeKey 加密key
	 * @param seconds   截图时间点，单位s
	 * @param width     宽度
	 * @return 缩略图信息
	 */
	Thumb createThumb(String pathname, String encodeKey, int seconds, int width);
	
	/**
	 * 上传
	 *
	 * @param inputStream 文件流
	 * @param pathname    文件
	 * @throws IOException 异常
	 */
	void upload(InputStream inputStream, String pathname) throws IOException;
	
	/**
	 * 是否活着
	 *
	 * @return 是否活着
	 */
	boolean isAlive();
}
