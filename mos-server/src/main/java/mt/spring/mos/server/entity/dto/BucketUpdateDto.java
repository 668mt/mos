package mt.spring.mos.server.entity.dto;

import lombok.Data;

/**
 * @Author Martin
 * @Date 2020/10/13
 */
@Data
public class BucketUpdateDto {
	private Long id;
	private String bucketName;
	private Boolean defaultIsPublic;
	private Integer dataFragmentsAmount;
}
