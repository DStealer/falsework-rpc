package com.falsework.governance.composite;

import com.falsework.core.composite.ResponseMetaException;
import com.falsework.core.generated.common.ResponseMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务错误码定义
 */
public enum ErrorCode {

    NA("NA", "OK"), //正常返回

    INVALID_ARGUMENT("GA1001", "Invalid argument"),//参数错误

    NOT_FOUND("GA1002", "Not found"),//数据未找到

    ALREADY_EXISTS("GA1003", "Already exists"),//数据已存在

    PERMISSION_DENIED("GA1004", "Permission denied"),//没有权限

    UNAUTHENTICATED("GA1005", "Unauthenticated"),//未授权

    UNAVAILABLE("GA1006", "Unavailable"),//服务不可用

    INVALID_STATUS("GA1007", "Invalid status"),//状态不正常


    INTERNAL("GA9999", "Internal error");//未定义内部错误

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
        return ErrCodeInternal.RESPONSE_META_MAP.get(this);
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
        return ErrCodeInternal.RESPONSE_META_EXCEPTION_MAP.get(this);
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

    /**
     * 使用静态类存储减少新建对象数量
     */
    private static class ErrCodeInternal {
        private static final Map<ErrorCode, ResponseMeta> RESPONSE_META_MAP = new HashMap<>();
        private static final Map<ErrorCode, ResponseMetaException> RESPONSE_META_EXCEPTION_MAP = new HashMap<>();

        static {
            for (ErrorCode ec : ErrorCode.values()) {
                RESPONSE_META_MAP.put(ec, ResponseMeta.newBuilder()
                        .setErrCode(ec.code)
                        .setDetails(ec.description)
                        .build());
                RESPONSE_META_EXCEPTION_MAP.put(ec, new ResponseMetaException(ec.code, ec.description));
            }
        }
    }

}
