package com.edtech.platform.architecture;

import com.edtech.platform.auth.controller.AuthController;
import com.edtech.platform.catalog.controller.TeacherPricingPackageController;
import com.edtech.platform.teacher.controller.TeacherDocumentController;
import com.edtech.platform.teacher.controller.TeacherSubjectController;
import com.edtech.platform.finance.controller.TeacherBankAccountController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestStatusContractTest {
    @Test
    void authCreateEndpointsUse201() {
        assertStatus(AuthController.class, "register", HttpStatus.CREATED);
        assertStatus(AuthController.class, "completeOAuthRegistration", HttpStatus.CREATED);
    }

    @Test
    void packageCreateUses201AndDeletesUse204() {
        assertStatus(TeacherPricingPackageController.class, "createPackage", HttpStatus.CREATED);
        assertStatus(TeacherDocumentController.class, "deleteDocument", HttpStatus.NO_CONTENT);
        assertStatus(TeacherSubjectController.class, "unassignSubject", HttpStatus.NO_CONTENT);
        assertStatus(TeacherBankAccountController.class, "delete", HttpStatus.NO_CONTENT);
    }

    private static void assertStatus(Class<?> type, String methodName, HttpStatus expected) {
        Method method = java.util.Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);
        assertEquals(expected, status == null ? HttpStatus.OK : status.value(),
                type.getSimpleName() + "#" + methodName);
    }
}
