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

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.NestedRuntimeException;

import java.beans.Transient;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 业务异常基类
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class BizException extends NestedRuntimeException {

    protected static final String CODE_AUTO_DETECT = "N/A";

    private String code;

    private MethodInvocation methodInvocation;

    private String[] constraintViolations;

    public BizException(String message) {
        this(CODE_AUTO_DETECT, message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public BizException(String message, Throwable cause) {
        this(CODE_AUTO_DETECT, message, cause);
    }


    public BizException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 返回违规约束
     *
     * @return
     */
    @Transient
    @JsonIgnore
    @JSONField(serialize = false)
    public String[] getConstraintViolations() {
        return constraintViolations;
    }

    /**
     * 返回业务异常方法
     *
     * @return
     */
    @Transient
    @JsonIgnore
    @JSONField(serialize = false)
    public Method getInvocationMethod() {
        return methodInvocation.getMethod();
    }

    /**
     * 返回业务异常方法参数
     *
     * @return
     */
    @Transient
    @JsonIgnore
    @JSONField(serialize = false)
    public Object[] getInvocationArguments() {
        return methodInvocation.getArguments();
    }


    /**
     * user code never call this method.该函数用于从 {@link BizError} 获取错误码并设置.
     *
     * @param code
     */
    protected void setCode(String code) {
        this.code = code;
    }

    /**
     * user code never call this method.该函数主要用于从 {@link javax.validation.ConstraintViolationException} 以及
     * {@link org.springframework.dao.DataIntegrityViolationException}等 提取必要的错误参数或特定约束字段说明等.
     *
     * @param bizProperties
     */
    public void setConstraintViolations(String[] constraintViolations) {
        this.constraintViolations = constraintViolations;
    }

    /**
     * user code never call this method.该函数用于将方法执行上下绑定到异常对象便于框架处理.
     *
     * @param methodInvocation
     */
    protected void setMethodInvocation(MethodInvocation methodInvocation) {
        this.methodInvocation = methodInvocation;
    }

    /**
     * return true if current code already set. {@link BizExceptionTranslationInterceptor.BizExceptionTranslator} don't apply code from @BizError.
     *
     * @return
     */
    @Transient
    @JsonIgnore
    @JSONField(serialize = false)
    public boolean isCodeAutoDetect() {
        return Objects.equals(getCode(), CODE_AUTO_DETECT);
    }
}
