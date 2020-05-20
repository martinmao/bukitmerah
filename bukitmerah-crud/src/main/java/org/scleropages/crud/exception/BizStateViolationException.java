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

/**
 * 违反业务约束，例如不能将订单待支付状态直接变为已发货状态
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class BizStateViolationException extends BizException {

    public static final String CODE = CODE_AUTO_DETECT;

    public BizStateViolationException(String message) {
        super(CODE, message);
    }

    public BizStateViolationException(Throwable cause) {
        super(CODE, cause.getMessage(), cause);
    }

    public BizStateViolationException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public BizStateViolationException(String code, String message) {
        super(code, message);
    }

    public BizStateViolationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    @Override
    public final boolean isCodeAutoDetect() {
        return true;
    }
}
