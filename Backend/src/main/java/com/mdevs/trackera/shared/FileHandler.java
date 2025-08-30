package com.mdevs.trackera.shared;

import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.utils.TrackeraDateUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
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

    private final static int MAX_WORKLOG_ROWS = 300;

    public static List<List<String>> validateAndParse(MultipartFile workLogFile) {
        String mimeType;
        try {
            mimeType = tika.detect(workLogFile.getInputStream(), workLogFile.getOriginalFilename());
        } catch (Exception e) {
            LOGGER.error("Could not detect mime type for file {}", workLogFile.getOriginalFilename(), e);
            throw new IllegalArgumentException("Error uploading file: " + workLogFile.getOriginalFilename());
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            LOGGER.error("Invalid MIME type detected for file {}, Detected mime type: {}", workLogFile.getOriginalFilename(), mimeType);
            throw new IllegalArgumentException("The uploaded file type is not supported. Allowed types are excel and csv files only.");
        }
        return parseWorklogFile(workLogFile);
    }

    public static List<List<String>> parseWorklogFile(MultipartFile workLogFile) {
        try (InputStream inputStream = workLogFile.getInputStream()) {
            String fileName = workLogFile.getOriginalFilename();
            if (StringUtils.isEmpty(fileName)) {
                throw new IllegalArgumentException("File name is missing.");
            }
            List<List<String>> parsedData = fileName.toLowerCase().endsWith(".xlsx") ? parseExcel(inputStream) : parseCsv(inputStream);
            return parsedData.stream().filter(row -> !isRowEmpty(row)).toList();
        } catch (Exception ex) {
            LOGGER.error("Error parsing worklog file", ex);
            throw new RuntimeException(ex.getMessage());
        }
    }

    private static List<List<String>> parseExcel(InputStream inputStream) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        int parsedRows = 0;

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<String> columns = new ArrayList<>();
                for (Cell cell : row) {
                    columns.add(getCellValueAsString(cell));
                }
                rows.add(columns);
                parsedRows++;
                validateFileRowsLimit(parsedRows);
            }
        }
        return rows;
    }

    private static boolean isRowEmpty(List<String> row) {
        return row == null || row.isEmpty() || row.stream().allMatch(StringUtils::isEmpty);
    }

    private static String getCellValueAsString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC ->
                    DateUtil.isCellDateFormatted(cell) ? TrackeraDateUtil.getSimple12hFormat().format(cell.getDateCellValue()) : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private static List<List<String>> parseCsv(InputStream inputStream) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        int parsedRows = 0;

        try (Reader reader = new InputStreamReader(inputStream)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);
            for (CSVRecord record : records) {
                List<String> columns = new ArrayList<>();
                record.forEach(r -> columns.add(normalizeCsvCell(r)));
                rows.add(columns);
                parsedRows++;
                validateFileRowsLimit(parsedRows);
            }
        }
        return rows;
    }

    private static String normalizeCsvCell(String cell) {
        if (StringUtils.isEmpty(cell)) {
            return cell;
        }

        String trimmed = cell.trim();
        if (trimmed.matches("\\d{1,2}:\\d{2}\\s*[AaPp][Mm]")) {
            String[] parts = trimmed.split(":");
            String hour = parts[0];
            String minuteAndAmPm = parts[1];
            if (hour.length() == 1) {
                return String.format("0%s:%s", hour, minuteAndAmPm);
            }
        }

        return cell;
    }

    private static void validateFileRowsLimit(int parsedRows) {
        if (parsedRows > MAX_WORKLOG_ROWS) {
            throw new BusinessException("The uploaded file contains more than the allowed limit of " + MAX_WORKLOG_ROWS + " rows.");
        }
    }
}
