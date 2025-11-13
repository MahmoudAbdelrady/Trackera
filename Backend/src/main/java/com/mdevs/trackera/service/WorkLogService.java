package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.worklog.*;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.shared.FileHandler;
import com.mdevs.trackera.shared.WorkLogQueryBuilder;
import com.mdevs.trackera.shared.enums.WorkLogColumn;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.exceptions.types.NotFoundException;
import com.mdevs.trackera.shared.exceptions.types.UnauthorizedException;
import com.mdevs.trackera.shared.DurationFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.DecimalFormat;
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

    @PersistenceContext
    private EntityManager entityManager;

    private final static Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)h)?\\s*(?:(\\d+)m)?");

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkLogService.class);

    public WorkLogService(WorkLogRepository workLogRepository, WorkLogDetailRepository workLogDetailRepository, ModelMapper modelMapper) {
        this.workLogRepository = workLogRepository;
        this.workLogDetailRepository = workLogDetailRepository;
        this.modelMapper = modelMapper;
    }

    //<editor-fold desc="Search & Retrieval">
    public Page<WorkLogInfoDTO> searchAllWorkLogs(WorkLogSearchFilterDTO searchFilterDTO, Pageable pageable) {
        Map<String, Object> queryParameters = new HashMap<>();
        String searchQuery = buildSearchQuery(searchFilterDTO, queryParameters);

        TypedQuery<WorkLog> typedQuery = entityManager.createQuery(searchQuery, WorkLog.class);
        queryParameters.forEach(typedQuery::setParameter);
        typedQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<WorkLogInfoDTO> workLogInfoDTOList = typedQuery.getResultList().stream().map(workLog -> {
            WorkLogInfoDTO workLogInfoDTO = modelMapper.map(workLog, WorkLogInfoDTO.class);
            workLogInfoDTO.setId(workLog.getUuid());
            workLogInfoDTO.setTotalTime(DurationFormatter.formatDuration(workLog.getTotalMinutes(), true));
            return workLogInfoDTO;
        }).toList();

        String countQueryStr = searchQuery.replaceFirst("SELECT wl FROM WorkLog wl", "SELECT COUNT(wl) FROM WorkLog wl");
        TypedQuery<Long> countQuery = entityManager.createQuery(countQueryStr, Long.class);
        queryParameters.forEach(countQuery::setParameter);
        Long totalRecords = countQuery.getSingleResult();

        return new PageImpl<>(workLogInfoDTOList, pageable, totalRecords);
    }

    public WorkLogInfoDTO getWorkLogByUUID(String uuid) {
        WorkLog workLog = validateWorkLogExistsAndHasPermission(uuid);
        WorkLogInfoDTO workLogInfoDTO = modelMapper.map(workLog, WorkLogInfoDTO.class);
        workLogInfoDTO.setId(workLog.getUuid());
        workLogInfoDTO.setTotalTime(DurationFormatter.formatDuration(workLog.getTotalMinutes(), true));
        return workLogInfoDTO;
    }

    public List<WorkLogTaskDTO> getWorkLogDetailSummary(String uuid) {
        WorkLog workLog = validateWorkLogExistsAndHasPermission(uuid);
        List<Map<String, Object>> workLogDetailGroups = workLogDetailRepository.getGroupedWorkLogDetailsByWorkLog(workLog);
        return workLogDetailGroups.stream().map(worklogGroup -> {
            WorkLogTaskDTO workLogTaskDTO = new WorkLogTaskDTO(worklogGroup.get("taskName").toString(), (String) worklogGroup.get("taskUrl"));
            Integer totalMinutes = Integer.parseInt(worklogGroup.get("totalMinutes").toString());
            workLogTaskDTO.setTotalHours(DurationFormatter.formatDuration(totalMinutes, false));
            workLogTaskDTO.setTotalMinutes(totalMinutes); // for sorting purpose
            workLogTaskDTO.setStatus(WorkLogStatus.valueOf(worklogGroup.get("status").toString()));
            return workLogTaskDTO;
        }).toList();
    }

    public List<WorkLogEntryDTO> getWorkLogTaskDetails(String uuid, String taskName) {
        WorkLog workLog = validateWorkLogExistsAndHasPermission(uuid);
        List<WorkLogDetail> workLogDetails = workLogDetailRepository.findByWorkLogAndTaskName(workLog, taskName);
        if (workLogDetails.isEmpty()) {
            throw new NotFoundException("No logs found for the specified task in this worklog");
        }
        return workLogDetails.stream().map(workLogDetail -> {
            WorkLogEntryDTO workLogEntryDTO = new WorkLogEntryDTO();
            workLogEntryDTO.setId(workLogDetail.getUuid());
            workLogEntryDTO.setFromTime(DurationFormatter.getDateTime12hFormatter().format(workLogDetail.getStartTime()));
            workLogEntryDTO.setToTime(DurationFormatter.getDateTime12hFormatter().format(workLogDetail.getEndTime()));
            workLogEntryDTO.setDuration(DurationFormatter.formatDuration(workLogDetail.getDuration(), false));
            workLogEntryDTO.setDescription(workLogDetail.getDescription());
            workLogEntryDTO.setStatus(workLogDetail.isSynced() ? WorkLogStatus.SYNCED : WorkLogStatus.NOT_SYNCED);
            return workLogEntryDTO;
        }).toList();
    }
    //</editor-fold>

    //<editor-fold desc="Creation & Update">
    @Transactional
    public Map<String, Object> addWorkLog(ManageWorkLogDTO manageWorkLogDTO, MultipartFile worklogFile) {
        validateWorkLog(manageWorkLogDTO, null);

        Map<String, Object> processResult = processWorkLogFile(worklogFile);
        if (processResult.containsKey("isError")) {
            return processResult;
        }
        WorkLog workLog = saveWorkLog(manageWorkLogDTO, (int) processResult.get("totalMinutes"));
        saveWorkLogDetails((List<WorkLogDetail>) processResult.get("workLogDetails"), workLog);
        return Map.of("message", "Worklog uploaded successfully");
    }

    @Transactional
    public Map<String, Object> updateWorkLog(String uuid, ManageWorkLogDTO manageWorkLogDTO, MultipartFile worklogFile) {
        WorkLog workLog = validateWorkLogExistsAndHasPermission(uuid);
        validateWorkLog(manageWorkLogDTO, workLog.getId());

        if (worklogFile != null) {
            Map<String, Object> processResult = processWorkLogFile(worklogFile);
            if (processResult.containsKey("isError")) {
                return processResult;
            }
            workLog.setTotalMinutes((int) processResult.get("totalMinutes"));
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
    //</editor-fold>

    //<editor-fold desc="Deletion">
    @Transactional
    public void deleteWorkLog(String uuid) {
        WorkLog workLog = validateWorkLogExistsAndHasPermission(uuid);
        workLogDetailRepository.deleteAllByWorkLog(workLog);
        workLogRepository.delete(workLog);
    }

    @Transactional
    public Map<String, Object> deleteWorkLogTaskDetails(String uuid, String taskName) {
        WorkLog workLog = validateWorkLogExistsAndHasPermission(uuid);
        List<WorkLogDetail> detailsToDelete = workLogDetailRepository.findByWorkLogAndTaskName(workLog, taskName);
        if (detailsToDelete.isEmpty()) {
            throw new NotFoundException("No logs found for the specified task in this worklog");
        }
        workLogDetailRepository.deleteAll(detailsToDelete);

        List<WorkLogDetail> existingDetails = workLogDetailRepository.findAllByWorkLog(workLog);
        Map<String, Object> result = new HashMap<>();
        if (existingDetails.isEmpty()) {
            workLogRepository.delete(workLog);
            result.put("isLast", true);
        } else {
            int totalDeletedMinutes = detailsToDelete.stream().mapToInt(WorkLogDetail::getDuration).sum();
            workLog.setTotalMinutes(workLog.getTotalMinutes() - totalDeletedMinutes);
            workLogRepository.save(workLog);
        }

        result.put("message", "Task logs deleted successfully");
        return result;
    }

    @Transactional
    public Map<String, Object> deleteWorkLogTaskEntry(String uuid) {
        WorkLogDetail workLogEntry = workLogDetailRepository.findByUuid(uuid);
        if (workLogEntry == null) {
            throw new NotFoundException("WorkLog entry not found");
        }
        WorkLog workLog = workLogEntry.getWorkLog();
        if (!workLog.getUser().getId().equals(AppConfig.getAuthenticatedCurrentUser().getId())) {
            throw new UnauthorizedException("You are not authorized to access this entry");
        }

        workLogDetailRepository.delete(workLogEntry);

        Map<String, Object> result = new HashMap<>();
        if (!workLogDetailRepository.existsByWorkLog(workLog)) {
            workLogRepository.delete(workLog);
            result.put("isLast", true);
        } else {
            int deletedDuration = workLogEntry.getDuration();
            workLog.setTotalMinutes(workLog.getTotalMinutes() - deletedDuration);
            workLogRepository.save(workLog);

            if (!workLogDetailRepository.existsByWorkLogAndTaskName(workLog, workLogEntry.getTaskName())) {
                result.put("isLastOfTask", true);
            }
        }

        result.put("message", "WorkLog entry deleted successfully");
        return result;
    }
    //</editor-fold>

    //<editor-fold desc="Summary & Analytics">
    public List<WorkLogSummaryDTO> getCurrentMonthSummary() {
        LocalDate now = LocalDate.now();
        DecimalFormat durationDecimalFormat = DurationFormatter.getDurationDecimalFormat();
        int totalLogged = workLogRepository.sumTotalMinutesByUserAndWorkDateBetween(AppConfig.getAuthenticatedCurrentUser(), now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
        int targetMinutes = 200 * 60; // @TODO --> Should be based on user settings and user can choose decimal target hours
        int remainingMinutes = Math.max(targetMinutes - totalLogged, 0);
        return List.of(
                new WorkLogSummaryDTO("Logged Hours", "Equivalent to " + DurationFormatter.formatDuration(totalLogged, true), "logged", durationDecimalFormat.format(totalLogged / 60.0)),
                new WorkLogSummaryDTO("Target Hours", "Equivalent to " + DurationFormatter.formatDuration(targetMinutes, true), "target", durationDecimalFormat.format(targetMinutes / 60.0)),
                new WorkLogSummaryDTO("Remaining Hours", "Equivalent to " + DurationFormatter.formatDuration(remainingMinutes, true), "remaining", durationDecimalFormat.format(remainingMinutes / 60.0))
        );
    }
    //</editor-fold>

    //<editor-fold desc="Internal Methods & Validations">
    private String buildSearchQuery(WorkLogSearchFilterDTO searchFilterDTO, Map<String, Object> queryParameters) {
        WorkLogQueryBuilder workLogQueryBuilder = new WorkLogQueryBuilder(AppConfig.getAuthenticatedCurrentUser().getId());
        if (searchFilterDTO != null) {
            searchFilterDTO.validate();
            workLogQueryBuilder
                    .withLogName(searchFilterDTO.getLogName())
                    .withDateRange(searchFilterDTO.getDateFrom(), searchFilterDTO.getDateTo())
                    .withTotalHours(searchFilterDTO.getTotalHours())
                    .withEvaluation(searchFilterDTO.getEvaluation())
                    .withStatus(searchFilterDTO.getStatus());
        }

        queryParameters.putAll(workLogQueryBuilder.getParameters());
        return workLogQueryBuilder.getQuery();
    }

    private void validateWorkLog(ManageWorkLogDTO manageWorkLogDTO, Long existingWorkLogId) {
        if (workLogRepository.existsByUserAndWorkDateAndWorkLogNot(AppConfig.getAuthenticatedCurrentUser(), manageWorkLogDTO.getLogDate(), existingWorkLogId)) {
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
        int totalMinutes = 0;
        for (Map<WorkLogColumn, String> row : parsedData) {
            totalMinutes += Optional.ofNullable(processWorkLogRow(row, allWorkLogDetails, rowErrors)).orElse(0);
        }

        Map<String, Object> result = new HashMap<>();
        if (!rowErrors.isEmpty()) {
            result.put("isError", true);
            result.put("message", "There were some errors in the uploaded file.");
            result.put("errors", rowErrors);
        } else {
            result.put("totalMinutes", totalMinutes);
            result.put("workLogDetails", allWorkLogDetails);
        }
        return result;
    }

    private Integer processWorkLogRow(Map<WorkLogColumn, String> row, List<WorkLogDetail> allWorkLogDetails, List<Map<String, Object>> rowErrors) {
        StringJoiner rowErrorMessages = new StringJoiner("; ");

        String taskName = validateAndGetCell(row, WorkLogColumn.TASK_NAME, rowErrorMessages);
        LocalTime fromHour = validateAndGetCell(row, WorkLogColumn.FROM_HOUR, rowErrorMessages);
        LocalTime toHour = validateAndGetCell(row, WorkLogColumn.TO_HOUR, rowErrorMessages);
        Integer taskLogDuration = validateAndGetCell(row, WorkLogColumn.DURATION, this::parseDuration, rowErrorMessages);
        String taskDescription = validateAndGetCell(row, WorkLogColumn.DESCRIPTION, rowErrorMessages);

        if (rowErrorMessages.length() > 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("row", row.get(WorkLogColumn.ROW_NUMBER));
            error.put("error", rowErrorMessages.toString());
            rowErrors.add(error);
            return null;
        }

        int taskLogDurationValue = taskLogDuration != null ? taskLogDuration : 0;

        WorkLogDetail workLogDetail = new WorkLogDetail();
        workLogDetail.setTaskName(taskName);
        workLogDetail.setTaskUrl(null); // @TODO --> Should be based on the user's selected project
        workLogDetail.setStartTime(fromHour);
        workLogDetail.setEndTime(toHour);
        workLogDetail.setDuration(taskLogDurationValue);
        workLogDetail.setDescription(taskDescription);
        workLogDetail.setSynced(false);
        allWorkLogDetails.add(workLogDetail);

        return taskLogDurationValue;
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
                result = LocalTime.parse(cell, DurationFormatter.getDateTime12hFormatter());
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

    private int parseDuration(String duration) {
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

    private WorkLog saveWorkLog(ManageWorkLogDTO manageWorkLogDTO, int totalMinutes) {
        try {
            WorkLog workLog = new WorkLog();
            workLog.setUser(AppConfig.getAuthenticatedCurrentUser());
            workLog.setTotalMinutes(totalMinutes);
            workLog.setWorkDate(manageWorkLogDTO.getLogDate());
            workLog.setStatus(WorkLogStatus.NOT_SYNCED);
            if (!StringUtils.isEmpty(manageWorkLogDTO.getLogName())) {
                workLog.setName(manageWorkLogDTO.getLogName());
            } else {
                DayOfWeek dayOfWeek = manageWorkLogDTO.getLogDate().getDayOfWeek();
                String dayName = dayOfWeek.name().substring(0, 1).toUpperCase() + dayOfWeek.name().substring(1).toLowerCase();
                String formattedDate = DurationFormatter.getCompactedDateFormatter().format(manageWorkLogDTO.getLogDate());
                workLog.setName("Worklog - " + dayName + formattedDate);
            }

            return workLogRepository.save(workLog);
        } catch (Exception e) {
            LOGGER.error("Error saving worklog", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void saveWorkLogDetails(List<WorkLogDetail> allWorkLogDetails, WorkLog workLog) {
        try {
            allWorkLogDetails.forEach(logDetail -> logDetail.setWorkLog(workLog));
            workLogDetailRepository.saveAll(allWorkLogDetails);
        } catch (Exception e) {
            LOGGER.error("Error saving worklog details", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private WorkLog validateWorkLogExistsAndHasPermission(String uuid) {
        WorkLog workLog = workLogRepository.findByUserAndUuid(AppConfig.getAuthenticatedCurrentUser(), uuid);
        if (workLog == null) {
            throw new NotFoundException("WorkLog not found");
        }
        if (!workLog.getUser().getId().equals(AppConfig.getAuthenticatedCurrentUser().getId())) {
            throw new UnauthorizedException("You are not authorized to access this worklog");
        }
        return workLog;
    }
    //</editor-fold>
}
