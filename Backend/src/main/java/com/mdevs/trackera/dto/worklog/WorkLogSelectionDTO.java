package com.mdevs.trackera.dto.worklog;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogSelectionDTO {
    @Size(max = 300, message = "Task names list can contain a maximum of 300 items.")
    private List<String> taskNames;

    @Size(max = 300, message = "Entry IDs list can contain a maximum of 300 items.")
    private List<String> entryIds;
}
