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
package org.scleropages.core.mapper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class JsonMapper2 {

    private static Logger logger = LoggerFactory.getLogger(JsonMapper.class);

    private static AtomicBoolean applyFeatureConfigure = new AtomicBoolean(false);

    private static volatile int DEFAULT_PARSER_FEATURE = JSON.DEFAULT_PARSER_FEATURE;

    public static volatile int DEFAULT_GENERATE_FEATURE = JSON.DEFAULT_GENERATE_FEATURE;


    /**
     * Change default parser feature and generate feature can only be set once.User code should never use this method.
     * This method as extensions used for framework context to apply unified configuration.
     *
     * @param parserFeatures
     * @param generateFeatures
     */
    public static void applyFeatureConfig(List<Feature> parserFeatures, List<SerializerFeature> generateFeatures) {
        if (applyFeatureConfigure.compareAndSet(false, true)) {
            int parserFeature = DEFAULT_PARSER_FEATURE;
            int generateFeature = DEFAULT_GENERATE_FEATURE;
            for (int i = 0; i < parserFeatures.size(); i++) {
                parserFeature |= parserFeatures.get(i).getMask();
            }
            for (int i = 0; i < generateFeatures.size(); i++) {
                generateFeature |= generateFeatures.get(i).getMask();
            }

            DEFAULT_PARSER_FEATURE |= parserFeature;
            DEFAULT_GENERATE_FEATURE |= generateFeature;
        }
    }


    /**
     * serialize a object to json text.
     *
     * @param object
     * @return json text or null (any fault)
     */
    public static String toJson(Object object) {
        try {
            SerializeConfig config = new SerializeConfig();


            return JSON.toJSONString(object, DEFAULT_GENERATE_FEATURE, new SerializerFeature[0]);
        } catch (Exception e) {
            logger.warn("write to json string error:" + object, e);
            return null;
        }
    }

    /**
     * parse json text by given clazz(s)
     * <pre>
     * if clazz not provided use JSON.parseObject(json,Map.class)
     * if clazz[0] can assignable to Collection use JSON.parseArray(json, clazz[1]) or JSON.parseArray(json, Map.class) when clazz[1] not provided
     * otherwise use JSON.parseObject(json, clazz[0])
     * </pre>
     *
     * @param json
     * @param clazz
     * @param <T>
     * @return json object or null (any fault)
     */
    public static <T> T fromJson(String json, Class... clazz) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            if (ArrayUtils.isEmpty(clazz)) {
                return (T) JSON.parseObject(json, Map.class, DEFAULT_PARSER_FEATURE);
            }
            if (ClassUtils.isAssignable(Collection.class, clazz[0])) {
                return (T) (clazz.length > 1 ? parseArray(json, clazz[1]) : parseArray(json, Map.class));
            }
            return JSON.parseObject(json, clazz[0], DEFAULT_PARSER_FEATURE);
        } catch (Exception e) {
            if (logger.isDebugEnabled())
                logger.warn("parse json string error:" + json, e);
            else
                logger.warn("parse json string error:" + json);
            return null;
        }
    }


    private static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (text == null) {
            return null;
        }

        List<T> list;

        DefaultJSONParser parser = new DefaultJSONParser(text, ParserConfig.getGlobalInstance(), DEFAULT_PARSER_FEATURE);
        JSONLexer lexer = parser.lexer;
        int token = lexer.token();
        if (token == JSONToken.NULL) {
            lexer.nextToken();
            list = null;
        } else if (token == JSONToken.EOF && lexer.isBlankInput()) {
            list = null;
        } else {
            list = new ArrayList<T>();
            parser.parseArray(clazz, list);

            parser.handleResovleTask(list);
        }

        parser.close();

        return list;
    }

}
