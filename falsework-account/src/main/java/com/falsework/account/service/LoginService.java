package com.falsework.account.service;

import com.falsework.account.dao.UserDao;
import com.falsework.account.generated.LoginGrpc;
import com.falsework.account.generated.LoginReply;
import com.falsework.account.generated.LoginRequest;
import com.falsework.account.model.tables.records.TUserRecord;
import com.falsework.core.composite.FalseWorkMetaUtil;
import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 登陆请求
 */
public class LoginService extends LoginGrpc.LoginImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);
    private final UserDao userDao;

    @Inject
    public LoginService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginReply> responseObserver) {
        LOGGER.info("user login:{}={}", request.getUsername(), request.getPassword());
        Optional<TUserRecord> record = this.userDao.findByUsernamePassword(request.getUsername(), request.getPassword());
        LoginReply.Builder builder = LoginReply.newBuilder();
        if (record.isPresent()) {
            LOGGER.info("user found,login success");
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META);
            builder.setId(record.get().getId())
                    .setUsername(record.get().getUsername())
                    .setStatus(record.get().getStatus());
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } else {
            LOGGER.info("user not found,login failed");
            responseObserver.onNext(builder
                    .setMeta(FalseWorkMetaUtil.responseMetaBuilder()
                            .setErrCode(""))
                    .build());
            responseObserver.onCompleted();
        }
    }
}
