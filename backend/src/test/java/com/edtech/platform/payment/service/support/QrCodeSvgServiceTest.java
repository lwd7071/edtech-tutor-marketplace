package com.edtech.platform.payment.service.support;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeSvgServiceTest {

    private final QrCodeSvgService service = new QrCodeSvgService();

    @Test
    void returnsNullForNullOrBlankContent() {
        assertThat(service.generateSvgDataUri(null, 200)).isNull();
        assertThat(service.generateSvgDataUri("", 200)).isNull();
        assertThat(service.generateSvgDataUri("   ", 200)).isNull();
    }

    @Test
    void generatesValidSvgDataUriForPayload() {
        String payload = "00020101021238540010A0000007270124000697040701140000000000530370454061000005802VN";
        String dataUri = service.generateSvgDataUri(payload, 240);

        assertThat(dataUri).isNotNull();
        assertThat(dataUri).startsWith("data:image/svg+xml;base64,");

        String base64Part = dataUri.substring("data:image/svg+xml;base64,".length());
        String decodedSvg = new String(Base64.getDecoder().decode(base64Part), StandardCharsets.UTF_8);

        assertThat(decodedSvg).contains("<svg xmlns='http://www.w3.org/2000/svg'");
        assertThat(decodedSvg).contains("shape-rendering='crispEdges'");
        assertThat(decodedSvg).contains("<rect width='100%' height='100%' fill='#ffffff'/>");
        assertThat(decodedSvg).contains("<path fill='#171714'");
    }
}
