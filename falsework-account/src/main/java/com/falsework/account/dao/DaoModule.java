package com.falsework.account.dao;

import com.falsework.core.aop.common.EnvAwareModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaoModule extends EnvAwareModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(DaoModule.class);


    @Override
    protected void configure() {
        LOGGER.info("Dao module init...");
        try {
            bind(UserDao.class).asEagerSingleton();
        } catch (Exception e) {
            this.addError(e);
        }
    }
}
