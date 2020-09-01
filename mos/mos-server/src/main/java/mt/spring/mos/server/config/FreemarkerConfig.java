package mt.spring.mos.server.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/5/22
 */
@Configuration
public class FreemarkerConfig {
	
	@Bean
	public CommandLineRunner customFreemarker(final FreeMarkerViewResolver resolver) {
		return args -> {
			//增加视图
			resolver.setViewClass(MyFreemarkerView.class);
//			//添加自定义解析器
//			Map<String, Object> map = resolver.getAttributesMap();
//			for (Map.Entry<String, TemplateModel> entry : models.entrySet()) {
//				map.put(entry.getKey(), entry.getValue());
//			}
		};
	}
	
	public static class MyFreemarkerView extends FreeMarkerView {
		
		/**
		 * 设置全局变量
		 * @param model
		 * @param request
		 * @throws Exception
		 */
		@Override
		protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
			model.put("base", request.getContextPath());
			super.exposeHelpers(model, request);
		}
	}
}
