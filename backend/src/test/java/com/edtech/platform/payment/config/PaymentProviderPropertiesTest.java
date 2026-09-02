package com.edtech.platform.payment.config;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class PaymentProviderPropertiesTest {
    @Test void disabledNeedsNoCredentialsButPayosFailsFastWithoutAllThree() {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        assertThatCode(properties::validate).doesNotThrowAnyException();
        properties.setProvider("payos");
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setClientId("client"); properties.setApiKey("api"); properties.setChecksumKey("checksum");
        assertThatCode(properties::validate).doesNotThrowAnyException();
    }
}
