package mt.utils;

import com.fasterxml.jackson.core.type.TypeReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

public class JsUtils {
	
	public static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
	
	@SuppressWarnings("unchecked")
	public static <T> T eval(String script, Class<T> type) throws ScriptException {
		ScriptEngine nashorn = scriptEngineManager.getEngineByName("nashorn");
		Object value = nashorn.eval(script);
		return (T) ConvertUtils.convert(value, type);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T eval(String script) throws ScriptException {
		ScriptEngine nashorn = scriptEngineManager.getEngineByName("nashorn");
		Object value = nashorn.eval(script);
		return (T) ConvertUtils.convert(value, Object.class);
	}
	
	@SuppressWarnings("deprecation")
	public static List<Object> toJavaList(String json) {
		Assert.notNull(json);
		return JsonUtils.toObject(json, new TypeReference<List<Object>>() {
		});
	}
	
	public static ScriptEngine getNashorn() {
		return scriptEngineManager.getEngineByName("nashorn");
	}
}
