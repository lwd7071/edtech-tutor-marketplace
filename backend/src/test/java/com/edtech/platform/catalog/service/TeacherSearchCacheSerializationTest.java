package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.dto.TeacherCard;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherSearchCacheSerializationTest {

    @Test
    void cachedPageRoundTripsThroughItsRedisSerializer() {
        var card = new TeacherCard(
                UUID.randomUUID(), "Nguyễn Văn Toán", null, "Dạy Toán", 5,
                true, true, false, List.of(), 500_000L, 4.5, 4.4, 10, 1);
        var page = CachedTeacherSearchPage.from(
                new PageImpl<>(List.of(card), PageRequest.of(0, 20), 1));
        var serializer = TeacherSearchCacheConfiguration.serializer();

        Object restored = serializer.deserialize(serializer.serialize(page));

        assertThat(restored).isEqualTo(page);
    }
}
