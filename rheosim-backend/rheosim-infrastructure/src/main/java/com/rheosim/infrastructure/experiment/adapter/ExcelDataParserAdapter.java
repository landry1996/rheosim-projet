package com.rheosim.infrastructure.experiment.adapter;

import com.rheosim.domain.experiment.model.DataColumn;
import com.rheosim.domain.experiment.port.DataParserPort;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelDataParserAdapter implements DataParserPort {

    @Override
    public boolean supports(String contentType) {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType)
                || "application/vnd.ms-excel".equals(contentType);
    }

    @Override
    public ParseResult parse(InputStream input, String contentType) {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file has no header row");
            }

            int colCount = headerRow.getLastCellNum();
            List<DataColumn> columns = new ArrayList<>();
            for (int i = 0; i < colCount; i++) {
                Cell cell = headerRow.getCell(i);
                String header = (cell != null) ? cell.getStringCellValue().trim() : "Column" + i;
                columns.add(inferColumn(header));
            }

            List<double[]> rows = new ArrayList<>();
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                double[] values = new double[colCount];
                boolean hasData = false;
                for (int colIdx = 0; colIdx < colCount; colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        values[colIdx] = Double.NaN;
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        values[colIdx] = cell.getNumericCellValue();
                        hasData = true;
                    } else if (cell.getCellType() == CellType.STRING) {
                        try {
                            values[colIdx] = Double.parseDouble(cell.getStringCellValue().trim());
                            hasData = true;
                        } catch (NumberFormatException e) {
                            values[colIdx] = Double.NaN;
                        }
                    } else {
                        values[colIdx] = Double.NaN;
                    }
                }
                if (hasData) {
                    rows.add(values);
                }
            }

            return new ParseResult(columns, rows, rows.size());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Excel file", e);
        }
    }

    private DataColumn inferColumn(String header) {
        String normalized = header.toLowerCase().trim();
        String unit = extractUnit(header);
        DataColumn.PhysicalQuantity quantity = inferQuantity(normalized);
        return new DataColumn(header, unit, quantity);
    }

    private String extractUnit(String header) {
        int start = header.indexOf('(');
        int end = header.indexOf(')');
        if (start >= 0 && end > start) {
            return header.substring(start + 1, end).trim();
        }
        int bracketStart = header.indexOf('[');
        int bracketEnd = header.indexOf(']');
        if (bracketStart >= 0 && bracketEnd > bracketStart) {
            return header.substring(bracketStart + 1, bracketEnd).trim();
        }
        return "";
    }

    private DataColumn.PhysicalQuantity inferQuantity(String header) {
        if (header.contains("time") || header.contains("temps")) {
            return DataColumn.PhysicalQuantity.TIME;
        } else if (header.contains("freq") || header.contains("omega")) {
            return DataColumn.PhysicalQuantity.FREQUENCY;
        } else if (header.contains("shear_rate") || header.contains("gamma_dot")) {
            return DataColumn.PhysicalQuantity.SHEAR_RATE;
        } else if (header.contains("shear_stress") || header.contains("tau") || header.contains("sigma")) {
            return DataColumn.PhysicalQuantity.SHEAR_STRESS;
        } else if (header.contains("eta*") || header.contains("complex_visc")) {
            return DataColumn.PhysicalQuantity.COMPLEX_VISCOSITY;
        } else if (header.contains("viscosity") || header.contains("eta")) {
            return DataColumn.PhysicalQuantity.VISCOSITY;
        } else if (header.contains("g'") || header.contains("storage") || header.contains("g_prime")) {
            return DataColumn.PhysicalQuantity.STORAGE_MODULUS;
        } else if (header.contains("g\"") || header.contains("loss") || header.contains("g_double")) {
            return DataColumn.PhysicalQuantity.LOSS_MODULUS;
        } else if (header.contains("strain") || header.contains("gamma") || header.contains("deformation")) {
            return DataColumn.PhysicalQuantity.STRAIN;
        } else if (header.contains("temp") || header.contains("t(")) {
            return DataColumn.PhysicalQuantity.TEMPERATURE;
        } else if (header.contains("compliance") || header.contains("j(")) {
            return DataColumn.PhysicalQuantity.COMPLIANCE;
        }
        return DataColumn.PhysicalQuantity.OTHER;
    }
}
