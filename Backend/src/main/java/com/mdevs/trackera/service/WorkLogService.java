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
import com.mdevs.trackera.shared.enums.WorkLogColumn;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.search_filter.SearchFilter;
import com.mdevs.trackera.shared.search_filter.WorkLogSearchFilterBuilder;
import com.mdevs.trackera.shared.utils.TrackeraTimeSpanUtil;
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
import java.util.function.Function;
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
            workLogInfoDTO.setTotalHours(TrackeraTimeSpanUtil.formatDuration(workLog.getTotalHours()));
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
        if (workLog.getUser().getId() != Objects.requireNonNull(AppConfig.getCurrentUser()).getId()) {
            throw new BusinessException("You are not authorized to update this worklog");
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
        if (workLog.getUser().getId() != Objects.requireNonNull(AppConfig.getCurrentUser()).getId()) {
            throw new BusinessException("You are not authorized to delete this worklog");
        }
        workLogDetailRepository.deleteAllByWorkLog(workLog);
        workLogRepository.delete(workLog);
        return "Worklog deleted successfully";
    }

    public List<WorkLogSummaryDTO> getCurrentMonthSummary() {
        LocalDate now = LocalDate.now();
        BigDecimal totalHours = workLogRepository.sumTotalHoursByUserAndWorkDateBetween(AppConfig.getCurrentUser(), now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
        BigDecimal targetHours = BigDecimal.valueOf(200.0); // @TODO --> Should be based on user settings
        BigDecimal remainingHours = targetHours.subtract(totalHours).max(BigDecimal.ZERO);
        return List.of(
                new WorkLogSummaryDTO("Logged Hours", "Equivalent to " + TrackeraTimeSpanUtil.formatDurationWithDays(totalHours), "logged", totalHours.toString()),
                new WorkLogSummaryDTO("Target Hours", "Equivalent to " + TrackeraTimeSpanUtil.formatDurationWithDays(targetHours), "target", targetHours.toString()),
                new WorkLogSummaryDTO("Remaining Hours", "Equivalent to " + TrackeraTimeSpanUtil.formatDurationWithDays(remainingHours), "remaining", remainingHours.toString())
        );
    }

    private void validateWorkLog(ManageWorkLogDTO manageWorkLogDTO, Long existingWorkLogId) {
        if (workLogRepository.existsByUserAndWorkDateAndWorkLogNot(AppConfig.getCurrentUser(), manageWorkLogDTO.getLogDate(), existingWorkLogId)) {
            throw new BusinessException("WorkLog for the date " + manageWorkLogDTO.getLogDate() + " already exists.");
        }
        if (manageWorkLogDTO.getLogDate().isAfter(LocalDate.now())) {
            throw new BusinessException("WorkLog date cannot be in the future.");
        }
        if (manageWorkLogDTO.getLogDate().isBefore(AppConfig.getMinQueryableDate())) {
            throw new BusinessException("WorkLog date cannot be before " + AppConfig.getMinQueryableDate() + ".");
        }
    }

    private Map<String, Object> processWorkLogFile(MultipartFile worklogFile) {
        List<Map<WorkLogColumn, String>> parsedData;
        try {
            parsedData = FileHandler.validateAndParse(worklogFile);
        } catch (Exception ex) {
            LOGGER.error("Error processing worklog file", ex);
            throw new RuntimeException(ex.getMessage());
        }

        if (parsedData.isEmpty()) {
            throw new BusinessException("The uploaded file is empty or does not contain any valid data.");
        }

        List<WorkLogDetail> allWorkLogDetails = new ArrayList<>();
        List<Map<String, Object>> rowErrors = new ArrayList<>();
        double totalTime = 0;
        for (Map<WorkLogColumn, String> row : parsedData) {
            StringJoiner rowErrorMessages = new StringJoiner("; ");

            String taskName = validateAndGetCell(row, WorkLogColumn.TASK_NAME, rowErrorMessages);
            LocalTime fromHour = validateAndGetCell(row, WorkLogColumn.FROM_HOUR, rowErrorMessages);
            LocalTime toHour = validateAndGetCell(row, WorkLogColumn.TO_HOUR, rowErrorMessages);
            Double taskLogDuration = validateAndGetCell(row, WorkLogColumn.DURATION, this::parseDuration, rowErrorMessages);
            String taskDescription = validateAndGetCell(row, WorkLogColumn.DESCRIPTION, rowErrorMessages);

            if (rowErrorMessages.length() > 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("row", row.get(WorkLogColumn.ROW_NUMBER));
                error.put("error", rowErrorMessages.toString());
                rowErrors.add(error);
                continue;
            }

            double taskLogDurationValue = taskLogDuration != null ? taskLogDuration : 0;
            totalTime += taskLogDurationValue;

            WorkLogDetail workLogDetail = new WorkLogDetail();
            workLogDetail.setTaskName(taskName);
            workLogDetail.setTaskUrl(null); // @TODO --> Should be based on the user's selected project
            workLogDetail.setStartTime(fromHour);
            workLogDetail.setEndTime(toHour);
            workLogDetail.setDuration(BigDecimal.valueOf(taskLogDurationValue));
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

    private <T> T validateAndGetCell(Map<WorkLogColumn, String> row, WorkLogColumn columnType, StringJoiner errorMessages) {
        return validateAndGetCell(row, columnType, null, errorMessages);
    }

    private <T, R> R validateAndGetCell(Map<WorkLogColumn, String> row, WorkLogColumn columnType, Function<T, R> additionalParser, StringJoiner errorMessages) {
        try {
            T parsedCell = (T) parseCell(row.get(columnType), columnType.getLabel(), columnType.getResultType());
            return additionalParser != null ? additionalParser.apply(parsedCell) : (R) parsedCell;
        } catch (Exception e) {
            errorMessages.add(e.getMessage());
            return null;
        }
    }

    private <T> T parseCell(String cell, String cellName, Class<T> expectedType) {
        if (StringUtils.isEmpty(cell)) {
            throw new BusinessException("[" + cellName + "] cell is empty");
        }

        Object result = cell;

        if (expectedType == LocalTime.class) {
            try {
                result = LocalTime.parse(cell, TrackeraTimeSpanUtil.getDateTime12hFormatter());
            } catch (Exception e) {
                throw new BusinessException("[" + cellName + "] Invalid time format. Expected format is hh:mm AM/PM");
            }
        }

        try {
            return expectedType.cast(result);
        } catch (Exception e) {
            throw new BusinessException("[" + cellName + "] cell type is not supported");
        }
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
            throw new BusinessException("[" + WorkLogColumn.DURATION.getLabel() + "] Invalid format: '" + duration + "'. Expected format is 'Xh Ym' (e.g., '2h 30m')");
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
                String formattedDate = TrackeraTimeSpanUtil.getCompactedDateFormatter().format(manageWorkLogDTO.getLogDate());
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
