package com.edtech.platform.common.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStoragePort {
    UploadResult upload(MultipartFile file, String folder) throws IOException;
    void delete(String publicId) throws IOException;

    record UploadResult(String publicId, String secureUrl, String mimeType, long fileSize) {}
}
