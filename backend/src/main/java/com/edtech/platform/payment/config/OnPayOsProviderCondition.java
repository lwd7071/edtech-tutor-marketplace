package com.edtech.platform.payment.config;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class OnPayOsProviderCondition extends AnyNestedCondition {

    public OnPayOsProviderCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(name = "app.payment.provider", havingValue = "payos")
    static class OnPayos {}

    @ConditionalOnProperty(name = "app.payment.provider", havingValue = "enabled")
    static class OnEnabled {}
}
