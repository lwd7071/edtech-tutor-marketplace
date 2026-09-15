package com.edtech.platform.common.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/** Port for private, authenticated credential proofs. */
public interface CredentialProofStorage {
    StoredFile upload(MultipartFile file, String folder) throws IOException;
    void delete(String publicId) throws IOException;
    String signedUrl(String publicId);
    DownloadedFile download(String publicId) throws IOException;
    record StoredFile(String publicId, String secureUrl, String mimeType, long fileSize) {}
    record DownloadedFile(byte[] bytes, String mimeType) {}
}
