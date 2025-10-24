package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.config.FileStorageProperties;
import com.QhomeBase.datadocsservice.dto.FileUploadResponse;
import com.QhomeBase.datadocsservice.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;
    private final FileStorageProperties fileStorageProperties;
    
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg", 
            "image/png",
            "image/webp",
            "image/gif"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getLocation())
                .toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage location initialized at: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public FileUploadResponse uploadImage(
            MultipartFile file, 
            UUID tenantId, 
            UUID uploadedBy,
            String category) {
        
        validateImageFile(file);
        
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;
        
        String datePath = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Path targetLocation = this.fileStorageLocation
                .resolve(category)
                .resolve(tenantId.toString())
                .resolve(datePath)
                .resolve(fileName);
        
        try {
            Files.createDirectories(targetLocation.getParent());
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            String fileUrl = String.format("%s/%s/%s/%s/%s",
                    fileStorageProperties.getBaseUrl(),
                    category,
                    tenantId,
                    datePath,
                    fileName);
            
            log.info("File uploaded successfully: {} by user: {}", fileName, uploadedBy);
            
            return FileUploadResponse.success(
                    UUID.randomUUID(),
                    fileName,
                    originalFileName,
                    fileUrl,
                    file.getContentType(),
                    file.getSize(),
                    uploadedBy
            );
            
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String category, String tenantId, String date, String fileName) {
        try {
            Path filePath = this.fileStorageLocation
                    .resolve(category)
                    .resolve(tenantId)
                    .resolve(date)
                    .resolve(fileName)
                    .normalize();
            
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new com.QhomeBase.datadocsservice.exception.FileNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new com.QhomeBase.datadocsservice.exception.FileNotFoundException("File not found: " + fileName, ex);
        }
    }

    public void deleteFile(String category, String tenantId, String date, String fileName) {
        try {
            Path filePath = this.fileStorageLocation
                    .resolve(category)
                    .resolve(tenantId)
                    .resolve(date)
                    .resolve(fileName)
                    .normalize();
            
            Files.deleteIfExists(filePath);
            log.info("File deleted successfully: {}", fileName);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", fileName, ex);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file.");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException(
                    "Invalid file type. Only JPEG, PNG, WEBP, and GIF images are allowed.");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence: " + fileName);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
}

