package com.banny.lotsonote.config;

import com.banny.lotsonote.filter.TraceIdFilter;
import com.banny.lotsonote.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:D:/coding/Java/project/lotsonote/backend/upload}")
    private String uploadPath;

    @Autowired
    private TokenInterceptor tokenInterceptor;
    // 静态资源映射
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }

    /**
     * 添加拦截器，用于验证 token，初始化请求周期中的用户相关信息
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/error");
    }

    // CORS 统一由 SecurityConfig.corsConfigurationSource() 管理，此处注释掉避免配置冲突
    // Security 过滤器链优先级高于 MVC，两处同时配置时以 SecurityConfig 为准
    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
    //     registry.addMapping("/**")
    //             .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
    //             .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
    //             .allowedHeaders("*")
    //             .allowCredentials(true)
    //             .maxAge(3600);
    // }

    // TraceId过滤器，注册TraceIdFilter，对所有请求注入链路追踪ID，方便日志排查
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceIdFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
