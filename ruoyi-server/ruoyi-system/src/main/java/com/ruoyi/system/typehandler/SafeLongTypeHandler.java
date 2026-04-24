package com.ruoyi.system.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQLite 安全 Long 类型处理器
 * 
 * SQLite 为弱类型数据库，某列声明为 INTEGER 但仍可能存入字符串。
 * 当 MyBatis 将此类异常值映射到 Java Long 时会抛出 NumberFormatException。
 * 该 Handler 在读取阶段进行防御：非数字字符串转为 0L 并记录警告日志。
 * 
 * @author lingdoc
 */
@MappedTypes(Long.class)
@MappedJdbcTypes({JdbcType.BIGINT, JdbcType.INTEGER, JdbcType.NUMERIC})
public class SafeLongTypeHandler extends BaseTypeHandler<Long>
{
    private static final Logger log = LoggerFactory.getLogger(SafeLongTypeHandler.class);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType) throws SQLException
    {
        ps.setLong(i, parameter);
    }

    @Override
    public Long getNullableResult(ResultSet rs, String columnName) throws SQLException
    {
        Object value = rs.getObject(columnName);
        return convertSafely(value, columnName);
    }

    @Override
    public Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException
    {
        Object value = rs.getObject(columnIndex);
        return convertSafely(value, "index:" + columnIndex);
    }

    @Override
    public Long getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
    {
        Object value = cs.getObject(columnIndex);
        return convertSafely(value, "callable:" + columnIndex);
    }

    private Long convertSafely(Object value, String columnRef)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        try
        {
            String trimmedValue = value.toString().trim();
            return Long.parseLong(trimmedValue);
        }
        catch (NumberFormatException e)
        {
            String valueStr = value == null ? "null" : value.toString();
            String valueType = value == null ? "null" : value.getClass().getSimpleName();
            log.warn("SafeLongTypeHandler 类型转换失败: 列 [{}] 期望 Long 但收到非数字值 '{}' (类型: {}, 长度: {}). "
                    + "已按安全方式转为 0L. Stack: {}", 
                    columnRef, valueStr, valueType, valueStr.length(), 
                    Thread.currentThread().getStackTrace()[2]);
            
            // 额外的严格模式日志：记录完整堆栈，便于后续排查
            if (log.isDebugEnabled()) {
                StringBuilder stackTrace = new StringBuilder();
                for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                    if (element.getClassName().contains("com.ruoyi")) {
                        stackTrace.append("\n  at ").append(element);
                    }
                }
                log.debug("SafeLongTypeHandler 完整堆栈: {}", stackTrace);
            }
            
            return 0L;
        }
    }
}
