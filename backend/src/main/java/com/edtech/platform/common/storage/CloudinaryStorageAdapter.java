package com.edtech.platform.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageAdapter implements FileStoragePort {

    private final Cloudinary cloudinary;

    @Override
    public UploadResult upload(MultipartFile file, String folder) throws IOException {
        log.info("Uploading file to Cloudinary in folder: {}", folder);
        
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), 
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto"
                ));

        String publicId = (String) uploadResult.get("public_id");
        String secureUrl = (String) uploadResult.get("secure_url");
        String mimeType = file.getContentType();
        long fileSize = file.getSize();

        return new UploadResult(publicId, secureUrl, mimeType, fileSize);
    }

    @Override
    public void delete(String publicId) throws IOException {
        log.info("Deleting file from Cloudinary: {}", publicId);
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
