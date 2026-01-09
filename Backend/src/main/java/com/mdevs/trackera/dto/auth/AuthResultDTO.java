package com.mdevs.trackera.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResultDTO (String title, String desc) {
}
