package com.edtech.platform.payment.service.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class QrCodeSvgService {

    /**
     * Generates a vector SVG Data URI (base64) for the given content string.
     * Uses crisp `#171714` (var(--ink)) fill on white background.
     *
     * @param content QR payload (e.g. VietQR EMVCo string or payment URL)
     * @param size Nominal size in pixels for the viewBox
     * @return data:image/svg+xml;base64,... or null if content is blank or generation fails
     */
    public String generateSvgDataUri(String content, int size) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            int targetSize = Math.max(120, size);
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M
            );
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, targetSize, targetSize, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();

            StringBuilder svg = new StringBuilder(width * height * 4);
            svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ")
                    .append(width).append(" ").append(height)
                    .append("' shape-rendering='crispEdges'>");
            svg.append("<rect width='100%' height='100%' fill='#ffffff'/>");
            svg.append("<path fill='#171714' d='");
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        svg.append("M").append(x).append(",").append(y).append("h1v1h-1z ");
                    }
                }
            }
            svg.append("'/>");
            svg.append("</svg>");

            String base64 = Base64.getEncoder().encodeToString(svg.toString().getBytes(StandardCharsets.UTF_8));
            return "data:image/svg+xml;base64," + base64;
        } catch (Exception e) {
            return null;
        }
    }
}
