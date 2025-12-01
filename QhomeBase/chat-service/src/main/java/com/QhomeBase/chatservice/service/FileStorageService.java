package com.QhomeBase.chatservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${chat.upload.directory:uploads/chat}")
    private String uploadDirectory;

    @Value("${chat.cdn.base-url:}")
    private String cdnBaseUrl;

    /**
     * Upload image for chat message
     */
    public String uploadImage(MultipartFile file, UUID groupId) throws IOException {
        log.info("Uploading image for group: {}", groupId);
        
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, groupId.toString());
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + ".jpg";
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = getImageUrl(groupId.toString(), fileName);
        log.info("Uploaded image: {}", imageUrl);
        return imageUrl;
    }

    /**
     * Upload audio file for chat message
     */
    public String uploadAudio(MultipartFile file, UUID groupId) throws IOException {
        log.info("Uploading audio for group: {}", groupId);
        
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, groupId.toString(), "audio");
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + ".m4a"; // Default to m4a for voice messages
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String audioUrl = getAudioUrl(groupId.toString(), fileName);
        log.info("Uploaded audio: {}", audioUrl);
        return audioUrl;
    }

    /**
     * Upload file for chat message
     */
    public String uploadFile(MultipartFile file, UUID groupId) throws IOException {
        log.info("Uploading file for group: {}", groupId);
        
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, groupId.toString(), "files");
        Files.createDirectories(uploadPath);

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = getFileUrl(groupId.toString(), fileName);
        log.info("Uploaded file: {}", fileUrl);
        return fileUrl;
    }

    /**
     * Delete image file
     */
    public void deleteImage(UUID groupId, String fileName) {
        try {
            Path filePath = Paths.get(uploadDirectory, groupId.toString(), fileName);
            Files.deleteIfExists(filePath);
            log.info("Deleted image: {}", filePath);
        } catch (IOException e) {
            log.error("Error deleting image: {}", fileName, e);
        }
    }

    /**
     * Delete file
     */
    public void deleteFile(UUID groupId, String fileName) {
        try {
            Path filePath = Paths.get(uploadDirectory, groupId.toString(), "files", fileName);
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.error("Error deleting file: {}", fileName, e);
        }
    }

    private String getImageUrl(String groupId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return cdnBaseUrl + "/chat/" + groupId + "/" + fileName;
        }
        return "/uploads/chat/" + groupId + "/" + fileName;
    }

    private String getAudioUrl(String groupId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return cdnBaseUrl + "/chat/" + groupId + "/audio/" + fileName;
        }
        return "/uploads/chat/" + groupId + "/audio/" + fileName;
    }

    private String getFileUrl(String groupId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return cdnBaseUrl + "/chat/" + groupId + "/files/" + fileName;
        }
        return "/uploads/chat/" + groupId + "/files/" + fileName;
    }
}

