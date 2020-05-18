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
import com.google.common.collect.Maps;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.scleropages.core.util.Exceptions;
import org.scleropages.core.util.GenericTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 异常解析器，用于将业务层 @{@link org.springframework.stereotype.Service} 中抛出的异常通过 {@link BizExceptionTranslator }转换为其他可能的形式
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class BizExceptionTranslationInterceptor implements MethodInterceptor, InitializingBean, ApplicationContextAware {

    /**
     * Never using {@link java.lang.Integer.IntegerCache} (by default between -128 and 127 (inclusive)) or VM arguments -XX:AutoBoxCacheMax=<size>.
     * this may cause {@link #isTranslationStateChanged(Integer)} result not expected.
     */
    private static final int TRANSLATION_STATE_START = 128;

    protected static final Logger logger = LoggerFactory.getLogger(BizExceptionTranslationInterceptor.class);

    @Value("#{ @environment['application.biz-exception.code-prefix'] ?: N/A }")
    private String applicationBizExceptionCodePrefix;

    private static final ThreadLocal<Integer> translationState = new ThreadLocal<>();

    private BizExceptionTranslator finalBizExceptionTranslator = new FinalBizExceptionTranslator();

    private ApplicationContext applicationContext;


    private final static Integer joinTranslationState() {
        Integer state = translationState.get();
        state = null != state && state >= TRANSLATION_STATE_START ? state + 1 : TRANSLATION_STATE_START;
        translationState.set(state);
        return state;
    }

    private final static void leaveTranslationState() {
        Integer state = translationState.get();
        if (Objects.equals(state, TRANSLATION_STATE_START)) {
            translationState.remove();
            return;
        }
        translationState.set(state - 1);
    }

    protected final static boolean isTranslationStateChanged(Integer ticket) {
        return translationState.get() != ticket;
    }


    private List<BizExceptionTranslator> exceptionTranslators;


    @Override
    public void afterPropertiesSet() {
        if (null == exceptionTranslators)
            exceptionTranslators = Lists.newArrayList();
    }


    /**
     * 通用(最终)异常转换器，将java用户代码中最常见的 {@link IllegalArgumentException}以及 {@link IllegalStateException}等uncheck异常转换为
     * {@link BizException}体系.并将{@link BizError}中的设置应用到 {@link BizException}
     */
    private class FinalBizExceptionTranslator implements BizExceptionTranslator<BizException> {


        private final Class[] bizParamViolationExceptions = new Class[]{
                IllegalArgumentException.class,
                BizParamViolationException.class,
        };

        private final Class[] bizViolationExceptions = new Class[]{
                IllegalStateException.class,
                BizStateViolationException.class,
        };

        @Override
        public BizException translation(MethodInvocation invocation, Exception e) {


            Optional<BizErrorMetadata> optionalBizErrorMetadata = Optional.ofNullable(bizErrorRepository.computeIfAbsent(invocation.getMethod(), method -> {
                try {
                    return new BizErrorMetadata(invocation);
                } catch (BizErrorMetadataNotFoundException e1) {
                    return null;
                }
            }));
            if (e instanceof BizException) {
                BizException ex = (BizException) e;
                if (ex.isCodeAutoDetect()) {
                    String codeError = !Objects.equals(ex.getCode(), BizException.CODE_AUTO_DETECT) ? ex.getCode() : "";//用户代码抛出的biz exception中设置了code.
                    optionalBizErrorMetadata.ifPresent(bizErrorMetadata -> ex.setCode(bizErrorMetadata.code + codeError));
                }
                ex.setMethodInvocation(invocation);
                return ex;
            }
            RuntimeException runtimeCause = Exceptions.getCause(e, IllegalArgumentException.class);
            boolean isIllegalArgument = null != runtimeCause;
            if (null == runtimeCause)
                runtimeCause = Exceptions.getCause(e, IllegalStateException.class);
            if (null != runtimeCause) {

                BizException ex = null;
                if (optionalBizErrorMetadata.isPresent()) {
                    Class<? extends BizExceptionTransformer> bizExceptionTransformer = optionalBizErrorMetadata.get().bizExceptionTransformer;
                    if (!Objects.equals(BizExceptionNoTransformer.class, bizExceptionTransformer)) {
                        try {
                            ex = applicationContext.getBean(bizExceptionTransformer).apply(runtimeCause);
                        } catch (Exception beanException) {
                            logger.warn("can not transform BizException of transformer type: [" + bizExceptionTransformer + "]. use BizException as default. caused by:", beanException);
                        }
                    }
                }
                if (ex == null)
                    ex = isIllegalArgument ?
                            new BizParamViolationException(runtimeCause)
                            : new BizStateViolationException(runtimeCause);
                if (optionalBizErrorMetadata.isPresent()) {
                    ex.setCode(optionalBizErrorMetadata.get().code);
                }
                ex.setMethodInvocation(invocation);
                return ex;
            }
            logger.warn("internal warn: [{}] is a un-excepted exception type but occurred. this may caused some bugs.", e);
            return new BizException(e);
        }

        @Override
        public boolean support(Exception e) {
            return Exceptions.isCausedBy(e, bizParamViolationExceptions) || Exceptions.isCausedBy(e, bizViolationExceptions);
        }


        /**
         * used for get {@link BizError} access quickly.
         */
        private final Map<Method, BizErrorMetadata> bizErrorRepository = Maps.newConcurrentMap();

        private class BizErrorMetadataNotFoundException extends Exception {

        }

        private class BizErrorMetadata {

            private final String code;
            private final Class<? extends BizExceptionTransformer> bizExceptionTransformer;

            public BizErrorMetadata(MethodInvocation invocation) throws BizErrorMetadataNotFoundException {
                Method method = invocation.getMethod();
                BizError classDeclared = AnnotationUtils.findAnnotation(method.getDeclaringClass(), BizError.class);
                BizError methodDeclared = AnnotationUtils.findAnnotation(method, BizError.class);
                if (classDeclared == null && methodDeclared == null) {
                    throw new BizErrorMetadataNotFoundException();
                }
                String code = applicationBizExceptionCodePrefix;
                Class<? extends BizExceptionTransformer> bizExceptionTransformer = null;
                if (null != classDeclared) {
                    code += classDeclared.code();
                    bizExceptionTransformer = classDeclared.bizExceptionTransformer();
                } else {
                    code += BizError.DEFAULT_UNKNOWN_CODE;
                }
                if (null != methodDeclared) {
                    code += methodDeclared.code();
                    bizExceptionTransformer = methodDeclared.bizExceptionTransformer();
                } else {
                    code += BizError.DEFAULT_UNKNOWN_CODE;
                }
                this.code = code;
                this.bizExceptionTransformer = bizExceptionTransformer;
            }
        }
    }


    @Override
    public final Object invoke(MethodInvocation invocation) throws Throwable {
        Integer ticket = joinTranslationState();
        try {
            return invocation.proceed();
        } catch (Exception e) {
            if (isTranslationStateChanged(ticket)) {
                if (logger.isDebugEnabled())
                    logger.debug("[{}]: biz-exception translation state was changed. ignore to translate.", invocation.getMethod());
                throw e;
            }
            Exception throwable;
            try {
                throwable = translationException(invocation, e);
            } catch (Throwable t) {
                logger.warn("internal error: never throws any exception from BizExceptionTranslator implementations. caused by: ", t);
                throw e;
            }
            if (null == throwable) {
                logger.warn("internal error: never return null from BizExceptionTranslator implementations.");
                throw e;
            }
            throw throwable;

        } finally {
            leaveTranslationState();
        }
    }

    protected Exception translationException(final MethodInvocation invocation, final Exception e) {
        if (logger.isDebugEnabled()) {
            logger.debug("detected exception was occurred from: [{}] with error: [{}]:{} . performing biz-exception translating.....", invocation.getMethod(), e.getClass().getSimpleName(),e.getMessage());
        }
        Exception translating = e;
        BizExceptionTranslator translator = null;
        boolean translated = false;
        for (BizExceptionTranslator exceptionTranslator : exceptionTranslators) {
            if (exceptionTranslator.support(e)) {
                translator = exceptionTranslator;
                if (logger.isDebugEnabled()) {
                    logger.debug("using [{}] to translating exception from [{}] to [{}]",
                            translator.getClass().getSimpleName(),
                            e.getClass().getSimpleName(),
                            GenericTypes.getClassGenericType(false, exceptionTranslator.getClass(), BizExceptionTranslator.class, 0).getSimpleName());
                }
                translating = exceptionTranslator.translation(invocation, e);
                break;
            }
        }
        if (finalBizExceptionTranslator.support(translating)) {
            translating = finalBizExceptionTranslator.translation(invocation, translating);
            translated = true;
        } else if (null != translator) {
            logger.warn("final biz exception translator not supported [{}]. translated from [{}].",
                    GenericTypes.getClassGenericType(false, translator.getClass(),
                            BizExceptionTranslator.class, 0).getSimpleName(), translator.getClass().getSimpleName());
        }
        if ((!translated) && logger.isDebugEnabled()) {
            logger.debug("no match biz-exception translator for exception: " + e.getClass().getSimpleName());
        }
        return translating;
    }

    public void setExceptionTranslators(List<BizExceptionTranslator> exceptionTranslators) {
        this.exceptionTranslators = exceptionTranslators;
    }

    public List<BizExceptionTranslator> getExceptionTranslators() {
        if (null == exceptionTranslators)
            exceptionTranslators = Lists.newArrayList();
        return exceptionTranslators;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
