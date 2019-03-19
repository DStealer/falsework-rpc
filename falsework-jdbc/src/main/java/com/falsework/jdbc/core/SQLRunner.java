package com.falsework.jdbc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.sql.*;
import java.util.*;


/**
 * 数据库连接包装对象 非线程安全
 * Created by lishiwu on 2016/7/15.
 */
public class SQLRunner implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLRunner.class);
    //每次处理的数据数据量
    private static final int BATCH_OPERATION_SIZE = 5000;
    private Connection connection;

    /**
     * 获取只读连接
     *
     * @param connection
     * @throws SQLException
     */
    public SQLRunner(Connection connection) throws SQLException {
        this(connection, false);
    }

    public SQLRunner(Connection connection, boolean tx) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            this.connection = connection;
            if (tx) {
                this.connection.setAutoCommit(false);
            } else {
                this.connection.setReadOnly(true);
            }
        } else {
            throw new InvalidParameterException("Connection cant't be null or closed");
        }
    }

    /**
     * 获取只读连接
     *
     * @param dataSource
     * @return
     * @throws SQLException
     */
    public static SQLRunner readonly(DataSource dataSource) throws SQLException {


        return new SQLRunner(dataSource.getConnection(), false);

    }

    /**
     * 获取🦐连接
     *
     * @param dataSource
     * @return
     * @throws SQLException
     */
    public static SQLRunner tx(DataSource dataSource) throws SQLException {

        return new SQLRunner(dataSource.getConnection(), true);

    }

    /**
     * 回滚事务
     */
    public void rollback() {
        try {
            this.connection.rollback();
        } catch (SQLException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    /**
     * 提交事务
     */
    public void commit() {
        try {
            this.connection.commit();
        } catch (SQLException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    /**
     * 关闭数据库连接资源
     */
    public void closeQuietly() {
        this.closeQuietly(this.connection, null, null);
    }

    /**
     * 提交并关闭
     */
    public void commitAndClose() {
        try {
            this.connection.commit();
        } catch (SQLException e) {
            LOGGER.warn(e.getMessage(), e);
        } finally {
            this.closeQuietly(this.connection, null, null);
        }
    }

    /**
     * 回滚并关闭
     */
    public void rollbackAndClose() {
        try {
            this.connection.rollback();
        } catch (SQLException e) {
            LOGGER.warn(e.getMessage(), e);
        } finally {
            this.closeQuietly(this.connection, null, null);
        }
    }

    /**
     * 从数据库中读取数据
     *
     * @param statement
     * @param handler
     * @return
     * @throws SQLException
     */
    public <T> Optional<T> queryOne(SQLStatement statement, RSHandler<T> handler) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = statement.getSql();

            if (!sql.toUpperCase().contains("LIMIT ") && sql.toUpperCase().contains("SELECT ")) {
                sql += " LIMIT 1";
            }
            preparedStatement = this.connection.prepareStatement(sql);
            this.fillStatement(preparedStatement, statement.getParamList());
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(handler.parseFrom(resultSet));
            } else {
                return Optional.empty();
            }
        } finally {
            this.closeQuietly(null, preparedStatement, resultSet);
        }
    }

    /**
     * 从数据库中读取数据
     *
     * @param statement
     * @return
     * @throws SQLException
     */
    public Optional<Record> queryOne(SQLStatement statement) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = statement.getSql();

            if (!sql.toUpperCase().contains("LIMIT ") && sql.toUpperCase().contains("SELECT ")) {
                sql += " LIMIT 1";
            }
            preparedStatement = this.connection.prepareStatement(sql);
            this.fillStatement(preparedStatement, statement.getParamList());
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                Map<String, Integer> indexMap = new HashMap<>();
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    indexMap.put(metaData.getColumnLabel(i + 1), i);
                }
                Object[] data = new Object[metaData.getColumnCount()];
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    data[i] = resultSet.getObject(i + 1);
                }
                return Optional.of(new Record(data, indexMap));
            } else {
                return Optional.empty();
            }
        } finally {
            this.closeQuietly(null, preparedStatement, resultSet);
        }
    }

    /**
     * 从数据库中读取数据
     *
     * @param statement
     * @param handler
     * @return
     * @throws SQLException
     */
    public <T> List<T> queryList(SQLStatement statement, RSHandler<T> handler) throws SQLException {
        List<T> list = new LinkedList<>();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(statement.getSql());
            this.fillStatement(preparedStatement, statement.getParamList());
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                list.add(handler.parseFrom(resultSet));
            }
            return list;
        } finally {
            this.closeQuietly(null, preparedStatement, resultSet);
        }
    }

    /**
     * 从数据库中读取数据
     *
     * @param statement
     * @return
     * @throws SQLException
     */
    public List<Record> queryList(SQLStatement statement) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(statement.getSql());
            this.fillStatement(preparedStatement, statement.getParamList());
            resultSet = preparedStatement.executeQuery();
            return recordExtractor(resultSet);
        } finally {
            this.closeQuietly(null, preparedStatement, resultSet);
        }
    }

    /**
     * 数据保存
     *
     * @param t      数据
     * @param parser 参数处理
     * @return
     */
    public <T> int persist(T t, PSParser<T> parser) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.connection.prepareStatement(parser.getSql());
            parser.parseTo(preparedStatement, t);
            return preparedStatement.executeUpdate();
        } finally {
            this.closeQuietly(null, preparedStatement, null);
        }
    }

    /**
     * 数据批量保存
     *
     * @param dataList 数据
     * @param parser   参数处理
     * @return
     */
    public <T> int persist(List<T> dataList, PSParser<T> parser) throws SQLException {
        PreparedStatement preparedStatement = null;
        int hit = 0;
        try {
            preparedStatement = this.connection.prepareStatement(parser.getSql());
            //批量处理
            for (int i = 0, len = dataList.size(); i < len; i++) {
                parser.parseTo(preparedStatement, dataList.get(i));
                preparedStatement.addBatch();
                if (i % BATCH_OPERATION_SIZE == 0) {
                    hit += sumArray(preparedStatement.executeBatch());
                    preparedStatement.clearBatch();
                }
            }
            hit += sumArray(preparedStatement.executeBatch());
            preparedStatement.clearBatch();
            return hit;
        } finally {
            this.closeQuietly(null, preparedStatement, null);
        }
    }

    /**
     * @param ints
     * @return
     */
    private int sumArray(int[] ints) {
        int sum = 0;
        for (int is : ints) {
            sum += is;
        }
        return sum;
    }


    /**
     * 执行数据创建并获取自动生成的key,返回生成的key列表
     *
     * @param statement
     * @return
     * @throws SQLException
     */
    public Optional<BigDecimal> persistAndGetKey(SQLStatement statement) throws SQLException {
        if (!statement.getSql().toLowerCase().trim().startsWith("insert")) {
            throw new SQLException("Not a insert statement");
        }
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(statement.getSql(), PreparedStatement.RETURN_GENERATED_KEYS);
            this.fillStatement(preparedStatement, statement.getParamList());
            preparedStatement.executeUpdate();
            resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                return Optional.of(resultSet.getBigDecimal(1));
            } else {
                return Optional.empty();
            }
        } finally {
            this.closeQuietly(null, preparedStatement, resultSet);
        }
    }

    /**
     * 执行数据创建并获取自动生成的key,返回生成的key列表
     *
     * @param statement
     * @return
     * @throws SQLException
     */
    public List<BigDecimal> persistAndGetKeys(SQLStatement statement) throws SQLException {
        if (!statement.getSql().toLowerCase().trim().startsWith("insert")) {
            throw new SQLException("Not a insert statement");
        }
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(statement.getSql(), PreparedStatement.RETURN_GENERATED_KEYS);
            this.fillStatement(preparedStatement, statement.getParamList());
            preparedStatement.executeUpdate();
            resultSet = preparedStatement.getGeneratedKeys();
            //由于大多数的insert操作为单条，但是存在多条的可能性
            List<BigDecimal> keyList = new ArrayList<>(1);
            while (resultSet.next()) {
                keyList.add(resultSet.getBigDecimal(1));
            }
            return keyList;
        } finally {
            this.closeQuietly(null, preparedStatement, resultSet);
        }
    }

    /**
     * 执行数据更新
     *
     * @param statement
     * @throws SQLException
     */
    public int persistOrUpdate(SQLStatement statement) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.connection.prepareStatement(statement.getSql());
            this.fillStatement(preparedStatement, statement.getParamList());
            return preparedStatement.executeUpdate();
        } finally {
            this.closeQuietly(null, preparedStatement, null);
        }
    }

    /**
     * 执行数据更新并检测
     *
     * @param statement
     * @throws SQLException
     */
    public void checkedPersistOrUpdate(SQLStatement statement) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.connection.prepareStatement(statement.getSql());
            this.fillStatement(preparedStatement, statement.getParamList());
            if (1 != preparedStatement.executeUpdate()) {
                throw new SQLException("update count dismiss");
            }
        } finally {
            this.closeQuietly(null, preparedStatement, null);
        }
    }

    /**
     * 执行数据更新
     *
     * @param statements
     * @throws SQLException
     */
    public int[] persistOrUpdate(List<SQLStatement> statements) throws SQLException {
        int[] hits = new int[statements.size()];
        SQLStatement statement;
        PreparedStatement pstmt = null;
        for (int i = 0; i < statements.size(); i++) {
            statement = statements.get(i);
            try {
                pstmt = this.connection.prepareStatement(statement.getSql());
                this.fillStatement(pstmt, statement.getParamList());
                hits[i] = pstmt.executeUpdate();
            } finally {
                this.closeQuietly(null, pstmt, null);
            }
        }
        return hits;
    }

    /**
     * 执行数据更新
     *
     * @param statements
     * @throws SQLException
     */
    public void checkedPersistOrUpdate(List<SQLStatement> statements) throws SQLException {

        SQLStatement statement;
        PreparedStatement pstmt = null;
        for (int i = 0; i < statements.size(); i++) {
            statement = statements.get(i);
            try {
                pstmt = this.connection.prepareStatement(statement.getSql());
                this.fillStatement(pstmt, statement.getParamList());
                if (1 != pstmt.executeUpdate()) {
                    throw new SQLException("update count dismiss");
                }
            } finally {
                this.closeQuietly(null, pstmt, null);
            }
        }
    }

    /**
     * 关闭数据库资源
     *
     * @param connection
     * @param statement
     * @param resultSet
     */
    private void closeQuietly(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        if (connection != null) {
            try {
                //此处需要恢复原始状态
                if (connection.isReadOnly()) connection.setReadOnly(false);
                if (!connection.getAutoCommit()) connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * 设置PreparedStatement参数
     *
     * @param stmt
     * @param params
     * @throws SQLException
     */
    private void fillStatement(PreparedStatement stmt, List<Object> params)
            throws SQLException {
        ParameterMetaData pmd = stmt.getParameterMetaData();
        int stmtCount = pmd.getParameterCount();
        int paramsCount = params == null ? 0 : params.size();
        if (stmtCount != paramsCount) {
            throw new SQLException("Wrong number of parameters: expected " + stmtCount + ", was given " + paramsCount);
        }
        if (stmtCount == 0 || paramsCount == 0) {
            return;
        }
        for (int i = 0; i < params.size(); i++) {
            int type = pmd.getParameterType(i + 1);
            Object param = params.get(i);
            if (param == null) {
                stmt.setNull(i + 1, type);
            } else {
                stmt.setObject(i + 1, params.get(i));
            }
        }
    }

    /**
     * 从数据库中读取数据,并返回总记录数,必须 为 含有limit的非do查询
     *
     * @param statement
     * @param handler
     * @return
     * @throws SQLException
     */
    public <T> PageData<List<T>> queryPageList(SQLStatement statement, SQLStatement count, RSHandler<T> handler) throws SQLException {
        PageData<List<T>> pageData = new PageData<>();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(statement.getSql());
            this.fillStatement(preparedStatement, statement.getParamList());
            resultSet = preparedStatement.executeQuery();
            List<T> list = new LinkedList<>();
            while (resultSet.next()) {
                list.add(handler.parseFrom(resultSet));
            }
            pageData.setData(list);
        } finally {
            this.closeQuietly(null, preparedStatement, resultSet);
        }
        Number total = this.queryOne(count, SimpleResult.NumberResult)
                .orElseThrow(() -> new SQLException("Can't execute count"));
        pageData.setTotal(total.longValue());
        return pageData;
    }

    /**
     * 从数据库中读取数据,并返回总记录数,必须 为 含有limit的非do查询
     *
     * @param statement
     * @return
     * @throws SQLException
     */
    public PageData<List<Record>> queryPageListRecord(SQLStatement statement, SQLStatement count) throws SQLException {
        PageData<List<Record>> pageData = new PageData<>();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(statement.getSql());
            this.fillStatement(preparedStatement, statement.getParamList());
            resultSet = preparedStatement.executeQuery();

            pageData.setData(recordExtractor(resultSet));
        } finally {
            this.closeQuietly(null, preparedStatement, resultSet);
        }
        Number total = this.queryOne(count, SimpleResult.NumberResult)
                .orElseThrow(() -> new SQLException("Can't execute count"));
        pageData.setTotal(total.longValue());
        return pageData;
    }

    /**
     * record 集合
     *
     * @param resultSet
     * @return
     * @throws SQLException
     */
    private List<Record> recordExtractor(ResultSet resultSet) throws SQLException {
        LinkedList<Record> list = new LinkedList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < columnCount; i++) {
            indexMap.put(metaData.getColumnLabel(i + 1), i);
        }
        while (resultSet.next()) {
            Object[] data = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                data[i] = resultSet.getObject(i + 1);
            }
            list.add(new Record(data, indexMap));
        }
        return list;
    }

    @Override
    public void close() throws Exception {
        this.closeQuietly();
    }
}
