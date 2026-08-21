package com.edtech.platform.learning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlock {
    private String type; // TEXT, IMAGE, FILE, LINK
    private String content; // for TEXT
    private UUID attachmentId; // for IMAGE, FILE
    private String url; // for LINK
    private String label; // for LINK
}
