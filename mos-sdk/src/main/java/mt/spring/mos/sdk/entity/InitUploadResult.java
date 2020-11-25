package mt.spring.mos.sdk.entity;

import lombok.Data;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/23
 */
@Data
public class InitUploadResult {
	private boolean fileExists;
	private List<Integer> existedChunkIndexs;
	
	public boolean hasUploaded(int chunkIndex) {
		return existedChunkIndexs != null && existedChunkIndexs.contains(chunkIndex);
	}
}
