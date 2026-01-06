package com.mdevs.trackera.dto.worklog;

import java.util.List;

public record WorkLogSelectionDTO(List<String> taskNames, List<String> entryIds) {
}
