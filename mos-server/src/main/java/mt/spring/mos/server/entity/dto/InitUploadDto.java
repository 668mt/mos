package mt.spring.mos.server.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * @Author Martin
 * @Date 2020/11/23
 */
@Data
public class InitUploadDto {
	private boolean fileExists;
	private List<Integer> existedChunkIndexs;
}
