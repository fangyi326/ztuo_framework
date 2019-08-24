package cn.ztuo.bitrade.config;

import cn.ztuo.bitrade.ext.OrdinalToEnumConverterFactory;
import cn.ztuo.bitrade.interceptor.OpenApiInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @description: ApplicationConfig
 * @author: MrGao
 * @create: 2019/05/06 16:32
 */
@Configuration
public class ApplicationConfig extends WebMvcConfigurerAdapter {

    @Bean
    public FilterRegistrationBean corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }


    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new OrdinalToEnumConverterFactory());
        super.addFormatters(registry);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new OpenApiInterceptor())
                .addPathPatterns("/open/**", "/user/**")
                .excludePathPatterns("/open/**");
        super.addInterceptors(registry);
    }

//    /**
//     * 国际化
//     *
//     * @return
//     */
//    @Bean(name = "messageSource")
//    public ResourceBundleMessageSource getMessageSource() {
//        ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
//        resourceBundleMessageSource.setDefaultEncoding("UTF-8");
//        resourceBundleMessageSource.setBasenames("i18n/messages", "i18n/ValidationMessages");
//        resourceBundleMessageSource.setCacheSeconds(3600);
//        return resourceBundleMessageSource;
//    }

}
