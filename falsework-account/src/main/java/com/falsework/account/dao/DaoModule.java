package com.falsework.account.dao;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaoModule extends AbstractModule {
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
