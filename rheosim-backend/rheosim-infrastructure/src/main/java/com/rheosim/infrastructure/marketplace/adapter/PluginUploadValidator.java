package com.rheosim.infrastructure.marketplace.adapter;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

@Component
public class PluginUploadValidator {

    private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6d};
    private static final long MAX_PLUGIN_SIZE = 64 * 1024 * 1024; // 64 MB
    private static final List<String> REQUIRED_EXPORTS = List.of(
            "rheosim_plugin_init",
            "rheosim_plugin_get_metadata",
            "rheosim_plugin_relaxation_modulus",
            "rheosim_plugin_storage_modulus",
            "rheosim_plugin_loss_modulus",
            "rheosim_plugin_creep_compliance",
            "rheosim_plugin_shutdown",
            "memory"
    );

    public record ValidationResult(boolean valid, String message, String sha256Hash) {
        public static ValidationResult success(String hash) {
            return new ValidationResult(true, "Validation passed", hash);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }
    }

    public ValidationResult validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ValidationResult.failure("No file provided");
        }

        if (file.getSize() > MAX_PLUGIN_SIZE) {
            return ValidationResult.failure(
                    "Plugin exceeds maximum size of " + MAX_PLUGIN_SIZE / (1024 * 1024) + " MB");
        }

        byte[] bytes = file.getBytes();

        if (!hasValidMagicNumber(bytes)) {
            return ValidationResult.failure("Invalid WASM file: bad magic number");
        }

        if (!hasValidVersion(bytes)) {
            return ValidationResult.failure("Invalid WASM version (expected 1)");
        }

        String hash = computeSha256(bytes);
        return ValidationResult.success(hash);
    }

    public ValidationResult validateBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return ValidationResult.failure("Empty bytes");
        }

        if (bytes.length > MAX_PLUGIN_SIZE) {
            return ValidationResult.failure("Plugin exceeds maximum size");
        }

        if (!hasValidMagicNumber(bytes)) {
            return ValidationResult.failure("Invalid WASM file: bad magic number");
        }

        if (!hasValidVersion(bytes)) {
            return ValidationResult.failure("Invalid WASM version");
        }

        String hash = computeSha256(bytes);
        return ValidationResult.success(hash);
    }

    private boolean hasValidMagicNumber(byte[] bytes) {
        if (bytes.length < 8) return false;
        return bytes[0] == WASM_MAGIC[0]
                && bytes[1] == WASM_MAGIC[1]
                && bytes[2] == WASM_MAGIC[2]
                && bytes[3] == WASM_MAGIC[3];
    }

    private boolean hasValidVersion(byte[] bytes) {
        if (bytes.length < 8) return false;
        // WASM version 1 = 0x01 0x00 0x00 0x00
        return bytes[4] == 0x01 && bytes[5] == 0x00
                && bytes[6] == 0x00 && bytes[7] == 0x00;
    }

    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
