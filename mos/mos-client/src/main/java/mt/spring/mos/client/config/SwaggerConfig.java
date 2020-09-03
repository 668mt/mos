package mt.spring.mos.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

/**
 * @Author Martin
 * @Date 2020/5/15
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
	@Bean
	public Docket portalDocket() {
		Contact contact = new Contact("李茂涛", "", "765936728@qq.com");
		ApiInfo apiInfo = new ApiInfo(
				"Mos客户端", //项目标题
				"Mos客户端", //项目描述
				"1.0.0", //版本
				null, //应用url
				contact, //作者名称, 网页, 联系方式
				null, //许可
				null, Collections.emptyList()); //许可url
		return new Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo)
				.select()
				.apis(RequestHandlerSelectors.basePackage("mt.spring.mos"))
				.paths(PathSelectors.any()).build();
	}
}
