package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.worklog.ManageWorkLogDTO;
import com.mdevs.trackera.dto.worklog.WorkLogInfoDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSummaryDTO;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.shared.FileHandler;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.search_filter.SearchFilter;
import com.mdevs.trackera.shared.search_filter.WorkLogSearchFilterBuilder;
import com.mdevs.trackera.shared.utils.TrackeraDateUtil;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WorkLogService {
    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final ModelMapper modelMapper;

    private final static Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)h)?\\s*(?:(\\d+)m)?");

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkLogService.class);

    @Autowired
    public WorkLogService(WorkLogRepository workLogRepository, WorkLogDetailRepository workLogDetailRepository, ModelMapper modelMapper) {
        this.workLogRepository = workLogRepository;
        this.workLogDetailRepository = workLogDetailRepository;
        this.modelMapper = modelMapper;
    }

    public Page<WorkLogInfoDTO> searchAllWorkLogs(List<SearchFilter> searchFilters, Pageable pageable) {
        WorkLogSearchFilterBuilder searchFilterBuilder = new WorkLogSearchFilterBuilder(searchFilters);
        Page<WorkLog> workLogList = workLogRepository.findAll(searchFilterBuilder.build(), pageable);
        List<WorkLogInfoDTO> workLogInfoDTOList = workLogList.getContent().stream().map(workLog -> {
            WorkLogInfoDTO workLogInfoDTO = modelMapper.map(workLog, WorkLogInfoDTO.class);
            workLogInfoDTO.setTotalHours(TrackeraDateUtil.formatDuration(workLog.getTotalHours()));
            return workLogInfoDTO;
        }).toList();
        return new PageImpl<>(workLogInfoDTOList, pageable, workLogList.getTotalElements());
    }

    @Transactional
    public Map<String, Object> addWorkLog(ManageWorkLogDTO manageWorkLogDTO, MultipartFile worklogFile) {
        validateWorkLog(manageWorkLogDTO, null);

        Map<String, Object> processResult = processWorkLogFile(worklogFile);
        if (processResult.containsKey("isError")) {
            return processResult;
        }
        WorkLog workLog = saveWorkLog(manageWorkLogDTO, Double.parseDouble(processResult.get("totalTime").toString()) / 60.0);
        saveWorkLogDetails((List<WorkLogDetail>) processResult.get("workLogDetails"), workLog);
        return Map.of("message", "Worklog uploaded successfully");
    }

    @Transactional
    public Map<String, Object> updateWorkLog(Long id, ManageWorkLogDTO manageWorkLogDTO, MultipartFile worklogFile) {
        WorkLog workLog = workLogRepository.findOne(id);
        if (workLog == null) {
            throw new BusinessException("WorkLog with ID " + id + " doesn't exist");
        }
        validateWorkLog(manageWorkLogDTO, id);

        if (worklogFile != null) {
            Map<String, Object> processResult = processWorkLogFile(worklogFile);
            if (processResult.containsKey("isError")) {
                return processResult;
            }
            workLog.setTotalHours(BigDecimal.valueOf(Double.parseDouble(processResult.get("totalTime").toString()) / 60.0));
            workLogDetailRepository.deleteAllByWorkLog(workLog);
            saveWorkLogDetails((List<WorkLogDetail>) processResult.get("workLogDetails"), workLog);
        }

        if (!StringUtils.isEmpty(manageWorkLogDTO.getLogName())) {
            workLog.setName(manageWorkLogDTO.getLogName());
        }
        workLog.setWorkDate(manageWorkLogDTO.getLogDate());
        workLogRepository.save(workLog);
        return Map.of("message", "Worklog updated successfully");
    }

    @Transactional
    public String deleteWorkLog(Long id) {
        WorkLog workLog = workLogRepository.findOne(id);
        if (workLog == null) {
            throw new BusinessException("WorkLog with ID " + id + " doesn't exist");
        }
        workLogDetailRepository.deleteAllByWorkLog(workLog);
        workLogRepository.delete(workLog);
        return "Worklog deleted successfully";
    }

    public List<WorkLogSummaryDTO> getCurrentMonthSummary() {
        LocalDate now = LocalDate.now();
        BigDecimal totalHours = workLogRepository.sumTotalHoursByWorkDateBetween(now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
        BigDecimal targetHours = BigDecimal.valueOf(200.0); // @TODO --> Should be based on user settings
        BigDecimal remainingHours = targetHours.subtract(totalHours).max(BigDecimal.ZERO);
        return List.of(
                new WorkLogSummaryDTO("Total Logged Hours", "Equivalent to " + TrackeraDateUtil.formatDurationWithDays(totalHours), "total", totalHours.toString()),
                new WorkLogSummaryDTO("Target Hours", "Equivalent to " + TrackeraDateUtil.formatDurationWithDays(targetHours), "target", targetHours.toString()),
                new WorkLogSummaryDTO("Remaining Hours", "Equivalent to " + TrackeraDateUtil.formatDurationWithDays(remainingHours), "remaining", remainingHours.toString())
        );
    }

    private void validateWorkLog(ManageWorkLogDTO manageWorkLogDTO, Long existingWorkLogId) {
        if (workLogRepository.existsByWorkDateAndWorkLogNot(manageWorkLogDTO.getLogDate(), existingWorkLogId)) {
            throw new BusinessException("WorkLog for the date " + manageWorkLogDTO.getLogDate() + " already exists.");
        }
        if (manageWorkLogDTO.getLogDate().isAfter(LocalDate.now())) {
            throw new BusinessException("WorkLog date cannot be in the future.");
        }
    }

    private Map<String, Object> processWorkLogFile(MultipartFile worklogFile) {
        List<List<String>> parsedData;
        try {
            parsedData = FileHandler.validateAndParse(worklogFile);
        } catch (Exception ex) {
            LOGGER.error("Error processing worklog file", ex);
            throw new RuntimeException(ex.getMessage());
        }
        if (parsedData.isEmpty() || parsedData.size() == 1) {
            throw new IllegalArgumentException("The uploaded file is empty or does not contain any valid data.");
        }

        List<WorkLogDetail> allWorkLogDetails = new ArrayList<>();
        List<Map<String, Object>> rowErrors = new ArrayList<>();
        double totalTime = 0;
        for (int i = 1; i < parsedData.size(); i++) {
            List<String> row = parsedData.get(i);
            String taskName;
            LocalTime fromHour;
            LocalTime toHour;
            double taskLogDuration;
            String taskDescription;
            try {
                if (row.size() < 5) {
                    throw new IllegalArgumentException("Row does not contain enough columns. Expected at least 4 columns.");
                }
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
            workLogDetail.setDuration(BigDecimal.valueOf(taskLogDuration));
            workLogDetail.setDescription(taskDescription);
            workLogDetail.setStatus(WorkLog.Status.NOT_SYNCED);
            allWorkLogDetails.add(workLogDetail);
        }

        Map<String, Object> result = new HashMap<>();
        if (!rowErrors.isEmpty()) {
            result.put("isError", true);
            result.put("message", "There were some errors in the uploaded file.");
            result.put("errors", rowErrors);
        } else {
            result.put("totalTime", totalTime);
            result.put("workLogDetails", allWorkLogDetails);
        }
        return result;
    }

    private <T> T getCellValue(String cell, int idx, Class<T> expectedType) {
        int cellPosition = idx + 1;
        Object result;
        if (StringUtils.isEmpty(cell)) {
            throw new IllegalArgumentException("Cell value at position [" + cellPosition + "] is empty");
        }
        if (expectedType == String.class) {
            result = cell;
        } else if (expectedType == LocalTime.class) {
            try {
                result = LocalTime.parse(cell, TrackeraDateUtil.getDateTime12hFormatter());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid time format in cell at position [" + cellPosition + "]. Expected format is hh:mm AM/PM", e);
            }
        } else {
            throw new IllegalArgumentException("Cell value type at position [" + cellPosition + "] is not supported");
        }

        return expectedType.cast(result);
    }

    private double parseDuration(String duration) {
        Matcher matcher = DURATION_PATTERN.matcher(duration);
        int hours = 0, minutes = 0;

        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                hours = Integer.parseInt(matcher.group(1));
            }
            if (matcher.group(2) != null) {
                minutes = Integer.parseInt(matcher.group(2));
            }
        } else {
            throw new IllegalArgumentException("Invalid duration format: '" + duration + "'. Expected format is 'Xh Ym' (e.g., '2h 30m')");
        }

        return (hours * 60) + minutes;
    }

    public WorkLog saveWorkLog(ManageWorkLogDTO manageWorkLogDTO, double totalTime) {
        try {
            WorkLog workLog = new WorkLog();
            workLog.setUser(AppConfig.getCurrentUser());
            workLog.setTotalHours(BigDecimal.valueOf(totalTime));
            workLog.setWorkDate(manageWorkLogDTO.getLogDate());
            workLog.setStatus(WorkLog.Status.NOT_SYNCED);
            if (!StringUtils.isEmpty(manageWorkLogDTO.getLogName())) {
                workLog.setName(manageWorkLogDTO.getLogName());
            } else {
                DayOfWeek dayOfWeek = manageWorkLogDTO.getLogDate().getDayOfWeek();
                String dayName = dayOfWeek.name().substring(0, 1).toUpperCase() + dayOfWeek.name().substring(1).toLowerCase();
                String formattedDate = TrackeraDateUtil.getCompactedDateFormatter().format(manageWorkLogDTO.getLogDate());
                workLog.setName("Worklog - " + dayName + formattedDate);
            }

            return workLogRepository.save(workLog);
        } catch (Exception e) {
            LOGGER.error("Error saving worklog", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public void saveWorkLogDetails(List<WorkLogDetail> allWorkLogDetails, WorkLog workLog) {
        try {
            allWorkLogDetails.forEach(logDetail -> logDetail.setWorkLog(workLog));
            workLogDetailRepository.saveAll(allWorkLogDetails);
        } catch (Exception e) {
            LOGGER.error("Error saving worklog details", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
