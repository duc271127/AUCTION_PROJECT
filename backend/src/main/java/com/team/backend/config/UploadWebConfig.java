package com.team.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class UploadWebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String imagePath = Path.of(uploadDir, "images")
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        if (!imagePath.endsWith("/")) {
            imagePath = imagePath + "/";
        }

        registry.addResourceHandler("/api/uploads/images/**")
                .addResourceLocations(imagePath);
    }
}