package mt.common.starter.message;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.CommonProperties;
import mt.common.starter.message.messagehandler.MessageHandler;
import mt.common.starter.message.utils.MessageUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
@Slf4j
@Configuration
@ConditionalOnBean(MessageHandler.class)
public class MessageConfiguration {
	
	@Bean
	public MessageUtils messageUtils(CommonProperties commonProperties, Map<String, MessageHandler> messageHandlers) {
		return new MessageUtils(commonProperties, messageHandlers);
	}
	
	@Bean
	@ConditionalOnMissingBean(MessageAspectAdapter.class)
	public DefaultMessageAspect defaultMessageAspect() {
		return new DefaultMessageAspect();
	}
}