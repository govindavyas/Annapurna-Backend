package com.localfood.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MediaProperties mediaProperties;

    public WebConfig(MediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + mediaProperties.uploadDir() + "/";
        registry.addResourceHandler(mediaProperties.publicBasePath() + "/**")
            .addResourceLocations(location);
    }
}
