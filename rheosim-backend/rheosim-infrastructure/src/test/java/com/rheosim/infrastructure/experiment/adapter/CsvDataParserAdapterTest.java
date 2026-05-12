package com.rheosim.infrastructure.experiment.adapter;

import com.rheosim.domain.experiment.model.DataColumn;
import com.rheosim.domain.experiment.port.DataParserPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CsvDataParserAdapter")
class CsvDataParserAdapterTest {

    private final CsvDataParserAdapter parser = new CsvDataParserAdapter();

    @Test
    @DisplayName("supports text/csv content type")
    void supports_shouldAcceptCsvContentTypes() {
        assertThat(parser.supports("text/csv")).isTrue();
        assertThat(parser.supports("application/csv")).isTrue();
        assertThat(parser.supports("text/plain")).isTrue();
        assertThat(parser.supports("application/json")).isFalse();
    }

    @Test
    @DisplayName("parse extracts columns and rows from valid CSV")
    void parse_shouldExtractColumnsAndRows() {
        String csv = "time (s),strain,shear_stress (Pa)\n" +
                "0.1,0.01,100.5\n" +
                "0.2,0.02,201.0\n" +
                "0.3,0.03,300.8\n";

        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        DataParserPort.ParseResult result = parser.parse(input, "text/csv");

        assertThat(result.columns()).hasSize(3);
        assertThat(result.rows()).hasSize(3);
        assertThat(result.totalRows()).isEqualTo(3);
    }

    @Test
    @DisplayName("parse infers physical quantities from headers")
    void parse_shouldInferPhysicalQuantities() {
        String csv = "time (s),frequency (Hz),viscosity (Pa.s)\n1,10,100\n";
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DataParserPort.ParseResult result = parser.parse(input, "text/csv");

        assertThat(result.columns().get(0).quantity()).isEqualTo(DataColumn.PhysicalQuantity.TIME);
        assertThat(result.columns().get(1).quantity()).isEqualTo(DataColumn.PhysicalQuantity.FREQUENCY);
        assertThat(result.columns().get(2).quantity()).isEqualTo(DataColumn.PhysicalQuantity.VISCOSITY);
    }

    @Test
    @DisplayName("parse extracts units from parentheses")
    void parse_shouldExtractUnitsFromParentheses() {
        String csv = "time (s),G' [Pa]\n1.0,1000\n";
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DataParserPort.ParseResult result = parser.parse(input, "text/csv");

        assertThat(result.columns().get(0).unit()).isEqualTo("s");
        assertThat(result.columns().get(1).unit()).isEqualTo("Pa");
    }

    @Test
    @DisplayName("parse handles empty values as NaN")
    void parse_shouldHandleEmptyValuesAsNaN() {
        String csv = "time,value\n1.0,10.0\n2.0,\n3.0,30.0\n";
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        DataParserPort.ParseResult result = parser.parse(input, "text/csv");

        assertThat(result.rows().get(1)[1]).isNaN();
    }

    @Test
    @DisplayName("parse throws on non-numeric data")
    void parse_shouldThrowOnNonNumericData() {
        String csv = "time,value\n1.0,hello\n";
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(input, "text/csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-numeric");
    }
}
