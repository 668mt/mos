package mt.spring.mos.server.service;

import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.server.entity.po.Bucket;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Author Martin
 * @Date 2023/9/5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
	"java.io.tmpdir=D:/temp",
	"spring.profiles.active=dev",
	"NACOS_ENABLED=true",
	"NACOS_SERVER=192.168.0.2:8848",
	"DNACOS_SERVICE=mos-server-dev"
})
@RunWith(SpringRunner.class)
public class BaseSpringBootTest {
	protected Bucket bucket;
	@Autowired
	protected BucketService bucketService;
	protected MosSdk mosSdk;
	
	@Before
	public void before() {
		String bucketName = "default";
		bucket = bucketService.findOne("bucketName", bucketName);
		mosSdk = new MosSdk("http://localhost:9700", 5, "default", "b-T3wXaUu5umA3vumqEIVA==");
	}
}
