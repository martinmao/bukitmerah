package org.scleropages.serialize.kryo;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for serialize implementations. the value must unique and consistency.<br>
 * <b>NOTE: value must more than 100</b>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Component
public @interface KryoId {
	int value();
}