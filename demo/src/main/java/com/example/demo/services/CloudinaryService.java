package com.example.demo.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    @Value("${cloudinary.folder:project-cnpm/products}")
    private String cloudinaryFolder;

    public String uploadProductImage(MultipartFile file, String productId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (!StringUtils.hasText(cloudinaryUrl)) {
            throw new IllegalStateException("Cloudinary is not configured. Please set CLOUDINARY_URL");
        }

        try {
            Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
            cloudinary.config.secure = true;

            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", cloudinaryFolder + "/" + productId,
                            "resource_type", "image",
                            "overwrite", false,
                            "unique_filename", true
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl != null) {
                return secureUrl.toString();
            }

            Object url = uploadResult.get("url");
            if (url != null) {
                return url.toString();
            }

            throw new IllegalStateException("Cloudinary upload succeeded but no URL was returned");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to upload image to Cloudinary", ex);
        }
    }
}
