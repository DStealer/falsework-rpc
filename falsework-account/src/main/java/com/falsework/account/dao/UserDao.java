package com.falsework.account.dao;


import com.falsework.account.model.Testdb;
import com.falsework.account.model.tables.records.TUserRecord;
import org.jooq.DSLContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

public class UserDao {
    private final DSLContext context;

    @Inject
    public UserDao(@Named("default") DSLContext context) {
        this.context = context;
    }

    /**
     * 根据用户名密码查询
     *
     * @param username
     * @param password
     * @return
     */
    public Optional<TUserRecord> findByUsernamePassword(String username, String password) {
        return this.context.fetchOptional(Testdb.TESTDB.T_USER, Testdb.TESTDB.T_USER.USERNAME.eq(username));
    }
}
