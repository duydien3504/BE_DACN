package com.example.DACN.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_FORMATS = { "jpg", "jpeg", "png", "gif", "webp" };

    public String uploadImage(MultipartFile file) throws IOException {
        log.info("Uploading image to Cloudinary: {}", file.getOriginalFilename());

        // Validate file
        validateFile(file);

        // Upload to Cloudinary
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "user_avatars",
                        "resource_type", "image"));

        String imageUrl = (String) uploadResult.get("secure_url");
        log.info("Image uploaded successfully: {}", imageUrl);

        return imageUrl;
    }

    public List<String> uploadProductImages(List<MultipartFile> files) throws IOException {
        log.info("Uploading {} product images to Cloudinary", files.size());

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            // Validate file
            validateProductImage(file);

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "product_images",
                            "resource_type", "image"));

            String imageUrl = (String) uploadResult.get("secure_url");
            imageUrls.add(imageUrl);
            log.info("Product image uploaded successfully: {}", imageUrl);
        }

        log.info("All {} product images uploaded successfully", imageUrls.size());
        return imageUrls;
    }

    public void deleteImage(String imageUrl) {
        try {
            if (imageUrl != null && imageUrl.contains("cloudinary")) {
                // Extract public_id from URL
                String publicId = extractPublicId(imageUrl);
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted image from Cloudinary: {}", publicId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        boolean isValidFormat = false;
        for (String format : ALLOWED_FORMATS) {
            if (format.equals(extension)) {
                isValidFormat = true;
                break;
            }
        }

        if (!isValidFormat) {
            throw new IllegalArgumentException("Invalid file format. Allowed formats: jpg, jpeg, png, gif, webp");
        }
    }

    private void validateProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
            throw new IllegalArgumentException("Invalid file format. Only JPG and PNG are allowed for product images");
        }
    }

    private String extractPublicId(String imageUrl) {
        // Extract public_id from Cloudinary URL
        // Example:
        // https://res.cloudinary.com/cloud_name/image/upload/v123456/user_avatars/image_id.jpg
        String[] parts = imageUrl.split("/");
        if (parts.length >= 2) {
            String lastPart = parts[parts.length - 1];
            String secondLastPart = parts[parts.length - 2];
            return secondLastPart + "/" + lastPart.substring(0, lastPart.lastIndexOf("."));
        }
        return "";
    }
}
