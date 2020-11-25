package mt.spring.mos.sdk.entity.upload;

import lombok.Data;

import static mt.spring.mos.base.utils.IOUtils.MB;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
@Data
public class UploadConfig {
	private int expectChunks = 100;
	private long minPartSize = MB;
	private long maxPartSize = 20 * MB;
	private int threadPoolCore = 5;
}
