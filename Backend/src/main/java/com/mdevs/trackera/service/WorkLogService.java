package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.worklog.NewWorkLogDTO;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.repository.WorkLogDetailRepository;
import com.mdevs.trackera.repository.WorkLogRepository;
import com.mdevs.trackera.shared.FileValidator;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WorkLogService {
    private final WorkLogRepository workLogRepository;

    private final WorkLogDetailRepository workLogDetailRepository;

    private final static SimpleDateFormat PERIOD_TIME_FORMAT = new SimpleDateFormat("hh:mm a");

    private final static Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)h)?\\s*(?:(\\d+)m)?");

    private final static DateTimeFormatter WORKLOG_DETAIL_DURATION_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final static DateTimeFormatter WORKLOG_NAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    public WorkLogService(WorkLogRepository workLogRepository, WorkLogDetailRepository workLogDetailRepository) {
        this.workLogRepository = workLogRepository;
        this.workLogDetailRepository = workLogDetailRepository;
    }

    @Transactional
    public String addWorkLog(NewWorkLogDTO newWorkLogDTO, MultipartFile worklogFile) {
        validateWorkLog(newWorkLogDTO, worklogFile);
        List<WorkLogDetail> allWorkLogDetails = new ArrayList<>();
        double totalTime = 0;
        try (InputStream inputStream = worklogFile.getInputStream()) {
            XSSFWorkbook workBook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workBook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0 || row.getCell(0) == null || StringUtils.isEmpty(row.getCell(0).getStringCellValue()))
                    continue;

                String taskName = row.getCell(0).getStringCellValue();
                String fromHour = PERIOD_TIME_FORMAT.format(row.getCell(1).getDateCellValue());
                String toHour = PERIOD_TIME_FORMAT.format(row.getCell(2).getDateCellValue());
                double taskLogDuration = parseDuration(row.getCell(3).getStringCellValue().trim());
                String taskDescription = row.getCell(4).getStringCellValue();

                totalTime += taskLogDuration;

                WorkLogDetail workLogDetail = new WorkLogDetail();
                workLogDetail.setTaskName(taskName);
                workLogDetail.setTaskUrl(null); // @TODO --> Should be based on the user's selected project
                workLogDetail.setStartTime(LocalTime.parse(fromHour, WORKLOG_DETAIL_DURATION_FORMATTER));
                workLogDetail.setEndTime(LocalTime.parse(toHour, WORKLOG_DETAIL_DURATION_FORMATTER));
                workLogDetail.setDuration(taskLogDuration);
                workLogDetail.setDescription(taskDescription);
                allWorkLogDetails.add(workLogDetail);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        WorkLog workLog = saveWorkLog(newWorkLogDTO, totalTime);
        saveWorkLogDetails(allWorkLogDetails, workLog);
        return "Worklog uploaded successfully";
    }

    private void validateWorkLog(NewWorkLogDTO newWorkLogDTO, MultipartFile worklogFile) {
        if (workLogRepository.existsByWorkDate(newWorkLogDTO.getLogDate())) {
            throw new BusinessException("Work log for the date " + newWorkLogDTO.getLogDate() + " already exists.");
        }
        FileValidator.validate(worklogFile);
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
        WorkLog workLog = new WorkLog();
        workLog.setTotalHours(totalTime);
        if (!StringUtils.isEmpty(newWorkLogDTO.getLogName())) {
            workLog.setName(newWorkLogDTO.getLogName());
        } else {
            LocalDate now = LocalDate.now();
            DayOfWeek dayOfWeek = now.getDayOfWeek();
            String dayName = dayOfWeek.name().substring(0, 1).toUpperCase() + dayOfWeek.name().substring(1).toLowerCase();
            String formattedDate = WORKLOG_NAME_DATE_FORMATTER.format(now);
            workLog.setName("Worklog - " + dayName + formattedDate);
        }


        return workLogRepository.save(workLog);
    }

    public void saveWorkLogDetails(List<WorkLogDetail> allWorkLogDetails, WorkLog workLog) {
        allWorkLogDetails.forEach(logDetail -> logDetail.setWorkLog(workLog));
        workLogDetailRepository.saveAll(allWorkLogDetails);
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
