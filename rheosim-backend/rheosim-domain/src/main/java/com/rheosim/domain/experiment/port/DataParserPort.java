package com.rheosim.domain.experiment.port;

import com.rheosim.domain.experiment.model.DataColumn;

import java.io.InputStream;
import java.util.List;

public interface DataParserPort {

    boolean supports(String contentType);

    ParseResult parse(InputStream input, String contentType);

    record ParseResult(
            List<DataColumn> columns,
            List<double[]> rows,
            int totalRows
    ) {}
}
