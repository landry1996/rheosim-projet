package com.rheosim.application.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateVersionRequest(
        @NotBlank @Pattern(regexp = "\\d+\\.\\d+\\.\\d+") String version,
        @NotBlank String artifactUrl,
        @NotBlank String artifactHash,
        String changelog
) {}
