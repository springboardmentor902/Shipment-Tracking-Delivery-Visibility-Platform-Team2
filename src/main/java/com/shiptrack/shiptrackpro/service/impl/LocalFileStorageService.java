package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Stores POD files locally. The path is configurable and ignored by Git. */
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDirectory;

    public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        try {
            this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the POD upload directory", exception);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String safeExtension = extensionOf(originalName);
        String storedName = UUID.randomUUID() + safeExtension;
        Path destination = uploadDirectory.resolve(storedName).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            return "/api/pod/files/" + storedName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store the uploaded proof file");
        }
    }

    @Override
    public Resource load(String storedFileName) {
        if (storedFileName == null || storedFileName.contains("..")
                || storedFileName.contains("/") || storedFileName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        try {
            Path file = uploadDirectory.resolve(storedFileName).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proof file not found");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proof file not found");
        }
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int extensionStart = originalName.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == originalName.length() - 1) {
            return "";
        }
        String extension = originalName.substring(extensionStart).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }
}
