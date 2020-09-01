package mt.utils.httpclient;

/**
 * @Author Martin
 * @Date 2018/9/24
 */
public class Test1 {
	public static void main(String[] args) {
		MyHttpClient myHttpClient = new MyHttpClient("https://www.baidu.com");
		String result = myHttpClient.connect();
		System.out.println(result);
	}
}
