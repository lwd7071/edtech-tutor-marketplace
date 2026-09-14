package com.edtech.platform.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthIdentitySerializationTest {

    private final GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

    @Test
    @DisplayName("Cycle 3: OAuthIdentity round-trip serialization and deserialization preserves all fields and @class")
    void oAuthIdentity_roundTripSerialization_success() {
        OAuthIdentity original = new OAuthIdentity("GOOGLE", "google-sub-789", "student@test.com", "Test Student");

        byte[] serialized = serializer.serialize(original);
        assertThat(serialized).isNotEmpty();

        String json = new String(serialized);
        assertThat(json).contains("\"@class\":\"com.edtech.platform.auth.service.OAuthIdentity\"");
        assertThat(json).contains("\"email\":\"student@test.com\"");
        assertThat(json).contains("\"provider\":\"GOOGLE\"");

        Object deserialized = serializer.deserialize(serialized);
        assertThat(deserialized).isInstanceOf(OAuthIdentity.class);

        OAuthIdentity result = (OAuthIdentity) deserialized;
        assertThat(result.provider()).isEqualTo("GOOGLE");
        assertThat(result.subject()).isEqualTo("google-sub-789");
        assertThat(result.email()).isEqualTo("student@test.com");
        assertThat(result.fullName()).isEqualTo("Test Student");
    }

    @Test
    @DisplayName("Cycle 3: OAuthIdentity with empty/null fullName also serializes and deserializes safely")
    void oAuthIdentity_emptyFullName_roundTrip_success() {
        OAuthIdentity original = new OAuthIdentity("GOOGLE", "google-sub-000", "anonymous@test.com", "");

        byte[] serialized = serializer.serialize(original);
        Object deserialized = serializer.deserialize(serialized);

        assertThat(deserialized).isInstanceOf(OAuthIdentity.class);
        OAuthIdentity result = (OAuthIdentity) deserialized;
        assertThat(result.fullName()).isEqualTo("");
    }
}
