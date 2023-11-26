package mt.spring.mos.server.config.aop;

import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.sdk.type.EncryptContent;
import mt.spring.mos.sdk.type.PathnamesEncryptContent;
import mt.spring.mos.server.entity.po.Bucket;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2022/9/4
 */
@Component
public class PathnameSignChecker implements SignChecker {

    @Override
    public boolean checkIsHasPerm(HttpServletRequest request, Bucket bucket, EncryptContent content, List<String> pathnames) {
        if (content instanceof PathnamesEncryptContent && CollectionUtils.isNotEmpty(pathnames)) {
            PathnamesEncryptContent pathnamesEncryptContent = (PathnamesEncryptContent) content;
            List<String> list = pathnamesEncryptContent.getPathnames();
            if (CollectionUtils.isEmpty(list)) {
                return false;
            }
            list = list.stream().map(s -> s.startsWith("/") ? s : "/" + s).collect(Collectors.toList());
            for (String pathname : pathnames) {
                if (!pathname.startsWith("/")) {
                    pathname = "/" + pathname;
                }
                if (!list.contains(pathname)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
