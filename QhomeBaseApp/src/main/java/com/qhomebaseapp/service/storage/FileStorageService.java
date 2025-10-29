package com.qhomebaseapp.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileStorageService {

    private final String uploadDir = "uploads/posts/";

    public List<String> uploadMultiple(List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        if (files == null || files.isEmpty()) return urls;

        Files.createDirectories(Path.of(uploadDir));

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Path.of(uploadDir + fileName);
            file.transferTo(filePath);

            String fileType = Files.probeContentType(filePath);
            log.info("Uploaded file: {} (type={})", fileName, fileType);

            urls.add("/" + uploadDir + fileName);
        }
        return urls;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        Files.createDirectories(Path.of(uploadDir));

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Path.of(uploadDir + fileName);
        file.transferTo(filePath);

        return "/" + uploadDir + fileName;
    }
}
