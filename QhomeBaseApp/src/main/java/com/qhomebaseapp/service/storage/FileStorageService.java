package com.qhomebaseapp.service.storage;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileStorageService {

    private final String uploadDir = "uploads/register/";

    public List<String> uploadMultiple(List<MultipartFile> files) throws Exception {
        List<String> urls = new ArrayList<>();
        Files.createDirectories(Path.of(uploadDir));

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String fileName = "vehicle_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Path.of(uploadDir + fileName);
            file.transferTo(filePath);
            urls.add("/" + uploadDir + fileName);
        }
        return urls;
    }
}
