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

import com.google.common.collect.Lists;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.scleropages.core.mapper.JsonMapper2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

import javax.validation.ConstraintViolationException;
import java.util.List;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ExceptionTranslationInterceptor implements MethodInterceptor, InitializingBean {

    private List<ExceptionTranslator> exceptionTranslators;


    @Override
    public void afterPropertiesSet() throws Exception {
        if (null == exceptionTranslators)
            exceptionTranslators = Lists.newArrayList();
        exceptionTranslators.add(new BizExceptionTranslator());
    }


    /**
     * 格式化错误日志输出，减少stack异常打印.调用端捕获该类型异常不应该打印stack.
     * 对参数检查类异常，warn记录并记录参数内容，并转换为统一的 {@link BizParamViolationException}.
     * 对业务检查异常，warn记录
     */
    private class BizExceptionTranslator implements ExceptionTranslator {

        protected final Logger logger = LoggerFactory.getLogger(getClass());

        private final Class[] bizParamViolationExceptions = new Class[]{
                IllegalArgumentException.class,
                ConstraintViolationException.class,
                BizParamViolationException.class
        };

        private final Class[] bizViolationExceptions = new Class[]{
                BizViolationException.class
        };

        @Override
        public Exception translation(MethodInvocation invocation, Exception e) {
            Object[] arguments = invocation.getArguments();
            if (supportBizParamViolationException(e)) {
                logger.warn("{}: BizParamViolationException: [{}]. from: [{}] with arguments: {}", BizParamViolationException.CODE,
                        e.getMessage(), invocation.getMethod().toGenericString(), JsonMapper2.toJson(arguments));
                return e instanceof BizParamViolationException ? e : new BizParamViolationException(e.getMessage(), e, arguments);
            }
            if (supportBizViolationExceptions(e)) {
                logger.warn("{}: BizViolationException: [{}]. from: [{}] with arguments: {}", BizViolationException.CODE,
                        e.getMessage(), invocation.getMethod().toGenericString(), JsonMapper2.toJson(arguments));
                return e instanceof BizViolationException ? e : new BizViolationException(e.getMessage(), e);
            }
            return e;
        }

        protected boolean supportBizParamViolationException(Throwable e) {
            for (Class clazz : bizParamViolationExceptions) {
                if (ClassUtils.isAssignableValue(clazz, e))
                    return true;
            }
            Throwable cause = e.getCause();//参数错误异常容易被隐藏在底层调用栈中（持久层校验），会被框架包装为各种xxx access exception.
            if (null != cause)
                return supportBizParamViolationException(cause);
            return false;
        }

        protected boolean supportBizViolationExceptions(Exception e) {
            for (Class clazz : bizViolationExceptions) {//违反业务规则校验往往只在manager层中抛出，一般不会被框架包装为其他异常
                if (ClassUtils.isAssignableValue(clazz, e))
                    return true;
            }
            return false;
        }

        @Override
        public boolean support(Exception e) {
            return supportBizParamViolationException(e) || supportBizViolationExceptions(e);
        }
    }


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (Exception e) {
            throw translationException(invocation, e);
        } finally {

        }
    }

    protected Exception translationException(final MethodInvocation invocation, final Exception e) {
        for (ExceptionTranslator exceptionTranslator : exceptionTranslators) {
            if (exceptionTranslator.support(e)) {
                return exceptionTranslator.translation(invocation, e);
            }
        }
        return e;
    }

    public void setExceptionTranslators(List<ExceptionTranslator> exceptionTranslators) {
        this.exceptionTranslators = exceptionTranslators;
    }

    public List<ExceptionTranslator> getExceptionTranslators() {
        if (null == exceptionTranslators)
            exceptionTranslators = Lists.newArrayList();
        return exceptionTranslators;
    }
}
