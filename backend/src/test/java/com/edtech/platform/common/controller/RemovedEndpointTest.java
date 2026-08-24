package com.edtech.platform.common.controller;

import org.junit.jupiter.api.Test;
public class RemovedEndpointTest {

    @Test
    void fixDbControllerShouldNotExist() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> Class.forName("com.edtech.platform.common.controller.FixDbController"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
