package com.edtech.platform.common.storage;

import com.cloudinary.Cloudinary;
import com.edtech.platform.common.config.properties.CloudinaryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(properties.url());
    }
}
