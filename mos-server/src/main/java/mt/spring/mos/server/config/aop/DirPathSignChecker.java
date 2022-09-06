package mt.spring.mos.server.config.aop;

import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.sdk.type.DirPathsEncryptContent;
import mt.spring.mos.sdk.type.EncryptContent;
import mt.spring.mos.server.entity.po.Bucket;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2022/9/4
 */
@Component
public class DirPathSignChecker implements SignChecker {
    @Override
    public boolean checkIsHasPerm(HttpServletRequest request, Bucket bucket, EncryptContent content, List<String> pathnames) {
        Set<String> pathnameList = new HashSet<>();
        if (CollectionUtils.isNotEmpty(pathnames)) {
            pathnameList.addAll(pathnames);
        }
        String queryPath = request.getParameter("path");
        if (StringUtils.isNotBlank(queryPath)) {
            pathnameList.add(queryPath);
        }
        if (content instanceof DirPathsEncryptContent && CollectionUtils.isNotEmpty(pathnames)) {
            DirPathsEncryptContent dirPathsEncryptContent = (DirPathsEncryptContent) content;
            List<String> paths = dirPathsEncryptContent.getPaths();
            if (CollectionUtils.isEmpty(paths)) {
                return false;
            }
            paths = paths.stream().map(s -> s.startsWith("/") ? s : "/" + s).collect(Collectors.toList());
            for (String pathname : pathnameList) {
                if (!pathname.startsWith("/")) {
                    pathname = "/" + pathname;
                }
                boolean pass = false;
                for (String path : paths) {
                    if (pathname.startsWith(path + "/") || pathname.equals(path)) {
                        pass = true;
                        break;
                    }
                }
                if (!pass) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
