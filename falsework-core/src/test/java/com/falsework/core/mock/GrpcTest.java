package com.falsework.core.mock;

import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GrpcTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcTest.class);

    @Test
    public void tt01() throws Exception {
        String uniqueName = InProcessServerBuilder.generateName();
        InProcessServerBuilder.forName(uniqueName)
                .directExecutor() // directExecutor is fine for unit tests
                .addService(new SimpleServiceGrpc.SimpleServiceImplBase() {
                    @Override
                    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                        try {
                            LOGGER.info("unary rpc:{}", request.getRequestMessage());
                            responseObserver.onNext(SimpleResponse.newBuilder().setResponseMessage("abc").build());
                            responseObserver.onCompleted();
                        } catch (Exception e) {
                            responseObserver.onError(Status.UNKNOWN.asException());
                        }
                    }

                    @Override
                    public StreamObserver<SimpleRequest> clientStreamingRpc(StreamObserver<SimpleResponse> responseObserver) {
                        return super.clientStreamingRpc(responseObserver);
                    }

                    @Override
                    public void serverStreamingRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                        super.serverStreamingRpc(request, responseObserver);
                    }

                    @Override
                    public StreamObserver<SimpleRequest> bidiStreamingRpc(StreamObserver<SimpleResponse> responseObserver) {
                        return super.bidiStreamingRpc(responseObserver);
                    }
                })
                .build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(uniqueName)
                .directExecutor()
                .build();
        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc.newBlockingStub(channel);
        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder().setRequestMessage("abc").build());
        LOGGER.info("client:{}", response);
    }

    @Test
    public void tt02() throws Exception {
        ServerBuilder.forPort(8080)
                .directExecutor() // directExecutor is fine for unit tests
                .addService(new SimpleServiceGrpc.SimpleServiceImplBase() {
                    @Override
                    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                        super.unaryRpc(request, responseObserver);
                    }

                    @Override
                    public StreamObserver<SimpleRequest> clientStreamingRpc(StreamObserver<SimpleResponse> responseObserver) {
                        return super.clientStreamingRpc(responseObserver);
                    }

                    @Override
                    public void serverStreamingRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                        super.serverStreamingRpc(request, responseObserver);
                    }

                    @Override
                    public StreamObserver<SimpleRequest> bidiStreamingRpc(StreamObserver<SimpleResponse> responseObserver) {
                        return super.bidiStreamingRpc(responseObserver);
                    }
                })
                .intercept(new ServerInterceptor() {
                    @Override
                    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                        return null;
                    }
                })
                .build()
                .start()
                .awaitTermination();
    }

    @Test
    public void tt03() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", 8080)
                .directExecutor()
                .usePlaintext()
                .intercept(new ClientInterceptor() {
                    @Override
                    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                        return next.newCall(method, callOptions);
                    }
                })
                .build();
        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc.newBlockingStub(channel);
        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder().setRequestMessage("abc").build());
        LOGGER.info("client:{}", response);
    }
}
