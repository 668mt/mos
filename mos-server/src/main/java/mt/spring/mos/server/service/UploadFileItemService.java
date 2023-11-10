package mt.spring.mos.server.service;

import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.UploadFileItem;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author Martin
 * @Date 2023/9/9
 */
@Service
public class UploadFileItemService extends BaseServiceImpl<UploadFileItem> {
	
	@Transactional(rollbackFor = Exception.class)
	public void deleteItems(@NotNull Long uploadFileId) {
		delete("uploadFileId", uploadFileId);
	}
	
	public List<UploadFileItem> findItems(@NotNull Long uploadFileId) {
		return findList("uploadFileId", uploadFileId);
	}
	
	public UploadFileItem findItem(@NotNull Long uploadFileId, int chunkIndex) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("uploadFileId", Filter.Operator.eq, uploadFileId));
		filters.add(new Filter("chunkIndex", Filter.Operator.eq, chunkIndex));
		return findOneByFilters(filters);
	}
	
	public int countItems(@NotNull Long uploadFileId) {
		return count(Collections.singletonList(new Filter("uploadFileId", Filter.Operator.eq, uploadFileId)));
	}
}
