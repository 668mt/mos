package mt.utils;

import java.io.IOException;

public class Base64 {
	public static String convertPasswordString(String text, boolean asStore) throws IOException {
		return Base64Decode.convertPasswordString(text, asStore);
	}
//	@Test
//	public void test(){
//		try {
//			System.out.println(convertPasswordString("`IiEgJyYl`",false));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
