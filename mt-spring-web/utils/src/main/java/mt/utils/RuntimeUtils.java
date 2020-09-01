package mt.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RuntimeUtils {
	
	public void exec(){
		Runtime runtime = Runtime.getRuntime();
		try {
			Process exec = runtime.exec("java -jar delivery.jar", null, new File("F:\\workspace\\un_delivery\\target"));
			InputStream inputStream = exec.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream,"GBK"));
			String buffer = null;
			while((buffer = br.readLine()) != null){
				System.out.println(buffer);
			}
			runtime.exec("taskkill /F /IM java.exe");
			System.out.println("退出");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
