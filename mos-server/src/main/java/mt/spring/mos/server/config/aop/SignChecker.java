package mt.spring.mos.server.config.aop;

import mt.spring.mos.sdk.utils.EncryptContent;
import mt.spring.mos.server.entity.po.Bucket;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author Martin
 * @Date 2022/9/4
 */
public interface SignChecker {
    boolean checkIsHasPerm(HttpServletRequest request, Bucket bucket, EncryptContent content, List<String> pathnames);
}
