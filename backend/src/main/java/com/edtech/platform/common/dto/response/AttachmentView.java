package com.edtech.platform.common.dto.response;

import lombok.Value;

import java.util.UUID;

@Value
public class AttachmentView {
    private UUID id;
    private String secureUrl;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
}
