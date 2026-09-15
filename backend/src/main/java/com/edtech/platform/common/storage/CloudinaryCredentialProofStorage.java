package com.edtech.platform.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class CloudinaryCredentialProofStorage implements CredentialProofStorage {
    private final Cloudinary cloudinary;
    @Override public StoredFile upload(MultipartFile file, String folder) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder, "resource_type", "auto", "type", "authenticated"));
        return new StoredFile((String) result.get("public_id"), (String) result.get("secure_url"), file.getContentType(), file.getSize());
    }
    @Override public void delete(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("type", "authenticated", "resource_type", "auto"));
    }
    @Override public String signedUrl(String publicId) {
        return cloudinary.url().secure(true).resourceType("auto").type("authenticated").signed(true).generate(publicId);
    }
    @Override public DownloadedFile download(String publicId) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(signedUrl(publicId)).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        if (connection.getResponseCode() / 100 != 2) throw new IOException("credential proof download failed");
        try (var input = connection.getInputStream()) {
            return new DownloadedFile(input.readAllBytes(), connection.getContentType());
        } finally { connection.disconnect(); }
    }
}
