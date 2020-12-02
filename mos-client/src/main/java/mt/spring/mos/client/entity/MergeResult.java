package mt.spring.mos.client.entity;

import lombok.Data;

import java.io.File;

/**
 * @Author Martin
 * @Date 2020/12/2
 */
@Data
public class MergeResult {
	private File file;
	private long length;
}
