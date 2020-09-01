package mt.utils;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @Author Martin
 * @Date 2019/8/3
 */
public class BasePackageUtils {
	public static List<String> getClassNames(String packageName){
		//第一个class类的集合
		List<String> classes = new ArrayList<>();
		//获取包的名字，进行替换
		String packageDirName = packageName.replace(".", "/");
		//定义一个枚举的集合 并循环来处理这个目录下的things
		try{
			URL url = Thread.currentThread().getContextClassLoader().getResource(packageDirName);
			if(url == null)
				return classes;
			String protocol = url.getProtocol();
			if("file".equals(protocol)){
				String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
				ClassUtils.findAndAddClassesInPackageByFile(packageName, filePath, true, classes);
			}else if("jar".equals(protocol)){
				JarFile jar;
				try{
					jar = ((JarURLConnection)url.openConnection()).getJarFile();
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()){
						JarEntry jarEntry = entries.nextElement();
						String name = jarEntry.getName();
						if(name.charAt(0) == '/'){
							name = name.substring(1);
						}
						if(name.startsWith(packageDirName)){
							int idx = name.lastIndexOf('/');
							if(idx != -1){
								packageName = name.substring(0,idx).replace('/','.');
							}
							if(name.endsWith(".class") && !jarEntry.isDirectory()){
								String className = name.substring(packageName.length() + 1,name.length());
								classes.add(packageName + "."+className);
							}
						}
					}
				}catch (IOException e){
					e.printStackTrace();
				}
			}
		}catch (IOException e){
			e.printStackTrace();
		}
		return classes;
	}

	public static String getBasePackage(Class<?> locationClass){
		Set<String> packageSet = new HashSet<>();
		String name = locationClass.getPackage().getName();
		String[] split = name.split("\\.");
		String packageName = split[0];
		if(split.length > 1){
			packageName += "."+split[1];
		}
		List<String> cn = getClassNames(packageName);
		for (String className : cn) {
			int i = className.lastIndexOf(".");
			packageSet.add(className.substring(0, i));
		}
		boolean isContains = true;
		StringBuilder sb = new StringBuilder();
		List<String> packages = new ArrayList<>(packageSet);
		Collections.sort(packages, Comparator.comparingInt(value -> value.length()));
		String first = packages.get(0);
		sb.append(first.charAt(0));
		do {
			for (String aPackage : packages) {
				if(!aPackage.startsWith(sb.toString())){
					isContains = false;
					break;
				}
			}
			if(isContains){
				if(first.length() > sb.length()){
					sb.append(first.charAt(sb.length()));
				}else{
					break;
				}
			}else{
				sb.delete(sb.length() - 2, sb.length());
			}
		}while (isContains);
		return sb.toString();
	}

}
