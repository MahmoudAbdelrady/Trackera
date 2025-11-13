package com.mdevs.trackera.shared;

import com.mdevs.trackera.shared.enums.WorkLogColumn;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static List<Map<WorkLogColumn, String>> validateAndParse(MultipartFile workLogFile) {
        String mimeType;
        try {
            mimeType = tika.detect(workLogFile.getInputStream(), workLogFile.getOriginalFilename());
        } catch (Exception e) {
            LOGGER.error("Could not detect mime type for file {}", workLogFile.getOriginalFilename(), e);
            throw new IllegalArgumentException("Error uploading file: " + workLogFile.getOriginalFilename());
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            LOGGER.error("Invalid MIME type detected for file {}, Detected mime type: {}", workLogFile.getOriginalFilename(), mimeType);
            throw new BusinessException("The uploaded file type is not supported. Allowed types are excel and csv files only.");
        }
        return parseWorklogFile(workLogFile);
    }

    public static List<Map<WorkLogColumn, String>> parseWorklogFile(MultipartFile workLogFile) {
        try (InputStream inputStream = workLogFile.getInputStream()) {
            String fileName = workLogFile.getOriginalFilename();
            if (StringUtils.isEmpty(fileName)) {
                throw new IllegalArgumentException("File name is missing.");
            }
            List<Map<WorkLogColumn, String>> parsedData = fileName.toLowerCase().endsWith(".xlsx") ? parseExcel(inputStream) : parseCsv(inputStream);
            return parsedData.stream().filter(row -> !isRowEmpty(row)).toList();
        } catch (Exception ex) {
            LOGGER.error("Error parsing worklog file", ex);
            throw new RuntimeException(ex.getMessage());
        }
    }

    private static List<Map<WorkLogColumn, String>> parseExcel(InputStream inputStream) throws IOException {
        List<Map<WorkLogColumn, String>> rows = new ArrayList<>();
        int parsedRows = 0;

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<WorkLogColumn, String> columns = new HashMap<>();
                columns.put(WorkLogColumn.ROW_NUMBER, String.valueOf(row.getRowNum() + 1));
                columns.put(WorkLogColumn.TASK_NAME, getCellValueAsString(row.getCell(0)));
                columns.put(WorkLogColumn.FROM_HOUR, getCellValueAsString(row.getCell(1)));
                columns.put(WorkLogColumn.TO_HOUR, getCellValueAsString(row.getCell(2)));
                columns.put(WorkLogColumn.DURATION, getCellValueAsString(row.getCell(3)));
                columns.put(WorkLogColumn.DESCRIPTION, getCellValueAsString(row.getCell(4)));

                rows.add(columns);
                parsedRows++;
                validateFileRowsLimit(parsedRows);
            }
        }
        return rows;
    }

    private static boolean isRowEmpty(Map<WorkLogColumn, String> row) {
        return row == null || row.isEmpty() || row.entrySet().stream().filter(col -> !col.getKey().equals(WorkLogColumn.ROW_NUMBER)).allMatch(col -> StringUtils.isEmpty(col.getValue()));
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC ->
                    DateUtil.isCellDateFormatted(cell) ? DurationFormatter.getSimple12hFormat().format(cell.getDateCellValue()) : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private static List<Map<WorkLogColumn, String>> parseCsv(InputStream inputStream) throws IOException {
        List<Map<WorkLogColumn, String>> rows = new ArrayList<>();
        int parsedRows = 0;

        try (Reader reader = new InputStreamReader(inputStream)) {
            CSVParser records = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).get().parse(reader);
            for (CSVRecord record : records) {
                Map<WorkLogColumn, String> columns = new HashMap<>();
                columns.put(WorkLogColumn.ROW_NUMBER, String.valueOf(record.getRecordNumber()));
                columns.put(WorkLogColumn.TASK_NAME, getAndNormalizeCsvCell(record, 0));
                columns.put(WorkLogColumn.FROM_HOUR, getAndNormalizeCsvCell(record, 1));
                columns.put(WorkLogColumn.TO_HOUR, getAndNormalizeCsvCell(record, 2));
                columns.put(WorkLogColumn.DURATION, getAndNormalizeCsvCell(record, 3));
                columns.put(WorkLogColumn.DESCRIPTION, getAndNormalizeCsvCell(record, 4));

                rows.add(columns);
                parsedRows++;
                validateFileRowsLimit(parsedRows);
            }
        }
        return rows;
    }

    private static String getAndNormalizeCsvCell(CSVRecord record, int index) {
        if (index >= record.size() || StringUtils.isEmpty(record.get(index))) {
            return "";
        }

        String trimmed = record.get(index).trim();
        // Parsing from/to time columns in HH:MM format
        if (trimmed.matches("\\d{1,2}:\\d{2}\\s*[AaPp][Mm]")) {
            String[] parts = trimmed.split(":");
            String hour = parts[0];
            String minuteAndAmPm = parts[1];
            if (hour.length() == 1) {
                return String.format("0%s:%s", hour, minuteAndAmPm);
            }
        }
        return trimmed;
    }

    private static void validateFileRowsLimit(int parsedRows) {
        if (parsedRows > MAX_WORKLOG_ROWS) {
            throw new BusinessException("The uploaded file contains more than the allowed limit of " + MAX_WORKLOG_ROWS + " rows.");
        }
    }
}
