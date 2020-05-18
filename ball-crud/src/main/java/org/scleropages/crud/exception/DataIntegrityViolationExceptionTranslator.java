/**
 * Copyright 2001-2005 The Apache Software Foundation.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scleropages.crud.exception;

import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.ConstraintViolationException;
import org.jooq.tools.jdbc.JDBCUtils;
import org.scleropages.core.util.Exceptions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 将 spring 数据访问异常中的 {@link DataIntegrityViolationException}(违反约束，非空，唯一, 引用约束等) 异常转换为 {@link BizStateViolationException}。当前实现高度依赖hibernate
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class DataIntegrityViolationExceptionTranslator implements BizExceptionTranslator<BizStateViolationException>, InitializingBean {

    private final DataSource dataSource;

    private Dialect dialect;


    public DataIntegrityViolationExceptionTranslator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public BizStateViolationException translation(MethodInvocation invocation, Exception ex) {
        ConstraintViolationException hibernateCause = Exceptions.getCause(ex, ConstraintViolationException.class);
        String constraintName = null;
        if (null != hibernateCause) {
            constraintName = hibernateCause.getConstraintName();
        } else {
            SQLIntegrityConstraintViolationException jdbcCause = Exceptions.getCause(ex, SQLIntegrityConstraintViolationException.class);
            if (null != jdbcCause) {
                constraintName = dialect.getViolatedConstraintNameExtracter().extractConstraintName(jdbcCause);
            }
        }
        BizStateViolationException bizStateViolationException = new BizStateViolationException(ex.getMessage(), ex);
        bizStateViolationException.setConstraintViolations(new String[]{constraintName});
        return bizStateViolationException;
    }

    @Override
    public boolean support(Exception e) {
        return Exceptions.isCausedBy(e, DataIntegrityViolationException.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Connection connection = null;
        try {
            connection = DataSourceUtils.doGetConnection(dataSource);
            if (null != connection) {
                String hibernateDialectClass = JDBCUtils.dialect(connection).thirdParty().hibernateDialect();
                dialect = (Dialect) ClassUtils.forName(hibernateDialectClass, getClass().getClassLoader()).newInstance();
            }
        } catch (Throwable ex) {
            throw new IllegalStateException("can not create hibernate dialect.", ex);
        } finally {
            DataSourceUtils.doReleaseConnection(connection, dataSource);
        }
    }
}
