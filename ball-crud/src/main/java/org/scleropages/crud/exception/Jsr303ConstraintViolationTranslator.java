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
import org.scleropages.core.util.Exceptions;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 实现了对 jsr-303 javax.validator 中的校验错误转换为 {@link BizParamViolationException}
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class Jsr303ConstraintViolationTranslator implements BizExceptionTranslator<BizParamViolationException> {


    @Override
    public BizParamViolationException translation(MethodInvocation invocation, Exception e) {
        ConstraintViolationException ex = (ConstraintViolationException) e;
        Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
        String[] constraintProperties = new String[constraintViolations.size()];
        int i = 0;
        for (ConstraintViolation constraintViolation :
                constraintViolations) {
            constraintProperties[i] = constraintViolation.getMessageTemplate()+constraintViolation.getPropertyPath().toString();
            i++;

        }
        BizParamViolationException bizParamViolationException = new BizParamViolationException(ex.getMessage(), ex);
        bizParamViolationException.setConstraintViolations(constraintProperties);
        return bizParamViolationException;
    }

    @Override
    public boolean support(Exception e) {
        return Exceptions.isCausedBy(e, ConstraintViolationException.class);
    }
}
