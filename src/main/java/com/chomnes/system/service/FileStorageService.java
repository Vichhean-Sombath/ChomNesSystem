package com.chomnes.system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("csv", "text/csv"), Map.entry("txt", "text/plain"),
            Map.entry("mp4", "video/mp4")
    );
    private final Path root;

    public FileStorageService(@Value("${app.files.storage-dir:./uploads}") String storageDirectory) {
        this.root = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Select a non-empty file to upload.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Files may not exceed 25 MB.");
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new IllegalArgumentException("Invalid uploaded filename.");
        }
        int dot = originalName.lastIndexOf('.');
        String extension = dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String contentType = CONTENT_TYPES.get(extension);
        if (contentType == null) throw new IllegalArgumentException("This file type is not supported.");
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(root);
            Path destination = root.resolve(fileName).normalize();
            if (!destination.startsWith(root)) throw new IllegalArgumentException("Invalid uploaded filename.");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination);
            }
            return new StoredFile(fileName, "/files/" + fileName, contentType, originalName, file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store uploaded file.", exception);
        }
    }

    public StoredFileInfo load(String fileName) {
        if (fileName == null || !fileName.matches("[0-9a-fA-F-]{36}\\.(pdf|png|jpe?g|gif|docx?|pptx?|xlsx?|csv|txt|mp4)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found.");
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        Path path = root.resolve(fileName).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found.");
        }
        return new StoredFileInfo(path, CONTENT_TYPES.get(extension), fileName);
    }

    public void discard(StoredFile file) {
        try {
            Files.deleteIfExists(root.resolve(file.fileName()).normalize());
        } catch (IOException ignored) { }
    }

    public record StoredFile(String fileName, String url, String contentType, String originalName, long size) {}
    public record StoredFileInfo(Path path, String contentType, String fileName) {}
}
