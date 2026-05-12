package com.rheosim.application.experiment.dto;

public record ValidationErrorResponse(
        int row,
        String column,
        String message,
        String severity
) {}
