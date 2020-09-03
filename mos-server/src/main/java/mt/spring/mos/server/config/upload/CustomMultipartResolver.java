package mt.spring.mos.server.config.upload;

import lombok.SneakyThrows;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author Martin
 * @Date 2019/12/28
 * CommonsMultipartResolver
 */
public class CustomMultipartResolver extends CommonsMultipartResolver {
	@Autowired
	private UploadService uploadService;
	@SneakyThrows
	@NotNull
	@Override
	protected MultipartParsingResult parseRequest(@NotNull HttpServletRequest request) throws MultipartException {
		String encoding = determineEncoding(request);
		FileUpload fileUpload = prepareFileUpload(encoding);
		fileUpload.setProgressListener(new SessionProcessListener(request,uploadService));
		
		List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
		return parseFileItems(fileItems, encoding);
	}
}
