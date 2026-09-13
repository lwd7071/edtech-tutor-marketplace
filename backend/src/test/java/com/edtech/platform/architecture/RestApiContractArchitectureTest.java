package com.edtech.platform.architecture;

import com.edtech.platform.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RestApiContractArchitectureTest {
    @Test
    void everyRestEndpointUsesSharedEnvelopeOrDocumentedNoContentException() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> violations = new ArrayList<>();

        for (var bean : scanner.findCandidateComponents("com.edtech.platform")) {
            Class<?> controller = Class.forName(bean.getBeanClassName(), true, Thread.currentThread().getContextClassLoader());
            for (Method method : controller.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                boolean sharedEnvelope = isDirectApiResponse(method)
                        || isApiResponseEntity(method);
                ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
                boolean noContent = returnType == Void.TYPE && responseStatus != null
                        && responseStatus.code() == HttpStatus.NO_CONTENT;
                boolean documentedException = (controller.getSimpleName().equals("PayOsWebhookController")
                        && method.getName().equals("handleWebhook"))
                        || (controller.getSimpleName().equals("HealthController")
                        && method.getName().equals("health"));
                if (!sharedEnvelope && !noContent && !documentedException) {
                    violations.add(controller.getSimpleName() + "#" + method.getName() + " -> "
                            + method.getGenericReturnType().getTypeName());
                }
            }
        }

        assertThat(violations).as("REST endpoints outside the five-field response contract").isEmpty();
    }

    private boolean isApiResponseEntity(Method method) {
        if (!ResponseEntity.class.isAssignableFrom(method.getReturnType())) return false;
        Type type = method.getGenericReturnType();
        if (!(type instanceof ParameterizedType responseEntity)) return false;
        Type[] args = responseEntity.getActualTypeArguments();
        if (args.length != 1 || !(args[0] instanceof ParameterizedType body)) return false;
        return body.getRawType() == ApiResponse.class;
    }

    private boolean isDirectApiResponse(Method method) {
        if (method.getReturnType() != ApiResponse.class) return false;
        Type type = method.getGenericReturnType();
        return type instanceof ParameterizedType response
                && response.getRawType() == ApiResponse.class
                && response.getActualTypeArguments().length == 1;
    }
}
