//package mt.spring.mos.server.controller.testconfig;
//
//import javassist.*;
//import mt.spring.mos.server.controller.test.MapperBeanFactory;
//import mt.spring.mos.server.entity.po.Client;
//import org.springframework.beans.factory.config.BeanDefinitionHolder;
//import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
//import org.springframework.beans.factory.support.BeanDefinitionRegistry;
//import org.springframework.beans.factory.support.GenericBeanDefinition;
//import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
//import org.springframework.core.type.AnnotationMetadata;
//import org.springframework.stereotype.Repository;
//import tk.mybatis.mapper.common.BaseMapper;
//import tk.mybatis.spring.mapper.ClassPathMapperScanner;
//
///**
// * @Author Martin
// * @Date 2021/8/28
// */
//public class MyMybatiMapperRegister implements ImportBeanDefinitionRegistrar {
//	@Override
//	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
//		System.out.println("MyMybatiMapperRegister scan...");
////		ClassPool pool = ClassPool.getDefault();
////		CtClass ctClass = null;
////		try {
////			ctClass = pool.makeInterface("mt.spring.mos.server.controller.testconfig.TestMapper", pool.getCtClass(mt.common.mybatis.mapper.BaseMapper.class.getName()));
////			ctClass.setGenericSignature("Ljava/lang/Object;L" + mt.common.mybatis.mapper.BaseMapper.class.getName().replace(".", "/") + "<L" + Client.class.getName().replace(".", "/") + ";>;");
////			// 将类搜索路径插入到搜索路径之前
////			pool.appendClassPath(new ClassClassPath(ctClass.toClass()));
//////			// 将类搜索路径添加到搜索路径之后
////		} catch (Exception e) {
////			e.printStackTrace();
////		}
////		GenericBeanDefinition definition = new GenericBeanDefinition();
////		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definition, "testMapper");
////
////		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
//
//		ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
//		scanner.setAnnotationClass(Repository.class);
//		scanner.setMarkerInterface(BaseMapper.class);
//		scanner.registerFilters();
//		scanner.doScan("mt.spring.mos.server.controller.testconfig");
//	}
//}
