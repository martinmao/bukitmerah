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
package org.scleropages.openapi.web;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.scleropages.openapi.OpenApiContextHolder;
import org.scleropages.openapi.annotation.ApiIgnore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@RequestMapping("api-docs")
@Controller
@ApiIgnore
public class ApiDocAction implements InitializingBean {

    private static final String VIEWER_REDOC = "redoc";
    private static final String VIEWER_SWAGGER = "swagger";

    @Value("#{ @environment['openapi.apidoc.default-viewer'] ?: 'redoc' }")
    private String defaultViewer;

    @Value("#{ @environment['openapi.gateway'] ?: null }")
    private String gateway;


    @GetMapping
    public String home() {
        return "redirect:webjars/api-home.html";
    }

    @GetMapping("list")
    @ResponseBody
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> response = Lists.newArrayList();
        OpenApiContextHolder.getOpenApiContext().ids().forEach(s -> {
            Map<String, Object> item = Maps.newHashMap();
            item.put("tittle", s);
            item.put("url", gateway + "/webjars/" + defaultViewer + "/index.html" + "?url=" + gateway + "/open-api/" + s);
            response.add(item);
        });
        return response;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.gateway = StringUtils.removeEnd(gateway, "/");
    }
}
