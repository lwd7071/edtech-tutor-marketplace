package com.edtech.platform.payment.gateway;

import com.edtech.platform.payment.config.PaymentProviderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;
import vn.payos.exception.NotFoundException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.WebhookData;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;
import vn.payos.service.blocking.webhooks.WebhooksService;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOsPaymentGatewayTest {

    @Mock
    private PayOS payOS;

    private PayOsPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        gateway = new PayOsPaymentGateway(payOS, properties);
    }

    @Test
    void createPaymentLink_shouldMapCorrectly_andReturnResult() throws Exception {
        long orderCode = 12345L;
        long amount = 50000L;
        String desc = "Thanh toan khoa hoc";
        URI returnUrl = URI.create("https://example.com/return");
        URI cancelUrl = URI.create("https://example.com/cancel");
        PaymentLinkCommand cmd = new PaymentLinkCommand(orderCode, amount, desc, returnUrl, cancelUrl);

        CreatePaymentLinkResponse mockResponse = CreatePaymentLinkResponse.builder()
                .bin("bin")
                .accountNumber("accNum")
                .accountName("accName")
                .amount(amount)
                .description(desc)
                .orderCode(orderCode)
                .currency("VND")
                .paymentLinkId("paymentLinkId")
                .status(PaymentLinkStatus.PENDING)
                .checkoutUrl("https://checkout.payos.vn")
                .qrCode("qrCodeString")
                .build();

        PaymentRequestsService paymentRequestsService = mock(PaymentRequestsService.class);
        when(payOS.paymentRequests()).thenReturn(paymentRequestsService);
        when(paymentRequestsService.create(any(CreatePaymentLinkRequest.class))).thenReturn(mockResponse);

        PaymentLinkResult result = gateway.createPaymentLink(cmd);

        ArgumentCaptor<CreatePaymentLinkRequest> captor = ArgumentCaptor.forClass(CreatePaymentLinkRequest.class);
        verify(paymentRequestsService).create(captor.capture());
        CreatePaymentLinkRequest sentData = captor.getValue();

        assertThat(sentData.getOrderCode()).isEqualTo(orderCode);
        assertThat(sentData.getAmount()).isEqualTo(amount);
        assertThat(result.orderCode()).isEqualTo(orderCode);
        assertThat(result.paymentLinkId()).isEqualTo("paymentLinkId");
        assertThat(result.checkoutUrl()).isEqualTo("https://checkout.payos.vn");
    }

    @Test
    void findPaymentLink_shouldReturnStatus_whenFound() throws Exception {
        long orderCode = 9999L;
        PaymentLink mockData = PaymentLink.builder()
                .orderCode(orderCode)
                .id("link-123")
                .amount(500000L)
                .amountPaid(500000L)
                .amountRemaining(0L)
                .status(PaymentLinkStatus.PAID)
                .createdAt("2026-09-04 12:00:00")
                .transactions(Collections.emptyList())
                .build();

        PaymentRequestsService paymentRequestsService = mock(PaymentRequestsService.class);
        when(payOS.paymentRequests()).thenReturn(paymentRequestsService);
        when(paymentRequestsService.get(orderCode)).thenReturn(mockData);

        Optional<com.edtech.platform.payment.gateway.PaymentLinkStatus> statusOpt = gateway.findPaymentLink(orderCode);

        assertThat(statusOpt).isPresent();
        assertThat(statusOpt.get().orderCode()).isEqualTo(orderCode);
        assertThat(statusOpt.get().paymentLinkId()).isEqualTo("link-123");
        assertThat(statusOpt.get().checkoutUrl()).isEqualTo("https://checkout.payos.vn/web/link-123");
    }

    @Test
    void findPaymentLink_shouldReturnEmpty_whenNotFound() throws Exception {
        long orderCode = 8888L;
        PaymentRequestsService paymentRequestsService = mock(PaymentRequestsService.class);
        when(payOS.paymentRequests()).thenReturn(paymentRequestsService);
        when(paymentRequestsService.get(orderCode)).thenThrow(mock(NotFoundException.class));

        Optional<com.edtech.platform.payment.gateway.PaymentLinkStatus> statusOpt = gateway.findPaymentLink(orderCode);

        assertThat(statusOpt).isEmpty();
    }

    @Test
    void verifyWebhook_shouldReturnVerifiedPayment_whenValid() throws Exception {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("code", "00");

        WebhookData webhookData = WebhookData.builder()
                .orderCode(12345L)
                .amount(500000L)
                .description("Payment description")
                .accountNumber("123456789")
                .reference("REF-123")
                .transactionDateTime("2026-09-04 12:00:00")
                .currency("VND")
                .paymentLinkId("payment-link-123")
                .code("00")
                .desc("success")
                .build();

        WebhooksService webhooksService = mock(WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksService);
        when(webhooksService.verify(any())).thenReturn(webhookData);

        VerifiedPayment verified = gateway.verifyWebhook(json);

        assertThat(verified.orderCode()).isEqualTo(12345L);
        assertThat(verified.amountVnd()).isEqualTo(500000L);
        assertThat(verified.providerReference()).isEqualTo("REF-123");
    }

    @Test
    void verifyWebhook_shouldThrowInvalidSignature_whenInvalid() throws Exception {
        ObjectNode json = new ObjectMapper().createObjectNode();

        WebhooksService webhooksService = mock(WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksService);
        when(webhooksService.verify(any())).thenThrow(new RuntimeException("Invalid checksum signature"));

        assertThatThrownBy(() -> gateway.verifyWebhook(json))
                .isInstanceOf(InvalidPaymentSignatureException.class);
    }
}
