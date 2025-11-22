package com.mdevs.trackera.dto.jira;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JiraSyncRequestDTO {
    private List<String> taskNames;

    private List<String> logIds;
}
