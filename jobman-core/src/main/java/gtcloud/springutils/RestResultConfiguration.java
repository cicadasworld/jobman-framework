package gtcloud.springutils;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RestResultConfiguration implements WebMvcConfigurer {
	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		RestResultHttpMessageConverter converter = new RestResultHttpMessageConverter();
		converters.add(0, converter);
	}
}
