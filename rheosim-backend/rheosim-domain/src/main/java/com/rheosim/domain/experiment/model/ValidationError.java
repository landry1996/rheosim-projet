package com.rheosim.domain.experiment.model;

public record ValidationError(
        int row,
        String column,
        String message,
        Severity severity
) {
    public enum Severity {
        WARNING,
        ERROR
    }
}
