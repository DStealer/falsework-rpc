package com.falsework.census.composite;

import com.falsework.core.composite.ResponseMetaException;
import com.falsework.core.generated.common.ResponseMeta;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务错误码定义
 */
public enum ErrorCode {

    NA("NA", "OK"), //正常返回

    INVALID_ARGUMENT("CS1001", "Invalid argument"),//参数错误

    NOT_FOUND("CS1002", "Not found"),//数据未找到

    ALREADY_EXISTS("CS1003", "Already exists"),//数据已存在

    PERMISSION_DENIED("CS1004", "Permission denied"),//没有权限

    UNAUTHENTICATED("CS1005", "Unauthenticated"),//未授权

    UNAVAILABLE("CS1006", "Unavailable"),//服务不可用

    INVALID_STATUS("CS1007", "Invalid status"),//状态不正常


    INTERNAL("CS9999", "Internal error");//未定义内部错误
    private static final Map<ErrorCode, ResponseMeta> metaByErrorCode;
    private static final Map<ErrorCode, ResponseMetaException> exceptionByErrorCode;

    static {
        Map<ErrorCode, ResponseMeta> realMetaMap = new LinkedHashMap<>();
        Map<ErrorCode, ResponseMetaException> realExceptionMap = new LinkedHashMap<>();
        for (ErrorCode ec : ErrorCode.values()) {
            realMetaMap.put(ec, ResponseMeta.newBuilder().
                    setErrCode(ec.code).setDetails(ec.description).build());
            realExceptionMap.put(ec, new ResponseMetaException(ec.code, ec.description));
        }
        exceptionByErrorCode = Collections.unmodifiableMap(realExceptionMap);
        metaByErrorCode = Collections.unmodifiableMap(realMetaMap);
    }

    private final String code;
    private final String description;


    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 返回 response meta
     *
     * @return
     */
    public ResponseMeta toResponseMeta() {
        return metaByErrorCode.get(this);
    }

    /**
     * 返回 response meta
     *
     * @param description 自定义描述信息
     * @return
     */
    public ResponseMeta toResponseMeta(String description) {
        return ResponseMeta.newBuilder()
                .setErrCode(this.code)
                .setDetails(description)
                .build();
    }

    /**
     * 返回 response meta
     *
     * @param description 自定义描述信息
     * @param cause       原因
     * @return
     */
    public ResponseMeta toResponseMeta(String description, ResponseMeta cause) {
        return ResponseMeta.newBuilder()
                .setErrCode(this.code)
                .setDetails(description)
                .addCauses(cause)
                .build();
    }

    /**
     * 返回 ResponseMetaException
     *
     * @return
     */
    public ResponseMetaException asException() {
        return exceptionByErrorCode.get(this);
    }

    /**
     * 返回 ResponseMetaException
     *
     * @param description 自定义描述信息
     * @return
     */
    public ResponseMetaException asException(String description) {
        return new ResponseMetaException(this.code, description);
    }

    /**
     * 返回 ResponseMetaException
     *
     * @param description 自定义描述信息
     * @param cause       原因
     * @return
     */
    public ResponseMetaException asException(String description, ResponseMeta cause) {
        return new ResponseMetaException(this.code, description, cause);
    }


    /**
     * 返回 ResponseMetaException
     *
     * @param description 自定义描述信息
     * @param cause       原因
     * @return
     */
    public ResponseMetaException asException(String description, Throwable cause) {
        return new ResponseMetaException(this.code, description, cause);
    }
}
