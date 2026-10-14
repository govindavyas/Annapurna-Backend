package com.localfood.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
	FeatureProperties.class,
	AppSecurityProperties.class,
	OrderProperties.class,
	MediaProperties.class,
	CustomerAuthProperties.class,
	CorsProperties.class,
	PushProperties.class
})
public class AppConfig {
}
