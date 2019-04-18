package com.falsework.governance.service;

import com.falsework.core.config.Props;
import com.falsework.core.generated.common.RequestMeta;
import com.falsework.governance.composite.ErrorCode;
import com.falsework.governance.config.PropsVars;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.tools.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 认证和授权
 */
@Singleton
public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private final Props props;
    private final Set<String> groups;
    private final HashMap<String, String> credential;
    private final String replicaToken;

    @Inject
    public AuthService(Props props) {
        this.props = props;
        String[] whiteGroups = this.props.getStringArray(PropsVars.REGISTER_GROUP_WHITE_LIST);
        this.groups = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(whiteGroups)));
        this.credential = new HashMap<>();
        this.replicaToken = this.props.getProperty(PropsVars.REGISTER_REPLICA_TOKEN);
    }

    /**
     * 组白名单
     *
     * @param group
     */
    public void groupAuthentication(String group) {
        if (!this.groups.contains(group)) {
            throw ErrorCode.PERMISSION_DENIED.asException("group denied");
        }
    }

    /**
     * 用户白名单
     *
     * @param meta
     */
    public void credentialAuthentication(RequestMeta meta) {
        LOGGER.warn("oops....,how.....,ignore");
    }

    /**
     * 认证
     *
     * @param meta
     */
    public void replicaAuthentication(RequestMeta meta) {
        if (!StringUtils.equals(this.replicaToken, meta.getAttributesMap().get("replica-token"))) {
            throw ErrorCode.UNAUTHENTICATED.asException();
        }
    }

    /**
     * 授权
     *
     * @param builder
     */
    public void replicaAuthorization(RequestMeta.Builder builder) {
        builder.putAttributes("replica-token", this.replicaToken);
    }
}
