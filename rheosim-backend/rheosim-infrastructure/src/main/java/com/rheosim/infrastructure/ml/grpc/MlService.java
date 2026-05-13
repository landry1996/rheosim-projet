package com.rheosim.infrastructure.ml.grpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MlService {

    private MlService() {}

    public static final class PredictionRequest {
        private final List<Double> timePoints;
        private final List<Double> values;
        private final String experimentType;

        private PredictionRequest(Builder builder) {
            this.timePoints = Collections.unmodifiableList(new ArrayList<>(builder.timePoints));
            this.values = Collections.unmodifiableList(new ArrayList<>(builder.values));
            this.experimentType = builder.experimentType;
        }

        public List<Double> getTimePointsList() { return timePoints; }
        public List<Double> getValuesList() { return values; }
        public String getExperimentType() { return experimentType; }

        public static Builder newBuilder() { return new Builder(); }

        public static final class Builder {
            private final List<Double> timePoints = new ArrayList<>();
            private final List<Double> values = new ArrayList<>();
            private String experimentType = "";

            public Builder addAllTimePoints(Iterable<Double> pts) {
                for (Double d : pts) timePoints.add(d);
                return this;
            }

            public Builder addAllValues(Iterable<Double> vals) {
                for (Double d : vals) values.add(d);
                return this;
            }

            public Builder setExperimentType(String type) {
                this.experimentType = type;
                return this;
            }

            public PredictionRequest build() { return new PredictionRequest(this); }
        }
    }

    public static final class PredictionResponse {
        private final String recommendedModel;
        private final double confidence;
        private final List<Double> initialParameters;
        private final List<Alternative> alternatives;

        public PredictionResponse(String recommendedModel, double confidence,
                                  List<Double> initialParameters, List<Alternative> alternatives) {
            this.recommendedModel = recommendedModel;
            this.confidence = confidence;
            this.initialParameters = initialParameters;
            this.alternatives = alternatives;
        }

        public String getRecommendedModel() { return recommendedModel; }
        public double getConfidence() { return confidence; }
        public List<Double> getInitialParametersList() { return initialParameters; }
        public List<Alternative> getAlternativesList() { return alternatives; }

        public static final class Alternative {
            private final String modelType;
            private final double confidence;
            private final List<Double> initialParameters;

            public Alternative(String modelType, double confidence, List<Double> initialParameters) {
                this.modelType = modelType;
                this.confidence = confidence;
                this.initialParameters = initialParameters;
            }

            public String getModelType() { return modelType; }
            public double getConfidence() { return confidence; }
            public List<Double> getInitialParametersList() { return initialParameters; }
        }
    }

    public static final class HealthResponse {
        private final String status;
        private final boolean modelsLoaded;

        public HealthResponse(String status, boolean modelsLoaded) {
            this.status = status;
            this.modelsLoaded = modelsLoaded;
        }

        public String getStatus() { return status; }
        public boolean getModelsLoaded() { return modelsLoaded; }
    }

    public static final class Empty {
        private Empty() {}

        public static Builder newBuilder() { return new Builder(); }

        public static final class Builder {
            public Empty build() { return new Empty(); }
        }
    }
}
