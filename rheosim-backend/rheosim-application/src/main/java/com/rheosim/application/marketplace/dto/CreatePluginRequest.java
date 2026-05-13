package com.rheosim.application.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePluginRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @Size(max = 50) String license,
        List<String> tags
) {}
