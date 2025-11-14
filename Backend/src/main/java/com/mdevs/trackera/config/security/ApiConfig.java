package com.mdevs.trackera.config.security;

import com.mdevs.trackera.shared.ApiScanner;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Configuration
@Slf4j
public class ApiConfig {
    private Map<Class<? extends Annotation>, Map<String, String>> customAnnotatedApis;

    private final ApiScanner apiScanner;

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    private static final String BASE_CONTROLLER_PACKAGE = "com.mdevs.trackera.controller";

    public ApiConfig(ApiScanner apiScanner) {
        this.apiScanner = apiScanner;
    }

    @PostConstruct
    public void init() {
        try {
            customAnnotatedApis = Collections.unmodifiableMap(apiScanner.scanCustomAnnotatedApis(BASE_CONTROLLER_PACKAGE, Set.of(PublicAPI.class)));
            log.info("Scanned {} public APIs", customAnnotatedApis.getOrDefault(PublicAPI.class, Map.of()).size());
        } catch (Exception e) {
            log.error("Error scanning custom annotated APIs: ", e);
            this.customAnnotatedApis = Map.of();
        }
    }

    public boolean hasAnnotations(String requestPath, String method, Set<Class<? extends Annotation>> annotationsClasses) {
        return annotationsClasses.stream().anyMatch(annotationClass -> {
            Map<String, String> apis = customAnnotatedApis.getOrDefault(annotationClass, Map.of());
            return apis.entrySet().stream().anyMatch(entry -> {
                String pattern = entry.getKey();
                if (ANT_PATH_MATCHER.match(pattern, requestPath)) {
                    String httpMethod = entry.getValue();
                    return httpMethod.equals(method) || httpMethod.equals("ALL");
                }
                return false;
            });
        });
    }
}
