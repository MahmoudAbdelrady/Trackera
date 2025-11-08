package com.mdevs.trackera.shared;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ApiScanner {
    private record EndpointInfo(String[] paths, String httpMethod) {
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
                processMethod(controllerClass, method, classPaths, annotationsClasses, processedApis);
            }
        }
        return processedApis;
    }

    private Set<BeanDefinition> scanForControllers(String packageName) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return provider.findCandidateComponents(packageName);
    }

    private void processMethod(Class<?> controllerClass, Method method, String[] classPaths, Set<Class<? extends Annotation>> annotationsClasses, Map<Class<? extends Annotation>, Map<String, String>> processedApis) {
        EndpointInfo endpointInfo = extractEndpointInfo(method);
        if (endpointInfo == null) {
            return;
        }

        for (Class<? extends Annotation> annotationClass : annotationsClasses) {
            if (controllerClass.isAnnotationPresent(annotationClass) || method.isAnnotationPresent(annotationClass)) {
                registerEndpoint(classPaths, endpointInfo, processedApis, annotationClass);
            }
        }
    }

    private EndpointInfo extractEndpointInfo(Method method) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return new EndpointInfo(getMapping.value(), "GET");
        }

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            return new EndpointInfo(postMapping.value(), "POST");
        }

        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            return new EndpointInfo(putMapping.value(), "PUT");
        }

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return new EndpointInfo(deleteMapping.value(), "DELETE");
        }

        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            return new EndpointInfo(patchMapping.value(), "PATCH");
        }

        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            String httpMethod = requestMapping.method().length > 0 ? requestMapping.method()[0].name() : "ALL";
            return new EndpointInfo(requestMapping.value(), httpMethod);
        }

        return null;
    }

    private void registerEndpoint(String[] classPaths, EndpointInfo endpointInfo, Map<Class<? extends Annotation>, Map<String, String>> processedApis, Class<? extends Annotation> annotationClass) {
        Map<String, String> apisMap = processedApis.get(annotationClass);
        String[] methodPaths = endpointInfo.paths;
        for (String classPath : classPaths) {
            for (String methodPath : methodPaths) {
                String fullPath = ("/trackera/" + classPath + "/" + methodPath).replaceAll("//+", "/");
                apisMap.put(fullPath, endpointInfo.httpMethod);
            }
        }
    }
}
