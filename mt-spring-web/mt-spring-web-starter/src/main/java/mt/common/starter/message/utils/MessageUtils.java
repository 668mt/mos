package mt.common.starter.message.utils;

import lombok.extern.slf4j.Slf4j;
import mt.common.annotation.Filter;
import mt.common.config.CommonProperties;
import mt.common.converter.Converter;
import mt.common.entity.PageBean;
import mt.common.starter.message.annotation.Message;
import mt.common.starter.message.exception.FieldNotFoundException;
import mt.common.starter.message.messagehandler.DefaultMessageHandler;
import mt.common.starter.message.messagehandler.MessageHandler;
import mt.utils.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: MessageUtils
 * @Description:
 * @Author LIMAOTAO236
 * @date 2017-11-30 上午10:13:47
 */
@Slf4j
public class MessageUtils {
	
	private Map<String, MessageHandler> messageHandlers;
	
	private CommonProperties commonProperties;
	
	public MessageUtils(CommonProperties commonProperties, Map<String, MessageHandler> messageHandlers) {
		this.messageHandlers = messageHandlers;
		this.commonProperties = commonProperties;
	}
	
	/**
	 * 是否继续处理这个字段
	 *
	 * @param field
	 * @return
	 */
	private boolean isContinueMessage(@NotNull Field field) {
		Class<?> type = field.getType();
		if (type.getPackage() == null) {
			return false;
		}
		if (type.getPackage().getName().matches(commonProperties.getMessager().getDealPackage() + ".+")) {
			return true;
		}
		if (type.equals(Object.class)) {
			return true;
		}
		return false;
	}
	
	private MessageHandler getMessageHandler(@NotNull Message message) {
		String handleBeanName = message.handlerBeanName();
		//使用Bean的名字进行查找
		if (StringUtils.isNotBlank(handleBeanName)) {
			return messageHandlers.get(handleBeanName);
		}
		Class<? extends MessageHandler> clazz = message.value().equals(DefaultMessageHandler.class) ? message.handlerClass() : message.value();
		Assert.notNull(clazz, "handleBeanName和handlerClass不能同时为空");
		//使用类名查找
		for (Map.Entry<String, MessageHandler> entry : messageHandlers.entrySet()) {
			MessageHandler messageHandler = entry.getValue();
			if (clazz.equals(messageHandler.getClass())) {
				return messageHandler;
			}
		}
		throw new IllegalStateException("找不到messageHandler：" + clazz.getSimpleName());
	}
	
	public Object message(@Nullable Object object, @Nullable String... includeFields) {
		//初始化所有messageHandler
		for (Map.Entry<String, MessageHandler> messageHandlerEntry : messageHandlers.entrySet()) {
			messageHandlerEntry.getValue().init();
		}
		return messageRecursive(object, includeFields);
	}
	
	/**
	 * 处理一个对象
	 *
	 * @param object
	 * @param includeFields
	 * @return
	 */
	@SuppressWarnings({"unchecked"})
	public Object messageRecursive(@Nullable Object object, @Nullable String... includeFields) {
		if (object == null)
			return null;
		if (object instanceof Collection)
			return dealWithCollection((Collection) object, includeFields);
		if (object instanceof PageBean)
			return dealWithPageBean((PageBean) object, includeFields);
		if (object instanceof Map) {
			Map map = (Map) object;
			return dealWithMap(map, includeFields);
		}
		
		List<String> includeList = null;
		if (MyUtils.isNotEmpty(includeFields)) {
			includeList = MyUtils.toList(includeFields);
		}
		//拉出mybatis缓存
		JsonUtils.toJson(object);
		//查找实体类所有字段
		List<Field> fields = ReflectUtils.findAllFields(object.getClass());
		if (MyUtils.isEmpty(fields)) {
			return object;
		}
		
		for (Field field : fields) {
			try {
				if (MyUtils.isNotEmpty(includeList) && !includeList.contains(field.getName())) {
					continue;
				}
				if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				field.setAccessible(true);
				//内嵌集合
				if (Collection.class.isAssignableFrom(field.getType())) {
					Object value = field.get(object);
					if (value == null)
						continue;
					Collection collection = (Collection) value;
					Iterator iterator = collection.iterator();
					while (iterator.hasNext()) {
						messageRecursive(iterator.next());
					}
				}
				//处理其它类型
				if (isContinueMessage(field) && !Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
					if (field.get(object) == null)
						continue;
					messageRecursive(field.get(object));
				}
				//获取注解
				Message message = AnnotatedElementUtils.findMergedAnnotation(field, Message.class);
				if (message == null)
					continue;
				//条件
				String condition = message.condition();
				if (StringUtils.isNotBlank(condition)) {
					//替换变量
					condition = replaceVariable(condition, object);
					//解析js条件表达式
					Boolean result = JsUtils.eval(condition, Boolean.class);
					//不满足条件
					if (result == null || !result)
						continue;
				}
				//替换变量值
				Object[] params = parseParams(field, message.params(), object);
				//计算出结果
				MessageHandler messageHandler = getMessageHandler(message);
				Object value = messageHandler.handle(params, message.mark());
				if (value != null && field.getType().isAssignableFrom(value.getClass())) {
					field.set(object, value);
				} else {
					log.warn("字段{}转码失败,fieldType:{}，valueClass:{}", field.getName(), field.getType(), value != null ? value.getClass() : null);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return object;
	}
	
	public Object[] parseParams(@NotNull Field field, @NotNull String[] params, @NotNull Object object) {
		if (params.length == 0) {
			field.setAccessible(true);
			try {
				return new Object[]{field.get(object)};
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		Object[] afterParams = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			afterParams[i] = parseParam(field, params[i], object);
		}
		return afterParams;
	}
	
	/**
	 * 解析参数值
	 *
	 * @param field  当前字段
	 * @param param  参数，为空则取当前字段，不为空 #id 表示取值id字段
	 * @param object 实体对象
	 * @return 解析后的值
	 */
	public Object parseParam(@NotNull Field field, @NotNull String param, @NotNull Object object) {
		try {
			if (StringUtils.isBlank(param)) {
				field.setAccessible(true);
				return field.get(object);
			}
			if (!param.contains("#"))
				return param;
			String fieldName = RegexUtils.findFirst(param, "#(\\w+)", 1);
			Field field1 = ReflectUtils.findField(object.getClass(), fieldName);
			if (field1 == null)
				throw new FieldNotFoundException("找不到字段：" + fieldName);
			field1.setAccessible(true);
			return field1.get(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String replaceVariable(String param, Object object) {
		return replaceVariable(param, object, true);
	}
	
	/**
	 * 替换变量
	 *
	 * @param param
	 * @param object
	 * @return
	 */
	public static String replaceVariable(String param, Object object, boolean checkSqlInject) {
		if (object == null) {
			return param;
		}
		if (StringUtils.isBlank(param)) {
			return param;
		}
		List<String> findList = RegexUtils.findList(param, "#(\\w+)", 1);
		if (MyUtils.isEmpty(findList)) {
			//没有变量设置
			return param;
		}
		for (String fieldName : findList) {
			
			Field findField = ReflectUtils.findField(object.getClass(), fieldName);
			if (findField == null) {
				continue;
			}
			try {
				findField.setAccessible(true);
				//获取字段值
				Object objectParam = findField.get(object);
				if (objectParam == null) {
					continue;
				}
				Filter annotation = AnnotatedElementUtils.getMergedAnnotation(findField, Filter.class);
				if (annotation != null) {
					Class<? extends Converter<?>> converter = annotation.converter();
					objectParam = converter.getDeclaredMethod("convert", Object.class).invoke(converter.newInstance(), objectParam);
				}
				String value = objectParam + "";
				if (checkSqlInject) {
					value = StringEscapeUtils.escapeSql(value);
				}
				//进行替换
				param = param.replace("#" + fieldName, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return param;
	}
	
	/**
	 * message注解处理pageBean模型
	 *
	 * @param <T>
	 * @param pageBean
	 * @return
	 */
	public <T> PageBean<T> dealWithPageBean(PageBean<T> pageBean, String... includeFields) {
		if (pageBean != null && MyUtils.isNotEmpty(pageBean.getRows())) {
			List<T> list = pageBean.getRows();
			for (T t : list) {
				messageRecursive(t, includeFields);
			}
		}
		if (pageBean != null && MyUtils.isNotEmpty(pageBean.getList())) {
			List<T> list = pageBean.getList();
			for (T t : list) {
				messageRecursive(t, includeFields);
			}
		}
		return pageBean;
	}
	
	public <T> Collection<T> dealWithCollection(Collection<T> list, String... includeFields) {
		if (MyUtils.isNotEmpty(list)) {
			for (T t : list) {
				messageRecursive(t, includeFields);
			}
		}
		return list;
	}
	
	public Map dealWithMap(@NotNull Map map, String[] includeFields) {
		for (Object entry : map.entrySet()) {
			messageRecursive(((Map.Entry) entry).getValue(), includeFields);
		}
		return map;
	}
	
}
