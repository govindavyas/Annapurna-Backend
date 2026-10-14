package com.localfood.backend.menu;

import com.localfood.backend.common.ApiException;
import com.localfood.backend.config.MediaProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final MediaProperties mediaProperties;

    public MediaService(MediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
    }

    public String saveMenuImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException("Image file is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ApiException("Unsupported image type. Allowed: jpeg, png, webp");
        }

        String ext = extensionFor(file.getOriginalFilename());
        String targetName = UUID.randomUUID() + ext;
        Path root = Path.of(mediaProperties.uploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(root);
            Path target = root.resolve(targetName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException("Failed to store image");
        }

        return mediaProperties.publicBasePath() + "/" + targetName;
    }

    private String extensionFor(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return ".jpg";
        }
        String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        return switch (ext) {
            case ".png", ".jpg", ".jpeg", ".webp" -> ext;
            default -> ".jpg";
        };
    }
}
