package com.example.DACN.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloudinaryService Tests")
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    @DisplayName("Should upload image successfully")
    void testUploadImageSuccess() throws IOException {
        // Given
        String expectedUrl = "https://res.cloudinary.com/test/image/upload/v123/user_avatars/test.jpg";
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", expectedUrl);

        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getSize()).thenReturn(1024L * 1024L); // 1MB
        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenReturn(new byte[1024]);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        // When
        String result = cloudinaryService.uploadImage(file);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    @DisplayName("Should throw exception when file is empty")
    void testUploadImageEmptyFile() throws IOException {
        // Given
        when(file.isEmpty()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> cloudinaryService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File is empty");

        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    @DisplayName("Should throw exception when file size exceeds limit")
    void testUploadImageFileSizeExceeded() throws IOException {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6L * 1024L * 1024L); // 6MB

        // When & Then
        assertThatThrownBy(() -> cloudinaryService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File size exceeds maximum limit of 5MB");

        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    @DisplayName("Should throw exception for invalid file format")
    void testUploadImageInvalidFormat() throws IOException {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L * 1024L);
        when(file.getOriginalFilename()).thenReturn("test.pdf");

        // When & Then
        assertThatThrownBy(() -> cloudinaryService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid file format. Allowed formats: jpg, jpeg, png, gif, webp");

        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    @DisplayName("Should delete image successfully")
    void testDeleteImageSuccess() throws Exception {
        // Given
        String imageUrl = "https://res.cloudinary.com/test/image/upload/v123/user_avatars/test.jpg";

        // When
        cloudinaryService.deleteImage(imageUrl);

        // Then
        verify(uploader).destroy(anyString(), anyMap());
    }

    @Test
    @DisplayName("Should handle null image URL gracefully")
    void testDeleteImageNullUrl() throws Exception {
        // When
        cloudinaryService.deleteImage(null);

        // Then
        verify(uploader, never()).destroy(anyString(), anyMap());
    }
}
