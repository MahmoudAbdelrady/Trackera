package com.mdevs.trackera.shared;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class FileValidator {

    private final static Tika tika = new Tika();

    private final static List<String> ALLOWED_MIME_TYPES = List.of(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "application/csv",
            "application/vnd.google-apps.spreadsheet"
    );

    private final static Logger LOGGER = LoggerFactory.getLogger(FileValidator.class);

    private final static long FILE_MAX_SIZE = 5L * 1024 * 1024; // 5 MB

    public static void validate(MultipartFile file) {
        String mimeType;
        try {
            mimeType = tika.detect(file.getInputStream());
        } catch (Exception e) {
            LOGGER.error("Could not detect mime type for file {}", file.getOriginalFilename(), e);
            throw new IllegalArgumentException("Error uploading file: " + file.getOriginalFilename());
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            LOGGER.error("Invalid MIME type detected for file {}, Detected mime type: {}", file.getOriginalFilename(), mimeType);
            throw new IllegalArgumentException("The uploaded file type is not supported. Allowed types are excel and csv files only.");
        }

        if (file.getSize() > FILE_MAX_SIZE) {
            throw new IllegalArgumentException("File size exceeds the allowed limit of " + (FILE_MAX_SIZE / 1024 / 1024) + " MB");
        }
    }
}
