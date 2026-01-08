package com.mdevs.trackera.shared;

import com.mdevs.trackera.shared.enums.WorkLogColumn;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class FileHandler {

    private static final Tika tika = new Tika();

    private static final List<String> EXCEL_MIME_TYPES = List.of(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final List<String> CSV_MIME_TYPES = List.of(
            "text/csv",
            "application/csv"
    );

    private static final List<String> ALLOWED_MIME_TYPES = Stream.concat(
            EXCEL_MIME_TYPES.stream(),
            CSV_MIME_TYPES.stream()
    ).toList();

    private static final int MAX_WORKLOG_ROWS = 300;

    private static final String TIME_CELL_REGEX = "\\d{1,2}:\\d{2}\\s*[AaPp][Mm]";

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    public static List<Map<WorkLogColumn, String>> validateAndParse(MultipartFile workLogFile) {
        String mimeType;
        try {
            mimeType = tika.detect(workLogFile.getInputStream(), workLogFile.getOriginalFilename());
        } catch (Exception e) {
            log.error("Could not detect mime type for file {}", workLogFile.getOriginalFilename(), e);
            throw new IllegalArgumentException("Error uploading file: " + workLogFile.getOriginalFilename());
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            log.error("Invalid MIME type detected for file {}, Detected mime type: {}", workLogFile.getOriginalFilename(), mimeType);
            throw new BusinessException("The uploaded file type is not supported. Allowed types are excel and csv files only.");
        }
        return parseWorklogFile(workLogFile, mimeType);
    }

    public static List<Map<WorkLogColumn, String>> parseWorklogFile(MultipartFile workLogFile, String mimeType) {
        try (InputStream inputStream = workLogFile.getInputStream()) {
            String fileName = workLogFile.getOriginalFilename();
            if (StringUtils.isEmpty(fileName)) {
                throw new IllegalArgumentException("File name is missing.");
            }
            List<Map<WorkLogColumn, String>> parsedData = EXCEL_MIME_TYPES.contains(mimeType) ? parseExcel(inputStream) : parseCsv(inputStream);
            return parsedData.stream().filter(row -> !isRowEmpty(row)).toList();
        } catch (Exception ex) {
            log.error("Error parsing worklog file", ex);
            throw new RuntimeException("Failed to parse worklog file", ex);
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
                columns.put(WorkLogColumn.TASK_NAME, getCellValueAsString(row.getCell(WorkLogColumn.TASK_NAME.getIndex())));
                columns.put(WorkLogColumn.FROM_HOUR, getCellValueAsString(row.getCell(WorkLogColumn.FROM_HOUR.getIndex())));
                columns.put(WorkLogColumn.TO_HOUR, getCellValueAsString(row.getCell(WorkLogColumn.TO_HOUR.getIndex())));
                columns.put(WorkLogColumn.DURATION, getCellValueAsString(row.getCell(WorkLogColumn.DURATION.getIndex())));
                columns.put(WorkLogColumn.DESCRIPTION, getCellValueAsString(row.getCell(WorkLogColumn.DESCRIPTION.getIndex())));

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
        return DATA_FORMATTER.formatCellValue(cell);
    }

    private static List<Map<WorkLogColumn, String>> parseCsv(InputStream inputStream) throws IOException {
        List<Map<WorkLogColumn, String>> rows = new ArrayList<>();
        int parsedRows = 0;

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            CSVParser records = CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).get().parse(reader);
            for (CSVRecord record : records) {
                Map<WorkLogColumn, String> columns = new HashMap<>();
                columns.put(WorkLogColumn.ROW_NUMBER, String.valueOf(record.getRecordNumber()));
                columns.put(WorkLogColumn.TASK_NAME, getAndNormalizeCsvCell(record, WorkLogColumn.TASK_NAME.getIndex()));
                columns.put(WorkLogColumn.FROM_HOUR, getAndNormalizeCsvCell(record, WorkLogColumn.FROM_HOUR.getIndex()));
                columns.put(WorkLogColumn.TO_HOUR, getAndNormalizeCsvCell(record, WorkLogColumn.TO_HOUR.getIndex()));
                columns.put(WorkLogColumn.DURATION, getAndNormalizeCsvCell(record, WorkLogColumn.DURATION.getIndex()));
                columns.put(WorkLogColumn.DESCRIPTION, getAndNormalizeCsvCell(record, WorkLogColumn.DESCRIPTION.getIndex()));

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
        if (trimmed.matches(TIME_CELL_REGEX)) {
            String[] parts = trimmed.split(":");
            String hour = parts[0];
            String minuteAndAmPm = parts[1];
            if (hour.length() == 1) {
                return String.format("0%s:%s", hour, minuteAndAmPm);
            }
        }

        // Prevent CSV Injection
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            trimmed = "'" + trimmed;
        }

        return trimmed;
    }

    private static void validateFileRowsLimit(int parsedRows) {
        if (parsedRows > MAX_WORKLOG_ROWS) {
            throw new BusinessException("The uploaded file contains more than the allowed limit of " + MAX_WORKLOG_ROWS + " rows.");
        }
    }
}
