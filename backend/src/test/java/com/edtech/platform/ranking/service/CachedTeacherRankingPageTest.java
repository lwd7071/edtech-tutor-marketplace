package com.edtech.platform.ranking.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CachedTeacherRankingPageTest {
    @Test
    void rankingCachePayloadCanRoundTripThroughConfiguredJsonSerializer() {
        CachedTeacherRankingPage expected = new CachedTeacherRankingPage(
                List.of(new CachedTeacherRankingItem(
                        UUID.randomUUID(), "Nguyễn Minh An", null, "Dạy Toán", new BigDecimal("4.82"), 24, 1)),
                0,
                20,
                1);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        byte[] encoded = serializer.serialize(expected);
        CachedTeacherRankingPage decoded = (CachedTeacherRankingPage) serializer.deserialize(encoded);

        assertEquals(expected, decoded);
        assertEquals("Nguyễn Minh An", decoded.content().getFirst().fullName());
        assertNotNull(decoded.toPage(org.springframework.data.domain.Sort.unsorted()).getContent().getFirst().getTeacherId());
    }
}
