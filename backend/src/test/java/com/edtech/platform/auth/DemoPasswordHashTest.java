package com.edtech.platform.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoPasswordHashTest {

    @Test
    void documentedDemoPasswordHashIsValid() {
        String hash = "$2a$10$FGibSl9jLNRhHJEfwhti1ODOUUnYt9F8b97Ax0h.yxjpfo/Ke1Mwa";
        assertTrue(new BCryptPasswordEncoder().matches("Password123!", hash));
    }
}
