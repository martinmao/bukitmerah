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
package org.scleropages.openapi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 可定义在类，方法，参数, 属性上，忽略目标元素作为api构成.适用范围如下所示:
 *
 * <pre>
 * &#64;ApiIgnore//忽略整个controller所有元素
 * &#64;Controller
 * public class ItemAction{
 *  ....
 * }
 * ------------------------------------------------------------------------------------------------------
 * &#64;Controller
 * public class ItemAction{
 *
 *      &#64;ApiIgnore//忽略该method
 *      public void deleteItem(Long id){
 *          ....
 *      }
 * }
 * ------------------------------------------------------------------------------------------------------
 * &#64;Controller
 * public class ItemAction{
 *
 *      //忽略 currentUser参数
 *      public void deleteItem(Long id,&#64;ApiIgnore String currentUser){
 *          ....
 *      }
 * }
 * ------------------------------------------------------------------------------------------------------
 * &#64;Controller
 * public class OrderAction{
 *
 *      //仅忽略Order.state属性
 *      public void createOrder(@RequestBody Order order){
 *          ....
 *      }
 * }
 * public class Order{
 *     &#64;ApiIgnore
 *     private Integer state;
 * }
 * ------------------------------------------------------------------------------------------------------
 * &#64;Controller
 * public class OrderAction{
 *
 *      //仅忽略Order.id属性
 *      public void createOrder(@RequestBody &#64;ApiIgnore({Order.Create.class}) Order order){
 *          ....
 *      }
 *      //仅忽略Order.createTime属性
 *      public void updateOrder(@RequestBody &#64;ApiIgnore({Order.Update.class}) Order order){
 *          ....
 *      }
 *
 *      &#64;ApiIgnore({Order.Get.class})//返回值忽略Order.id属性
 *      public Order getOrder(Long orderId){
 *
 *      }
 *
 * }
 * public class Order{
 *     public static interface Create{}
 *     public static interface Update{}
 *     public static interface Get{}
 *     &#64;ApiIgnore({Create.class,Get.class})
 *     private Long id;
 *     &#64;ApiIgnore({Update.class})
 *     private Date createTime;
 * }
 * ------------------------------------------------------------------------------------------------------
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Target({TYPE, METHOD, FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiIgnore {

    /**
     * 限定规则作用域
     *
     * @return
     */
    Class<?>[] value() default {};
}
