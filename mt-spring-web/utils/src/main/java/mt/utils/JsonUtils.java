package mt.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;


/**
 * JSON工具，Jackson
 *
 * @author Martin
 * @ClassName: JsonUtils
 * @Description:
 * @date 2017-6-12 下午4:44:16
 */
public final class JsonUtils {
	
	/**
	 * ObjectMapper
	 */
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	static {
		OBJECT_MAPPER.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//		OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL);
		OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
	}
	
	/**
	 * 不可实例化
	 */
	private JsonUtils() {
	}
	
	/**
	 * 将对象转换为JSON字符串
	 *
	 * @param value 对象
	 * @return JSON字符串
	 */
	public static String toJson(@NotNull Object value) {
		try {
			return OBJECT_MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public static String toPrettyJson(@NotNull Object value) {
		try {
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 转换成Map<String,String>
	 *
	 * @param json
	 * @return
	 */
	public static Map<String, String> toStringMap(@NotNull String json) {
		return toObject(json, new TypeReference<Map<String, String>>() {
		});
	}
	
	public static Map<String, Object> toObjectMap(@NotNull String json) {
		return toObject(json, new TypeReference<Map<String, Object>>() {
		});
	}
	
	/**
	 * 通过指定的视图
	 *
	 * @param <T>   jsonView 视图
	 * @param value
	 * @return
	 */
	public static <T> String toJsonByJsonView(@NotNull Object value, @NotNull Class<T> jsonView) {
		try {
			return OBJECT_MAPPER.writerWithView(jsonView).writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 通过指定的视图
	 *
	 * @param json
	 * @param jsonView
	 * @param <T>
	 * @return
	 */
	public static <T> String toObjectByJsonView(@NotNull String json, @NotNull Class<T> jsonView) {
		try {
			return OBJECT_MAPPER.readerWithView(jsonView).readValue(json);
		} catch (JsonParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 将JSON字符串转换为对象
	 *
	 * @param json      JSON字符串
	 * @param valueType 类型
	 * @return 对象
	 */
	public static <T> T toObject(@NotNull String json, @NotNull Class<T> valueType) {
		
		try {
			return OBJECT_MAPPER.readValue(json, valueType);
		} catch (JsonParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 将JSON字符串转换为对象
	 *
	 * @param json          JSON字符串
	 * @param typeReference 类型
	 * @return 对象
	 */
	public static <T> T toObject(@NotNull String json, @NotNull TypeReference<?> typeReference) {
		
		try {
			return OBJECT_MAPPER.readValue(json, typeReference);
		} catch (JsonParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 将JSON字符串转换为对象
	 *
	 * @param json     JSON字符串
	 * @param javaType 类型
	 * @return 对象
	 */
	public static <T> T toObject(@NotNull String json, @NotNull JavaType javaType) {
		
		try {
			return OBJECT_MAPPER.readValue(json, javaType);
		} catch (JsonParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 将JSON字符串转换为树
	 *
	 * @param json JSON字符串
	 * @return 树
	 */
	public static JsonNode toTree(@NotNull String json) {
		
		try {
			return OBJECT_MAPPER.readTree(json);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 将对象转换为JSON流
	 *
	 * @param writer Writer
	 * @param value  对象
	 */
	public static void writeValue(@NotNull Writer writer, @NotNull Object value) {
		
		try {
			OBJECT_MAPPER.writeValue(writer, value);
		} catch (JsonGenerationException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (JsonMappingException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 构造类型
	 *
	 * @param type 类型
	 * @return 类型
	 */
	public static JavaType constructType(@NotNull Type type) {
		
		return TypeFactory.defaultInstance().constructType(type);
	}
	
	/**
	 * 构造类型
	 *
	 * @param typeReference 类型
	 * @return 类型
	 */
	public static JavaType constructType(@NotNull TypeReference<?> typeReference) {
		
		return TypeFactory.defaultInstance().constructType(typeReference);
	}
	
	/**
	 * 将对象转成json，中文转义为unicode
	 *
	 * @param obj
	 * @return
	 */
	public static String toJsonUnicode(@NotNull Object obj) {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(String.class, new StringUnicodeSerializer());
		objectMapper.registerModule(module);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 将json(unicode转义)转中文对象
	 *
	 * @param json
	 * @param javaType
	 * @return
	 */
	public static <T> T toObjectUnicode(@NotNull String json, @NotNull TypeReference<T> javaType) {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(String.class, new StringUnicodeSerializer());
		objectMapper.registerModule(module);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		try {
			return objectMapper.readValue(json, javaType);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static class StringUnicodeSerializer extends JsonSerializer<String> {
		
		private final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
		private final int[] ESCAPE_CODES = CharTypes.get7BitOutputEscapes();
		
		private void writeUnicodeEscape(JsonGenerator gen, char c) throws IOException {
			gen.writeRaw('\\');
			gen.writeRaw('u');
			gen.writeRaw(HEX_CHARS[(c >> 12) & 0xF]);
			gen.writeRaw(HEX_CHARS[(c >> 8) & 0xF]);
			gen.writeRaw(HEX_CHARS[(c >> 4) & 0xF]);
			gen.writeRaw(HEX_CHARS[c & 0xF]);
		}
		
		private void writeShortEscape(JsonGenerator gen, char c) throws IOException {
			gen.writeRaw('\\');
			gen.writeRaw(c);
		}
		
		@Override
		public void serialize(String str, JsonGenerator gen,
							  SerializerProvider provider) throws IOException,
				JsonProcessingException {
			int status = ((JsonWriteContext) gen.getOutputContext()).writeValue();
			switch (status) {
				case JsonWriteContext.STATUS_OK_AFTER_COLON:
					gen.writeRaw(':');
					break;
				case JsonWriteContext.STATUS_OK_AFTER_COMMA:
					gen.writeRaw(',');
					break;
				case JsonWriteContext.STATUS_EXPECT_NAME:
					throw new JsonGenerationException("Can not write string value here");
			}
			gen.writeRaw('"');//写入JSON中字符串的开头引号
			for (char c : str.toCharArray()) {
				if (c >= 0x80) {
					writeUnicodeEscape(gen, c); // 为所有非ASCII字符生成转义的unicode字符
				} else {
					// 为ASCII字符中前128个字符使用转义的unicode字符
					int code = (c < ESCAPE_CODES.length ? ESCAPE_CODES[c] : 0);
					if (code == 0) {
						gen.writeRaw(c); // 此处不用转义
					} else if (code < 0) {
						writeUnicodeEscape(gen, (char) (-code - 1)); // 通用转义字符
					} else {
						writeShortEscape(gen, (char) code); // 短转义字符 (\n \t ...)
					}
				}
			}
			gen.writeRaw('"');//写入JSON中字符串的结束引号
		}
		
		@SuppressWarnings("deprecation")
		public static List<Object> toJavaList(String json) {
			Assert.notNull(json);
			return JsonUtils.toObject(json, new TypeReference<List<Object>>() {
			});
		}
	}
}

