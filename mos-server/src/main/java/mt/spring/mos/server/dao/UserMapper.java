package mt.spring.mos.server.dao;

import mt.common.mybatis.mapper.BaseMapper;
import mt.spring.mos.server.entity.po.User;
import org.springframework.stereotype.Repository;

/**
 * @Author Martin
 * @Date 2020/5/23
 */
@Repository
public interface UserMapper extends BaseMapper<User> {
}
