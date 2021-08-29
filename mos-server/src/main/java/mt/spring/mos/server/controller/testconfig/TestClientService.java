//package mt.spring.mos.server.controller.testconfig;
//
//import javassist.ClassClassPath;
//import javassist.ClassPool;
//import javassist.CtClass;
//import mt.common.mybatis.mapper.BaseMapper;
//import mt.common.service.BaseServiceImpl;
//import mt.common.utils.SpringUtils;
//import mt.spring.mos.server.entity.po.Client;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
//import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
//import org.springframework.beans.factory.support.AbstractBeanDefinition;
//import org.springframework.beans.factory.support.DefaultListableBeanFactory;
//import org.springframework.beans.factory.support.GenericBeanDefinition;
//import org.springframework.core.env.Environment;
//import org.springframework.stereotype.Component;
//import tk.mybatis.mapper.entity.Config;
//import tk.mybatis.mapper.mapperhelper.MapperHelper;
//import tk.mybatis.spring.mapper.MapperFactoryBean;
//import tk.mybatis.spring.mapper.SpringBootBindUtil;
//
//import java.util.Map;
//
///**
// * @Author Martin
// * @Date 2021/8/28
// */
//@Component
//public class TestClientService extends BaseServiceImpl<Client> implements BeanFactoryPostProcessor {
//	protected final Log logger = LogFactory.getLog(getClass());
//	private MapperHelper mapperHelper = new MapperHelper();
//	private boolean addToConfig = true;
//	@Autowired
//	private Environment environment;
//
//	@Override
//	public BaseMapper<Client> getBaseMapper() {
//		Map<String, BaseMapper> mappers = SpringUtils.getBeansOfType(BaseMapper.class);
//		for (Map.Entry<String, BaseMapper> stringBaseMapperEntry : mappers.entrySet()) {
////			stringBaseMapperEntry.getValue().getClass().getin
//		}
//		return null;
//	}
//
//	@Override
//	public void postProcessBeanFactory(@NotNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
//		setMapperProperties(environment);
//
//		ClassPool pool = ClassPool.getDefault();
//		CtClass ctClass = null;
//		try {
//			ctClass = pool.makeInterface("mt.spring.mos.server.controller.testconfig.TestMapper", pool.getCtClass(mt.common.mybatis.mapper.BaseMapper.class.getName()));
//			ctClass.setGenericSignature("Ljava/lang/Object;L" + mt.common.mybatis.mapper.BaseMapper.class.getName().replace(".", "/") + "<L" + Client.class.getName().replace(".", "/") + ";>;");
//			// 将类搜索路径插入到搜索路径之前
//			pool.appendClassPath(new ClassClassPath(ctClass.toClass()));
////			// 将类搜索路径添加到搜索路径之后
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		//转换为子类，因为父类没有添加beanDefintion对象的api
//		DefaultListableBeanFactory defaultbf = (DefaultListableBeanFactory) beanFactory;
//
//		GenericBeanDefinition definition = new GenericBeanDefinition();
//
//		// the mapper interface is the original class of the bean
//		// but, the actual class of the bean is MapperFactoryBean
//		definition.getConstructorArgumentValues().addGenericArgumentValue("mt.spring.mos.server.controller.testconfig.TestMapper");
//		definition.setBeanClass(MapperFactoryBean.class);
//		//设置通用 Mapper
//		//不做任何配置的时候使用默认方式
//		if (this.mapperHelper == null) {
//			this.mapperHelper = new MapperHelper();
//		}
//		definition.getPropertyValues().add("mapperHelper", this.mapperHelper);
//		definition.getPropertyValues().add("addToConfig", this.addToConfig);
//		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
//		defaultbf.registerBeanDefinition("testMapper", definition);
//	}
//
//	public void setMapperProperties(Environment environment) {
//		Config config = SpringBootBindUtil.bind(environment, Config.class, Config.PREFIX);
//		if (mapperHelper == null) {
//			mapperHelper = new MapperHelper();
//		}
//		if (config != null) {
//			mapperHelper.setConfig(config);
//		}
//	}
//}
