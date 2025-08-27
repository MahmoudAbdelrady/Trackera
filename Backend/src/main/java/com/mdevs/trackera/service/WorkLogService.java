package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.worklog.NewWorkLogDTO;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.shared.FileHandler;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WorkLogService {
    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final static SimpleDateFormat PERIOD_TIME_FORMAT = new SimpleDateFormat("HH:mm a");

    private final static Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)h)?\\s*(?:(\\d+)m)?");

    private final static DateTimeFormatter WORKLOG_DETAIL_DURATION_FORMATTER = DateTimeFormatter.ofPattern("HH:mm a");

    private final static DateTimeFormatter WORKLOG_NAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final static int MAX_WORKLOG_FILE_EMPTY_ROWS_COUNT = 10;

    private final static int MAX_WORKLOG_ROWS = 300;

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkLogService.class);

    @Autowired
    public WorkLogService(WorkLogRepository workLogRepository, WorkLogDetailRepository workLogDetailRepository) {
        this.workLogRepository = workLogRepository;
        this.workLogDetailRepository = workLogDetailRepository;
    }

    @Transactional
    public Map<String, Object> addWorkLog(NewWorkLogDTO newWorkLogDTO, MultipartFile worklogFile) {
        validateWorkLog(newWorkLogDTO, worklogFile);
        List<WorkLogDetail> allWorkLogDetails = new ArrayList<>();
        List<Map<String, Object>> rowErrors = new ArrayList<>();
        double totalTime = 0;
        try (InputStream inputStream = worklogFile.getInputStream()) {
            XSSFWorkbook workBook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workBook.getSheetAt(0);
            if ((sheet.getLastRowNum() + 1) > MAX_WORKLOG_ROWS) {
                throw new BusinessException("The uploaded file contains more than the allowed limit of 300 rows.");
            }
            int emptyRowCnt = 0;
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;
                if (isRowEmpty(row)) {
                    emptyRowCnt++;
                    if (emptyRowCnt >= MAX_WORKLOG_FILE_EMPTY_ROWS_COUNT) {
                        break;
                    }
                    continue;
                }
                emptyRowCnt = 0;
                String taskName;
                LocalTime fromHour;
                LocalTime toHour;
                double taskLogDuration;
                String taskDescription;
                try {
                    taskName = getCellValue(row.getCell(0), String.class);
                    fromHour = getCellValue(row.getCell(1), LocalTime.class);
                    toHour = getCellValue(row.getCell(2), LocalTime.class);
                    taskLogDuration = parseDuration(getCellValue(row.getCell(3), String.class));
                    taskDescription = getCellValue(row.getCell(4), String.class);
                } catch (Exception e) {
                    LOGGER.error("Error parsing row {}", row.getRowNum() + 1, e);
                    Map<String, Object> error = new HashMap<>();
                    error.put("row", row.getRowNum() + 1);
                    error.put("error", e.getCause().getMessage());
                    rowErrors.add(error);
                    continue;
                }

                totalTime += taskLogDuration;

                WorkLogDetail workLogDetail = new WorkLogDetail();
                workLogDetail.setTaskName(taskName);
                workLogDetail.setTaskUrl(null); // @TODO --> Should be based on the user's selected project
                workLogDetail.setStartTime(fromHour);
                workLogDetail.setEndTime(toHour);
                workLogDetail.setDuration(taskLogDuration);
                workLogDetail.setDescription(taskDescription);
                workLogDetail.setStatus(WorkLog.Status.NOT_SYNCED);
                allWorkLogDetails.add(workLogDetail);
            }
        } catch (Exception ex) {
            LOGGER.error("Error processing worklog file", ex);
            throw new RuntimeException(ex);
        }

        Map<String, Object> result = new HashMap<>();
        if (!rowErrors.isEmpty()) {
            result.put("isError", true);
            result.put("message", "There were some errors in the uploaded file.");
            result.put("errors", rowErrors);
        } else {
            WorkLog workLog = saveWorkLog(newWorkLogDTO, totalTime / 60.0);
            saveWorkLogDetails(allWorkLogDetails, workLog);
            result.put("message", "Worklog uploaded successfully");
        }
        return result;
    }

    private void validateWorkLog(NewWorkLogDTO newWorkLogDTO, MultipartFile worklogFile) {
        if (workLogRepository.existsByWorkDate(newWorkLogDTO.getLogDate())) {
            throw new BusinessException("WorkLog for the date " + newWorkLogDTO.getLogDate() + " already exists.");
        }
        FileHandler.validate(worklogFile);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().trim().isEmpty()) {
                    return false;
                } else if (cell.getCellType() != CellType.STRING) {
                    return false;
                }
            }
        }
        return true;
    }

    private <T> T getCellValue(Cell cell, Class<T> expectedType) {
        if (cell == null) {
            throw new IllegalArgumentException("Cell is empty");
        }

        int cellIdx = cell.getColumnIndex() + 1;
        try {
            Object result;

            if (expectedType == String.class) {
                String cellValue = cell.getCellType() == CellType.STRING ? cell.getStringCellValue() : cell.toString();
                if (StringUtils.isEmpty(cellValue)) {
                    throw new IllegalArgumentException("Cell value at index [" + cellIdx + "] is empty");
                }
                result = cellValue;
            } else if (expectedType == LocalTime.class) {
                if (cell.getDateCellValue() == null) {
                    throw new IllegalArgumentException("Date/Time cell value at index [" + cellIdx + "] is empty");
                }
                String formatted = PERIOD_TIME_FORMAT.format(cell.getDateCellValue());
                result = LocalTime.parse(formatted, WORKLOG_DETAIL_DURATION_FORMATTER);
            } else {
                throw new IllegalArgumentException("Cell value type at index [" + cellIdx + "] is not supported");
            }

            return expectedType.cast(result);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + expectedType.getSimpleName() + " value in cell at index [" + cellIdx + "]", e);
        }
    }

    private double parseDuration(String duration) {
        Matcher matcher = DURATION_PATTERN.matcher(duration);
        int hours = 0, minutes = 0;

        if (matcher.find()) {
            if (matcher.group(1) != null) {
                hours = Integer.parseInt(matcher.group(1));
            }
            if (matcher.group(2) != null) {
                minutes = Integer.parseInt(matcher.group(2));
            }
        }

        return (hours * 60) + minutes;
    }

    public WorkLog saveWorkLog(NewWorkLogDTO newWorkLogDTO, double totalTime) {
        try {
            WorkLog workLog = new WorkLog();
            workLog.setUser(AppConfig.getCurrentUser());
            workLog.setTotalHours(totalTime);
            workLog.setWorkDate(newWorkLogDTO.getLogDate());
            workLog.setStatus(WorkLog.Status.NOT_SYNCED);
            if (!StringUtils.isEmpty(newWorkLogDTO.getLogName())) {
                workLog.setName(newWorkLogDTO.getLogName());
            } else {
                DayOfWeek dayOfWeek = newWorkLogDTO.getLogDate().getDayOfWeek();
                String dayName = dayOfWeek.name().substring(0, 1).toUpperCase() + dayOfWeek.name().substring(1).toLowerCase();
                String formattedDate = WORKLOG_NAME_DATE_FORMATTER.format(newWorkLogDTO.getLogDate());
                workLog.setName("Worklog - " + dayName + formattedDate);
            }

            return workLogRepository.save(workLog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveWorkLogDetails(List<WorkLogDetail> allWorkLogDetails, WorkLog workLog) {
        try {
            allWorkLogDetails.forEach(logDetail -> logDetail.setWorkLog(workLog));
            workLogDetailRepository.saveAll(allWorkLogDetails);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        StringJoiner joiner = new StringJoiner(" ");
        if (hours > 0) {
            joiner.add(hours + "h");
        }
        if (minutes > 0) {
            joiner.add(minutes + "m");
        }
        return joiner.toString();
    }
}
