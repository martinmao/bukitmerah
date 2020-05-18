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

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解可应用于业务类或业务方法上(仅限注解了@{@link org.springframework.stereotype.Service}的类)，将业务代码相关的 java unchecked 异常转换为 {@link BizException} 体系.
 * 注意，用户代码内抛出的 BizException是有效的.
 * 建议的命名规范为:code-prefix(2位数字)+code-service(2位数字)+code-method(2位数字)+code-throwable(2位数字)
 * <pre>
 * eg:
 *
 * application.properties
 *      #订单中心异常码前缀
 *      application.biz-exception.code-prefix=30
 *
 * &#64;Component
 * class CarCreationBizExceptionTransformer implements BizExceptionTransformer{
 *     public BizException apply(Throwable t){
 *         return new CarCreationBizException(t);
 *     }
 * }
 *
 * &#64;Service
 * &#64;BizError(code="10")
 * class CarService{
 *
 *      //carService.deleteCar(null)---->will throws BizException with code: 3010xx.
 *      public void deleteCar(Long id){
 *          Assert.notNull(id,"id is required.");
 *      }
 *      //carService.updateCar(null);--->will throws BizException with code: 301001.
 *      &#64;BizError(code="01")
 *      public void updateCar(Car car){
 *          Assert.notNull(car,"given car must not be null.");
 *      }
 *      //carService.createCar(null);--->will throws CarCreationBizException with code: 301002.
 *      &#64;BizError(code="02",bizExceptionTransformer=CarCreationBizExceptionTransformer.class)
 *      public void createCar(Car car){
 *          Assert.notNull(car,"given car must not be null.");
 *      }
 *      ....
 *      //carService.createCar(new BrokenCar());--->will throws CarCreationBizException with code: 30100201.
 *      &#64;BizError(code="02",bizExceptionTransformer=CarCreationBizExceptionTransformer.class)
 *      public void createCarMore(Car car){
 *          Assert.notNull(car,"given car must not be null.");
 *          if(!car.stateValid()){
 *              throw new CarCreationBizException("01","car state is invalid.");
 *          }
 *      }
 *      ....
 * }
 *
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 * @see BizExceptionTransformer
 * @see Jsr303ConstraintViolationTranslator
 * @see DataIntegrityViolationExceptionTranslator
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface BizError {

    String DEFAULT_UNKNOWN_CODE = "XX";

    /**
     * 错误码
     *
     * @return
     */
    @AliasFor("code")
    String value() default DEFAULT_UNKNOWN_CODE;

    /**
     * bean type of {@link BizExceptionTransformer} from spring context.
     *
     * @return
     */
    Class bizExceptionTransformer() default BizExceptionNoTransformer.class;

    /**
     * 错误码
     *
     * @return
     */
    @AliasFor("value")
    String code() default DEFAULT_UNKNOWN_CODE;
}