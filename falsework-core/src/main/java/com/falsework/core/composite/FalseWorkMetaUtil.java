package com.falsework.core.composite;

import com.falsework.core.generated.common.RequestMeta;
import com.falsework.core.generated.common.ResponseMeta;

import javax.annotation.Nonnull;

/**
 * Meta帮助工具
 */
public abstract class FalseWorkMetaUtil {
    public static final RequestMeta DEFAULT_REQUEST_META = RequestMeta.newBuilder()
            .build();
    public static final ResponseMeta DEFAULT_RESPONSE_META = ResponseMeta.newBuilder()
            .setErrCode("NA").build();


    /**
     * 请求Meta构建类
     *
     * @return
     */
    public static RequestMeta.Builder requestMetaBuilder() {
        return RequestMeta.newBuilder();
    }

    /**
     * 响应请求Meta构建类
     *
     * @return
     */
    public static ResponseMeta.Builder responseMetaBuilder() {
        return ResponseMeta.newBuilder();
    }

    /**
     * 检测响应是否为NA
     *
     * @param meta
     * @return
     */
    public static boolean isNA(@Nonnull ResponseMeta meta) {
        return "NA".equals(meta.getErrCode());
    }

}
