package com.edtech.platform.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentView {
    private UUID id;
    private String secureUrl;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
}
