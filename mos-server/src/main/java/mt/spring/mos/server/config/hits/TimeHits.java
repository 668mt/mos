package mt.spring.mos.server.config.hits;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeHits {
	private String time;
	private Long hits;
	private Date date;
}
