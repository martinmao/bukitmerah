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

import org.scleropages.crud.web.Views;

import org.scleropages.openapi.OpenApi;
import org.scleropages.openapi.OpenApiContextHolder;
import org.scleropages.openapi.annotation.ApiIgnore;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@RequestMapping("open-api")
@RestController
@ApiIgnore
public class OpenApiAction {


    @GetMapping("ids")
    public Set<String> ids() {
        return OpenApiContextHolder.getOpenApiContext().ids();
    }

    @GetMapping("{id}")
    public void openApi(@PathVariable String id, HttpServletResponse response) {
        OpenApi openApi = OpenApiContextHolder.getOpenApiContext().openApi(id);
        Assert.notNull(openApi,"no open api found by given id.");
        Views.renderYaml(response, openApi.render());
    }

}
