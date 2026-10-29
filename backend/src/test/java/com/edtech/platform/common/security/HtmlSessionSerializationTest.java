package com.edtech.platform.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlSessionSerializationTest {
    @Test
    void securityContextPrincipalCanBeStoredByRedisSessionSerializer() throws Exception {
        var user = new AuthenticatedUser(UUID.randomUUID(), "student@example.test", "STUDENT");
        var context = new SecurityContextImpl(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        byte[] serialized;
        try (var bytes = new ByteArrayOutputStream(); var output = new ObjectOutputStream(bytes)) {
            output.writeObject(context);
            output.flush();
            serialized = bytes.toByteArray();
        }
        SecurityContextImpl restored;
        try (var input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            restored = (SecurityContextImpl) input.readObject();
        }

        var restoredUser = (AuthenticatedUser) restored.getAuthentication().getPrincipal();
        assertEquals(user.id(), restoredUser.id());
        assertEquals(user.role(), restoredUser.role());
    }
}
