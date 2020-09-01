package mt.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 
* @ClassName: MyZip
* @Description: 自己写的zip压缩
* @author Martin
* @date 2017-5-16 下午4:26:59
*
 */
public class MyZip {
	
	/**
	 * 递归获取指定文件夹中的文件集合
	 * @param path
	 * @return
	 */
	public static List<File> getFiles(String path){
		List<File> list = new ArrayList<File>();
		
		File file = new File(path);
		if(file.isDirectory()){
			File[] listFiles = file.listFiles();
			if(listFiles!=null && listFiles.length>0){
				for (File file2 : listFiles) {
					if(file2.isDirectory()){
						List<File> files = getFiles(file2.toString());
						list.addAll(files);
					}else{
						list.add(file2);
					}
				}
			}
		}else{
			list.add(file);
		}
		return list;
	}
	
	/**
	 * 压缩文件/文件夹
	 * @param path
	 * @param desPath
	 * @param isDelSrc 是否删除源文件
	 * @throws IOException
	 */
	public static boolean compress(String path, String desPath, boolean isCover, boolean isDelSrc) throws IOException {
		path = path.replace("/", "\\");
		desPath = desPath.replace("/", "\\");
		long time1 = System.currentTimeMillis();
		List<File> files = getFiles(path);
		//判断文件夹不为空
		if(files!=null && files.size()>0){
			File file2 = new File(path);
			if(!file2.exists()){
				return false;
			}
			String name = file2.getName();
			if(file2.isFile()){
				int lastIndexOf = name.lastIndexOf(".");
				if(lastIndexOf!=-1){
					name = name.substring(0, lastIndexOf);
				}
			}
			BufferedInputStream bis;
			File out = new File(desPath,name+".zip");
			if(!isCover){
				out = named(out, 0);
			}
			FileOutputStream fos = new FileOutputStream(out);
			ZipOutputStream zos = new ZipOutputStream(fos);
			for (File file : files) {
				bis = new BufferedInputStream(new FileInputStream(file));
				String fileName = file.toString().replace(path+"\\", "");
				ZipEntry ze = new ZipEntry(fileName);
				zos.putNextEntry(ze);
				byte[] b = new byte[1024];
				int len;
				while((len = bis.read(b, 0, b.length))!=-1){
					zos.write(b,0,len);
				}
				zos.flush();
				bis.close();
			}
			zos.close();
			if(isDelSrc){
				deleteFile(path);
			}
			long time2 = System.currentTimeMillis();
			double runtime = (time2-time1)*1.0/1000.0;
			System.out.println("用时："+runtime);
			return true;
		}
		return false;
	}
	
	/**
	 * 压缩文件列表
	 * @param list
	 * @return 文件字节数组
	 * @throws IOException
	 */
	public static byte[] compress(List<File> list) throws IOException {
		BufferedInputStream bis;
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(bos);
		for (File file : list) {
			if(file.exists()){
				String name = file.getName();
				bis = new BufferedInputStream(new FileInputStream(file));
				ZipEntry ze = new ZipEntry(name);
				zos.putNextEntry(ze);
				byte[] b = new byte[1024];
				int len;
				while((len = bis.read(b, 0, b.length))!=-1){
					zos.write(b,0,len);
				}
				zos.flush();
				bis.close();
			}else{
				System.out.println("文件不存在");
				return null;
			}
		}
		zos.close();
		bos.close();
		return bos.toByteArray();
	}
	
	/**
	 * 文件名重复，自动重新命名
	 * @param file
	 * @param num
	 * @return
	 */
	public static File named(File file, int num){
		if(file.exists()){
			num++;
			String fileName = file.getName();
			int indexOf1 = fileName.indexOf("(");
			int indexOf2 = fileName.indexOf(")");
			if(indexOf1!=-1 && indexOf2!=-1){
				fileName = fileName.substring(0,indexOf1)+fileName.substring(indexOf2+1);
			}
			
			String name = null;
			String suffix = null;
			int lastIndexOf = fileName.lastIndexOf(".");
			if(lastIndexOf!=-1){
				name = fileName.substring(0, lastIndexOf);
				suffix = fileName.substring(lastIndexOf);
			}
			String path = file.getPath();
			int lastIndexOf2 = path.lastIndexOf("\\");
			if(lastIndexOf2!=-1){
				path = path.substring(0,lastIndexOf2);
			}
			file = new File(path,name+"("+num+")"+suffix);
			
			file = named(file,num);
		}
		return file;
	}
	
	/**
	 * 删除文件夹
	 * @param path
	 */
	public static boolean deleteFile(String path){
		File file = new File(path);
		if(file.isDirectory()){
			File[] listFiles = file.listFiles();
			for (File file2 : listFiles) {
				if(!deleteFile(file2.toString())){
					return false;
				}
			}
		}
		return file.delete();
	}
	
//	public static void main(String[] args){
//		String filePath1 = "C:\\Users\\Administrator.PC-20170226CXSX\\Desktop\\查询结果\\SQLQuery1.sql";
//		String filePath2 = "C:\\Users\\Administrator.PC-20170226CXSX\\Desktop\\查询结果\\fms数据库数据2.sql";
//		String filePath3 = "C:\\Users\\Administrator.PC-20170226CXSX\\Desktop\\查询结果\\出入库信息.xls";
//		String filePath4 = "F:\\apache-tomcat-7.0.77\\webapps\\ROOT\\tomcat-power.gif";
//		
//		List<File> list = new ArrayList<File>();
//		list.add(new File(filePath1));
//		list.add(new File(filePath2));
//		list.add(new File(filePath3));
//		list.add(new File(filePath4));
//		
//		try {
//			byte[] compress = compress(list);
//			System.out.println(compress.length);
//			ByteArrayInputStream bis = new ByteArrayInputStream(compress);
//			FileOutputStream fos = new FileOutputStream(new File("C:\\Users\\Administrator.PC-20170226CXSX\\Desktop\\查询结果","test.zip"));
//			byte [] b = new byte[1024];
//			int len = 0;
//			while((len=bis.read(b, 0, b.length))!=-1){
//				fos.write(b,0,len);
//			}
//			fos.flush();
//			fos.close();
//			bis.close();
//			System.out.println("下载完成");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
