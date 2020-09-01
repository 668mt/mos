//package mt.utils.redis;
//
//import org.springframework.data.redis.serializer.RedisSerializer;
//import org.springframework.data.redis.serializer.SerializationException;
//import org.springframework.util.SerializationUtils;
//
///**
// * @Author Martin
// * @Date 2018/6/9
// */
//public class ObjectSerialize implements RedisSerializer<Object>{
//	@Override
//	public byte[] serialize(Object o) throws SerializationException {
//		return SerializationUtils.serialize(o);
//	}
//
//	@Override
//	public Object deserialize(byte[] bytes) throws SerializationException {
//		return SerializationUtils.deserialize(bytes);
//	}
//}
