package com.edtech.platform.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThymeleafRouteRegistryTest {
    @Test
    void recordsAllLegacyRoutesAndTheAdditionalBookingRequestRoute() {
        var routes = ThymeleafRouteRegistry.routes();
        var parityRoutes = routes.stream().filter(ThymeleafRouteRegistry.Route::legacyParityRoute).toList();

        assertEquals(70, parityRoutes.size());
        assertEquals(71, routes.stream().filter(route -> route.state() != ThymeleafRouteRegistry.State.DEVELOPMENT_ONLY).count());
        assertEquals(10, count(parityRoutes, ThymeleafRouteRegistry.Role.PUBLIC));
        assertEquals(5, count(parityRoutes, ThymeleafRouteRegistry.Role.GUEST));
        assertEquals(2, count(parityRoutes, ThymeleafRouteRegistry.Role.AUTH_FLOW));
        assertEquals(15, count(parityRoutes, ThymeleafRouteRegistry.Role.STUDENT));
        assertEquals(25, count(parityRoutes, ThymeleafRouteRegistry.Role.TEACHER));
        assertEquals(12, count(parityRoutes, ThymeleafRouteRegistry.Role.ADMIN));
        assertEquals(1, count(parityRoutes, ThymeleafRouteRegistry.Role.ANY));
        assertTrue(routes.stream().anyMatch(route -> route.pathPattern().equals("/teacher/booking-requests")
                && route.expectedRole() == ThymeleafRouteRegistry.Role.TEACHER && !route.legacyParityRoute()));
        assertTrue(routes.stream().anyMatch(route -> route.pathPattern().equals("/sandbox/components")
                && route.state() == ThymeleafRouteRegistry.State.DEVELOPMENT_ONLY));
    }

    @Test
    void routePatternsAreUniqueAndClassified() {
        var routes = ThymeleafRouteRegistry.routes();
        var paths = routes.stream().map(ThymeleafRouteRegistry.Route::pathPattern).toList();
        var states = routes.stream().collect(Collectors.groupingBy(
                ThymeleafRouteRegistry.Route::state, Collectors.counting()));

        assertEquals(paths.size(), new HashSet<>(paths).size());
        assertFalse(routes.stream().anyMatch(route -> route.pathPattern().isBlank() || !route.pathPattern().startsWith("/")));
        assertEquals(Map.of(
                ThymeleafRouteRegistry.State.LIVE, 70L,
                ThymeleafRouteRegistry.State.COMPATIBILITY_REDIRECT, 1L,
                ThymeleafRouteRegistry.State.DEVELOPMENT_ONLY, 1L), states);
    }

    @Test
    void everyNonDevelopmentRouteHasASpringMvcHandler() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        var mappings = new InspectableRequestMappingHandlerMapping();
        mappings.setPatternParser(new PathPatternParser());
        Set<String> mappedPaths = new HashSet<>();

        try (var context = new GenericApplicationContext()) {
            context.refresh();
            mappings.setApplicationContext(context);
            mappings.afterPropertiesSet();
            for (var component : scanner.findCandidateComponents("com.edtech.platform")) {
                Class<?> controller = Class.forName(component.getBeanClassName());
                for (Method method : controller.getDeclaredMethods()) {
                    var mapping = mappings.mappingFor(method, controller);
                    if (mapping != null) mappedPaths.addAll(mapping.getPatternValues());
                }
            }
        }

        var missing = ThymeleafRouteRegistry.routes().stream()
                .filter(route -> route.state() != ThymeleafRouteRegistry.State.DEVELOPMENT_ONLY)
                .map(ThymeleafRouteRegistry.Route::pathPattern)
                .filter(path -> !mappedPaths.contains(path))
                .toList();
        assertTrue(missing.isEmpty(), () -> "No Spring MVC handler registered for: " + missing);
    }

    private static final class InspectableRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
        private org.springframework.web.servlet.mvc.method.RequestMappingInfo mappingFor(Method method, Class<?> controller) {
            return getMappingForMethod(method, controller);
        }
    }

    private long count(java.util.List<ThymeleafRouteRegistry.Route> routes, ThymeleafRouteRegistry.Role role) {
        return routes.stream().filter(route -> route.expectedRole() == role).count();
    }
}
