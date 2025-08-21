package com.mdevs.trackera.config.security;

import com.mdevs.trackera.shared.annotations.PublicAPI;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Configuration
public class ApiConfig {
    private Map<Class<? extends Annotation>, Map<String, String>> CUSTOM_ANNOTATED_APIS;

    @PostConstruct
    public void init() {
        CUSTOM_ANNOTATED_APIS = Collections.unmodifiableMap(scanCustomAnnotatedApis("com.mdevs.trackera.controller", Set.of(PublicAPI.class)));
    }

    public Map<String, String> getAnnotationApis(Class<? extends Annotation> annotationClass) {
        return CUSTOM_ANNOTATED_APIS.get(annotationClass);
    }

    public boolean hasAnnotations(String requestPath, String method, Set<Class<? extends Annotation>> annotationsClasses) {
        return annotationsClasses.stream().anyMatch(annotationClass -> {
            String requestPathValue = String.valueOf(getAnnotationApis(annotationClass).get(requestPath));
            return requestPathValue.equals(method) || requestPathValue.equals("ALL");
        });
    }

    public Map<Class<? extends Annotation>, Map<String, String>> scanCustomAnnotatedApis(String packageName, Set<Class<? extends Annotation>> annotationsClasses) {
        Map<Class<? extends Annotation>, Map<String, String>> processedApis = new HashMap<>();
        annotationsClasses.forEach(annotation -> processedApis.put(annotation, new HashMap<>()));

        Set<BeanDefinition> controllerBeans = scanForControllers(packageName);
        for (BeanDefinition controllerBean : controllerBeans) {
            Class<?> controllerClass;
            try {
                controllerClass = Class.forName(controllerBean.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
            String[] classPaths = requestMapping != null ? requestMapping.value() : new String[]{};
            for (Method method : controllerClass.getMethods()) {
                String[] methodPaths = new String[]{};
                GetMapping methodGetMapping = method.getAnnotation(GetMapping.class);
                PostMapping methodPostMapping = method.getAnnotation(PostMapping.class);
                PutMapping methodPutMapping = method.getAnnotation(PutMapping.class);
                DeleteMapping methodDeleteMapping = method.getAnnotation(DeleteMapping.class);
                String httpMethod = "ALL";
                if (methodGetMapping != null) {
                    methodPaths = methodGetMapping.value();
                    httpMethod = "GET";
                } else if (methodPostMapping != null) {
                    methodPaths = methodPostMapping.value();
                    httpMethod = "POST";
                } else if (methodPutMapping != null) {
                    methodPaths = methodPutMapping.value();
                    httpMethod = "PUT";
                } else if (methodDeleteMapping != null) {
                    methodPaths = methodDeleteMapping.value();
                    httpMethod = "DELETE";
                }

                for (Class<? extends Annotation> annotationClass : annotationsClasses) {
                    if (controllerClass.isAnnotationPresent(annotationClass) || method.isAnnotationPresent(annotationClass)) {
                        Map<String, String> apisMap = processedApis.get(annotationClass);
                        for (String classPath : classPaths) {
                            for (String methodPath : methodPaths) {
                                String fullPath = ("/trackera/" + classPath + "/" + methodPath).replaceAll("//+", "/");
                                apisMap.put(fullPath, httpMethod);
                            }
                        }
                    }
                }
            }
        }
        return processedApis;
    }

    private Set<BeanDefinition> scanForControllers(String packageName) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return provider.findCandidateComponents(packageName);
    }
}
