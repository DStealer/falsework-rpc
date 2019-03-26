package com.falsework.governance.service;

import com.falsework.core.config.Props;
import com.falsework.core.generated.common.RequestMeta;
import com.falsework.governance.composite.ErrorCode;
import com.falsework.governance.config.PropsVars;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.HashSet;

@Singleton
public class WhiteListService {
    private final Props props;
    private final HashSet<String> group;
    private final HashMap<String, String> credential;

    @Inject
    public WhiteListService(Props props) {
        this.props = props;
        this.group = Sets.newHashSet((String[]) (this.props.getStringArray(PropsVars.REGISTER_GROUP_WHITE_LIST)));
        this.credential = new HashMap<>();
    }

    /**
     * 组白名单
     *
     * @param group
     */
    public void groupWhiteCheck(String group) {
        if (!this.group.contains(group)) {
            throw ErrorCode.PERMISSION_DENIED.asException("group denied");
        }
    }

    /**
     * 用户白名单
     *
     * @param meta
     */
    public void credentialCheck(RequestMeta meta) {
        String userName = meta.getAttributesOrDefault("user", null);
        if (userName == null) {
            throw ErrorCode.UNAUTHENTICATED.asException("user is empty");
        }
        String credential = meta.getAttributesOrDefault("credential", null);
        if (credential == null) {
            throw ErrorCode.UNAUTHENTICATED.asException("credential is empty");
        }
        if (!credential.equals(this.credential.get(userName))) {
            throw ErrorCode.UNAUTHENTICATED.asException("user or credential is wrong");
        }
    }
}
