package jp.lg.asp.accommodation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AOP設定クラス
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {

	/**
	 * JSON変換用ObjectMapperのBean定義
	 */
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}