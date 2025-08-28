package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.worklog.NewWorkLogDTO;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.shared.FileHandler;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.utils.TrackeraDateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WorkLogService {
    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final static Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)h)?\\s*(?:(\\d+)m)?");

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
        List<WorkLogDetail> allWorkLogDetails = new ArrayList<>();
        List<Map<String, Object>> rowErrors = new ArrayList<>();
        double totalTime = 0;
        try {
            List<List<String>> parsedData = processWorkLogFile(newWorkLogDTO, worklogFile);
            if (parsedData.size() > MAX_WORKLOG_ROWS) {
                throw new BusinessException("The uploaded file contains more than the allowed limit of " + MAX_WORKLOG_ROWS + " rows.");
            }
            int emptyRowCnt = 0;
            for (int i = 1; i < parsedData.size(); i++) {
                List<String> row = parsedData.get(i);
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
                    taskName = getCellValue(row.get(0), 0, String.class);
                    fromHour = getCellValue(row.get(1), 1, LocalTime.class);
                    toHour = getCellValue(row.get(2), 2, LocalTime.class);
                    taskLogDuration = parseDuration(getCellValue(row.get(3), 3, String.class));
                    taskDescription = getCellValue(row.get(4), 4, String.class);
                } catch (Exception e) {
                    LOGGER.error("Error parsing row {}", i + 1, e);
                    Map<String, Object> error = new HashMap<>();
                    error.put("row", i + 1);
                    error.put("error", e.getMessage());
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

    private List<List<String>> processWorkLogFile(NewWorkLogDTO newWorkLogDTO, MultipartFile worklogFile) {
        if (workLogRepository.existsByWorkDate(newWorkLogDTO.getLogDate())) {
            throw new BusinessException("WorkLog for the date " + newWorkLogDTO.getLogDate() + " already exists.");
        }
        return FileHandler.validateAndParse(worklogFile);
    }

    private boolean isRowEmpty(List<String> row) {
        return row == null || row.isEmpty() || row.stream().allMatch(StringUtils::isEmpty);
    }

    private <T> T getCellValue(String cell, int idx, Class<T> expectedType) {
        if (cell == null) {
            throw new IllegalArgumentException("Cell is empty");
        }

        try {
            Object result;
            if (StringUtils.isEmpty(cell)) {
                throw new IllegalArgumentException("Cell value at index [" + idx + "] is empty");
            }
            if (expectedType == String.class) {
                result = cell;
            } else if (expectedType == LocalTime.class) {
                result = LocalTime.parse(cell, TrackeraDateUtil.getTime12HourFormatter());
            } else {
                throw new IllegalArgumentException("Cell value type at index [" + idx + "] is not supported");
            }

            return expectedType.cast(result);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid '" + cell + "' value in cell at index [" + idx + "]", e);
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
                String formattedDate = TrackeraDateUtil.getDateCompactFormatter().format(newWorkLogDTO.getLogDate());
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
