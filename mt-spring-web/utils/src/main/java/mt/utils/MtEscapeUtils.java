package mt.utils;

import java.util.ArrayList;
import java.util.List;

public class MtEscapeUtils {

	public static List<String[]> escapeChars = new ArrayList<>();
	
	static{
		escapeChars.add(new String[]{"\"","&quot;"});
		escapeChars.add(new String[]{"&","&amp;"});
		escapeChars.add(new String[]{"'","&#39;"});
		escapeChars.add(new String[]{"<","&lt;"});
		escapeChars.add(new String[]{">","&gt;"});
		escapeChars.add(new String[]{" ","&nbsp;"});
		escapeChars.add(new String[]{"“","&ldquo;"});
		escapeChars.add(new String[]{"”","&rdquo;"});
	}
	
	public static String unescape(String html){
		Assert.notNull(html);
		for(String[] chars : escapeChars){
			//正常显示
			String normal = chars[0];
			String escapechar = chars[1];
			html = html.replace(escapechar, normal);
		}
		return html;
	}
	
}
