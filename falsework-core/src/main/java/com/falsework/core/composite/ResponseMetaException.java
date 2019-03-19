package com.falsework.core.composite;

import com.falsework.core.generated.common.ResponseMeta;

/**
 * ResponseMeta 异常
 */
public class ResponseMetaException extends RuntimeException {
    private String errorCode;
    private String description;
    private ResponseMeta causeMeta;


    public ResponseMetaException(String errorCode, String description) {
        super(description, null, true, false);
        this.errorCode = errorCode;
        this.description = description;
    }

    public ResponseMetaException(String errorCode, String description, ResponseMeta cause) {
        super(description, null, true, false);
        this.errorCode = errorCode;
        this.description = description;
        this.causeMeta = cause;
    }

    public ResponseMetaException(String errorCode, String description, Throwable cause) {
        super(description, cause);
        this.errorCode = errorCode;
        this.description = description;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDescription() {
        return description;
    }

    public ResponseMeta getCauseMeta() {
        return causeMeta;
    }

    public ResponseMeta toResponseMeta() {
        ResponseMeta.Builder builder = ResponseMeta.newBuilder();
        builder.setErrCode(errorCode).setDetails(description);
        if (this.causeMeta != null) {
            builder.addCauses(this.causeMeta);
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "ResponseMetaException{" +
                "errorCode='" + errorCode + '\'' +
                ", description='" + description + '\'' +
                ", causeMeta=" + causeMeta +
                '}';
    }
}
