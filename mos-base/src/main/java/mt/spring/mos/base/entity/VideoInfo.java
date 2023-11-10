package mt.spring.mos.base.entity;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2022/3/12
 */
@Data
public class VideoInfo {
	private long during;
	private String videoLength;
	private String format;
	private int width;
	private int height;
	private float frameRate;
	private int bitRate;
	private String decoder;
	
}
