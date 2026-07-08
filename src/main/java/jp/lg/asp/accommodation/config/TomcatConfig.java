package jp.lg.asp.accommodation.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat設定カスタマイズ
 * Tomcat 10.1.42のmultipartファイル数制限(fileCountMax)を引き上げる
 */
@Configuration
public class TomcatConfig {

	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
		return factory -> {
			factory.addConnectorCustomizers(connector -> {
				connector.setProperty("maxParameterCount", "10000");
				connector.setProperty("maxPostSize", "10485760");
			});
			factory.addContextCustomizers(context -> {
				// Tomcat 10.1.42 の multipart file count 制限を無効化（-1 = 無制限）
				context.setAllowCasualMultipartParsing(true);
			});
		};
	}
}
