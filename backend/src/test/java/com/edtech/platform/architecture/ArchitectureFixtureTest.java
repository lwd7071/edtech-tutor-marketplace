package com.edtech.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArchitectureFixtureTest {

    @Test
    void test_no_cross_module_repository_access_fails_when_violated() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.edtech.platform.architecture.fixtures");

        AssertionError error = assertThrows(AssertionError.class, () -> {
            classes().should(ArchitectureTest.NOT_DEPEND_ON_OTHER_MODULES_REPOSITORIES).check(classes);
        });

        assertTrue(error.getMessage().contains("depends on cross-module repository"));
    }

    @Test
    void test_no_cross_module_entity_access_fails_when_violated() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.edtech.platform.architecture.fixtures");

        AssertionError error = assertThrows(AssertionError.class, () -> {
            classes().should(ArchitectureTest.NOT_DEPEND_ON_OTHER_MODULES_ENTITIES).check(classes);
        });

        assertTrue(error.getMessage().contains("depends on cross-module domain/entity"));
    }

    @Test
    void test_no_jdbctemplate_outside_infrastructure_fails_when_violated() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.edtech.platform.architecture.fixtures");

        AssertionError error = assertThrows(AssertionError.class, () -> {
            classes().should(ArchitectureTest.NOT_USE_JDBCTEMPLATE_OUTSIDE_INFRASTRUCTURE).check(classes);
        });

        assertTrue(error.getMessage().contains("uses JdbcTemplate directly"));
    }
}
