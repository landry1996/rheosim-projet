package com.rheosim.infrastructure.ml.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.AbstractBlockingStub;

public final class MLServiceGrpc {

    private MLServiceGrpc() {}

    public static MLServiceBlockingStub newBlockingStub(Channel channel) {
        return new MLServiceBlockingStub(channel, CallOptions.DEFAULT);
    }

    public static final class MLServiceBlockingStub extends AbstractBlockingStub<MLServiceBlockingStub> {

        protected MLServiceBlockingStub(Channel channel, CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected MLServiceBlockingStub build(Channel channel, CallOptions callOptions) {
            return new MLServiceBlockingStub(channel, callOptions);
        }

        public MlService.PredictionResponse predictModel(MlService.PredictionRequest request) {
            throw new io.grpc.StatusRuntimeException(
                    io.grpc.Status.UNAVAILABLE.withDescription("ML Service not connected"));
        }

        public MlService.HealthResponse healthCheck(MlService.Empty request) {
            throw new io.grpc.StatusRuntimeException(
                    io.grpc.Status.UNAVAILABLE.withDescription("ML Service not connected"));
        }
    }
}
