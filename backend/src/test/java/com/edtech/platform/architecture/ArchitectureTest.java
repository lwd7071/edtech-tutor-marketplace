package com.edtech.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.edtech.platform", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    static final ArchCondition<JavaClass> NOT_DEPEND_ON_OTHER_MODULES_REPOSITORIES =
            new ArchCondition<JavaClass>("not depend on repositories of other modules") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    String moduleName = getModuleName(item);
                    if (moduleName == null) return;

                    item.getDirectDependenciesFromSelf().forEach(dependency -> {
                        JavaClass targetClass = dependency.getTargetClass();
                        String targetModule = getModuleName(targetClass);
                        if (targetModule != null && !moduleName.equals(targetModule)) {
                            if (targetClass.getPackageName().contains(".repository")) {
                                String message = String.format("%s depends on cross-module repository %s",
                                        item.getName(), targetClass.getName());
                                events.add(SimpleConditionEvent.violated(item, message));
                            }
                        }
                    });
                }
            };

    static final ArchCondition<JavaClass> NOT_DEPEND_ON_OTHER_MODULES_ENTITIES =
            new ArchCondition<JavaClass>("not depend on entities of other modules") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    String moduleName = getModuleName(item);
                    if (moduleName == null) return;

                    item.getDirectDependenciesFromSelf().forEach(dependency -> {
                        JavaClass targetClass = dependency.getTargetClass();
                        String targetModule = getModuleName(targetClass);
                        if (targetModule != null && !moduleName.equals(targetModule)) {
                            if (targetClass.isAnnotatedWith("jakarta.persistence.Entity") ||
                                targetClass.isAnnotatedWith("javax.persistence.Entity") ||
                                targetClass.getPackageName().contains(".domain")) {
                                String message = String.format("%s depends on cross-module domain/entity %s",
                                        item.getName(), targetClass.getName());
                                events.add(SimpleConditionEvent.violated(item, message));
                            }
                        }
                    });
                }
            };

    private static String getModuleName(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (packageName.startsWith("com.edtech.platform.architecture.fixtures.")) {
            String[] parts = packageName.split("\\.");
            if (parts.length > 5) {
                return parts[5];
            }
        } else if (packageName.startsWith("com.edtech.platform.")) {
            String[] parts = packageName.split("\\.");
            if (parts.length > 3) {
                return parts[3];
            }
        }
        return null;
    }

    static final ArchCondition<JavaClass> NOT_USE_JDBCTEMPLATE_OUTSIDE_INFRASTRUCTURE =
            new ArchCondition<JavaClass>("not use JdbcTemplate outside infrastructure") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    item.getDirectDependenciesFromSelf().forEach(dependency -> {
                        JavaClass targetClass = dependency.getTargetClass();
                        if (targetClass.getName().equals("org.springframework.jdbc.core.JdbcTemplate") ||
                            targetClass.getName().equals("org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate")) {
                            // Currently we just flag any usage of JdbcTemplate as a violation to map them.
                            // Realistically, it should only be used in specific repository implementations.
                            if (!item.getName().endsWith("RepositoryImpl") && !item.getName().endsWith("Repository")) {
                                String message = String.format("%s uses JdbcTemplate directly", item.getName());
                                events.add(SimpleConditionEvent.violated(item, message));
                            }
                        }
                    });
                }
            };

    @ArchTest
    static final ArchRule no_cross_module_repository_access = 
            classes().should(NOT_DEPEND_ON_OTHER_MODULES_REPOSITORIES);

    @ArchTest
    static final ArchRule no_cross_module_entity_access = 
            classes().should(NOT_DEPEND_ON_OTHER_MODULES_ENTITIES);

    @ArchTest
    static final ArchRule no_jdbctemplate_outside_infrastructure = 
            classes().should(NOT_USE_JDBCTEMPLATE_OUTSIDE_INFRASTRUCTURE);
}
