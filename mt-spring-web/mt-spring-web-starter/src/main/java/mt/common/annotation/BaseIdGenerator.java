package mt.common.annotation;


public interface BaseIdGenerator{
	Object generate(String tableName, IdGenerator idGenerator);
}
