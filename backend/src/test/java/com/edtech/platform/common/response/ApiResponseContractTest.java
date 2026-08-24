package com.edtech.platform.common.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successAlwaysUsesExactlyFiveTopLevelFields() {
        JsonNode json = objectMapper.valueToTree(ApiResponse.ok("Thành công", "payload"));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder("success", "message", "data", "errors", "meta");
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("errors").isNull()).isTrue();
        assertThat(json.get("meta").isNull()).isTrue();
    }

    @Test
    void errorAlwaysUsesExactlyFiveTopLevelFields() {
        ApiErrorDetail detail = new ApiErrorDetail("VALIDATION_ERROR", "startTime", "Không hợp lệ");
        JsonNode json = objectMapper.valueToTree(ApiResponse.error("Dữ liệu đầu vào không hợp lệ", List.of(detail)));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder("success", "message", "data", "errors", "meta");
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("data").isNull()).isTrue();
        assertThat(json.at("/errors/0/code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(json.at("/errors/0/field").asText()).isEqualTo("startTime");
    }

    @Test
    void pagePlacesItemsInDataAndPaginationInMeta() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 3);
        JsonNode json = objectMapper.valueToTree(ApiResponse.page(page.getContent(), PageMeta.from(page)));

        assertThat(json.get("data").isArray()).isTrue();
        assertThat(json.at("/meta/page").asInt()).isZero();
        assertThat(json.at("/meta/totalElements").asLong()).isEqualTo(3);
        assertThat(json.at("/meta/hasNext").asBoolean()).isTrue();
    }

    private Set<String> fieldNames(JsonNode node) {
        java.util.HashSet<String> names = new java.util.HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
