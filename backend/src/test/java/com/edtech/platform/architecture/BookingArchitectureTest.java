package com.edtech.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.edtech.platform", importOptions = ImportOption.DoNotIncludeTests.class)
public class BookingArchitectureTest {

    @ArchTest
    static final ArchRule booking_should_not_depend_on_other_modules_entities =
            noClasses().that().resideInAPackage("..booking..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..auth.domain..",
                            "..teacher.domain..",
                            "..enrollment.domain..",
                            "..finance.domain..",
                            "..communication.domain.."
                    );

    @ArchTest
    static final ArchRule booking_should_not_depend_on_other_modules_repositories =
            noClasses().that().resideInAPackage("..booking..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..auth.repository..",
                            "..teacher.repository..",
                            "..enrollment.repository..",
                            "..finance.repository..",
                            "..communication.repository.."
                    );
}