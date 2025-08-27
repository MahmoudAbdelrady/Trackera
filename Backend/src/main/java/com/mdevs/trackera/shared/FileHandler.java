package com.mdevs.trackera.shared;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {

    private final static Tika tika = new Tika();

    private final static List<String> ALLOWED_MIME_TYPES = List.of(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "application/csv",
            "application/vnd.google-apps.spreadsheet"
    );

    private final static Logger LOGGER = LoggerFactory.getLogger(FileHandler.class);

    private final static long FILE_MAX_SIZE = 5L * 1024 * 1024; // 5 MB

    public static void validate(MultipartFile file) {
        String mimeType;
        try {
            mimeType = tika.detect(file.getInputStream(), file.getOriginalFilename());
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

    public static List<List<String>> parseWorklogFile(String filename, InputStream inputStream) {
        try {
            return filename.toLowerCase().endsWith(".xlsx") ? parseExcel(inputStream) : parseCsv(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<List<String>> parseExcel(InputStream inputStream) throws IOException {
        List<List<String>> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                List<String> columns = new ArrayList<>();
                for (Cell cell : row) {
                    columns.add(getCellValueAsString(cell));
                }
                rows.add(columns);
            }
        }
        return rows;
    }

    private static String getCellValueAsString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC ->
                    DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue().toString() : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private static List<List<String>> parseCsv(InputStream inputStream) throws IOException {
        List<List<String>> rows = new ArrayList<>();

        try (Reader reader = new InputStreamReader(inputStream)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);
            for (CSVRecord record : records) {
                List<String> columns = new ArrayList<>();
                record.forEach(columns::add);
                rows.add(columns);
            }
        }
        return rows;
    }
}
