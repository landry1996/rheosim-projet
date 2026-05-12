package com.rheosim.domain.experiment.port;

import com.rheosim.domain.experiment.model.DataColumn;
import com.rheosim.domain.experiment.model.ExperimentType;
import com.rheosim.domain.experiment.model.ValidationError;

import java.util.List;

public interface DataValidatorPort {

    List<ValidationError> validate(
            ExperimentType experimentType,
            List<DataColumn> columns,
            List<double[]> rows
    );
}
