//package mt.spring.mos.server.controller.testconfig;
//
//import tk.mybatis.mapper.common.BaseMapper;
//
//import java.lang.reflect.ParameterizedType;
//import java.lang.reflect.Proxy;
//import java.lang.reflect.Type;
//import java.util.stream.Stream;
//
///**
// * @Author Martin
// * @Date 2021/8/28
// */
//public class BaseMapperHelper {
//	public static Class<?> getBaseMapperGenericType(Object obj) {
//		Class<?> aClass = obj.getClass();
//		if (obj instanceof Proxy) {
//			Class<?>[] interfaces = aClass.getInterfaces();
//			Class<?> anInterface = Stream.of(interfaces)
//					.filter(BaseMapper.class::isAssignableFrom)
//					.findFirst()
//					.orElse(null);
//			if (anInterface == null) {
//				return null;
//			}
//			return getBaseMapperGenericType(anInterface);
//		} else {
//			return getBaseMapperGenericType(aClass);
//		}
//	}
//
//	public static Class<?> getBaseMapperGenericType(Class<?> aClass) {
//		Type[] types = aClass.getGenericInterfaces();
//		if (types.length == 0) {
//			return null;
//		}
//		return Stream.of(types)
//				.filter(type -> type instanceof ParameterizedType)
//				.findFirst()
//				.map(type -> (ParameterizedType) type)
//				.filter(type -> type.getActualTypeArguments().length > 0)
//				.map(type -> {
//					Type[] actualTypeArguments = type.getActualTypeArguments();
//					return (Class<?>) actualTypeArguments[0];
//				})
//				.orElse(null);
//	}
//
//}
