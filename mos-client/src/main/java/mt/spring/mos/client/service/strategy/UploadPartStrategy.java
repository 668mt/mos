package mt.spring.mos.client.service.strategy;

import mt.spring.mos.client.entity.MosClientProperties;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * @Author Martin
 * @Date 2020/12/21
 */
public class UploadPartStrategy extends WeightStrategy {
	public UploadPartStrategy(MosClientProperties mosClientProperties) {
		super(mosClientProperties);
	}
	
	public String getParentPath(String pathname) {
		int lastIndexOf = pathname.lastIndexOf("/");
		if (lastIndexOf != -1) {
			return pathname.substring(0, lastIndexOf);
		}
		return null;
	}
	
	@Override
	public String getBasePath(@Nullable String pathname, List<MosClientProperties.BasePath> basePaths, long fileSize) {
		if (pathname == null) {
			return super.getBasePath(null, basePaths, fileSize);
		}
		String parentPath = getParentPath(pathname);
		if (parentPath != null && parentPath.endsWith("-parts")) {
			Optional<MosClientProperties.BasePath> first = basePaths.stream().filter(basePath -> new File(basePath.getPath(), parentPath).exists()).findFirst();
			if (first.isPresent()) {
				return first.get().getPath();
			}
		}
		return super.getBasePath(pathname, basePaths, fileSize);
	}
	
	@Override
	public String getName() {
		return "uploadPart";
	}
}
