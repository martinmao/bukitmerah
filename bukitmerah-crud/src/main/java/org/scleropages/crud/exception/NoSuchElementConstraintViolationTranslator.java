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
import org.apache.commons.lang3.StringUtils;
import org.scleropages.core.util.Exceptions;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class NoSuchElementConstraintViolationTranslator implements BizExceptionTranslator<BizStateViolationException> {
    @Override
    public BizStateViolationException translation(MethodInvocation invocation, Exception e) {
        BizStateViolationException bizStateViolationException = new BizStateViolationException(e);
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (null != stackTrace && stackTrace.length > 2) {
            if (Objects.equals(stackTrace[0].getClassName(), Optional.class.getName())) {
                StackTraceElement element = stackTrace[1];
                bizStateViolationException.setConstraintViolations(new String[]{StringUtils.substringAfterLast(element.getClassName(), ".") + "." + element.getMethodName()});
            }
        }
        return bizStateViolationException;
    }

    @Override
    public boolean support(Exception e) {
        return Exceptions.isCausedBy(e, NoSuchElementException.class);
    }
}
