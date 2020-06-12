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
import org.apache.commons.io.FileUtils;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConfigLoader;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.scleropages.crud.web.Views;
import org.scleropages.openapi.OpenApi;
import org.scleropages.openapi.OpenApiContextHolder;
import org.scleropages.openapi.annotation.ApiIgnore;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
        Assert.notNull(openApi, "no open api found by given id.");
        Views.renderYaml(response, openApi.render());
    }

    @GetMapping("codegen")
    public List<Map<String, Object>> codeGeneratorProvidersMetadata() {
        List<Map<String, Object>> providersMetadata = Lists.newArrayList();
        List<CodegenConfig> all = CodegenConfigLoader.getAll();
        all.forEach(codegenConfig -> {
            Map<String, Object> providerMetadata = Maps.newLinkedHashMap();
            providersMetadata.add(providerMetadata);
            providerMetadata.put(codegenConfig.getName(),codegenConfig.getHelp());
            providerMetadata.put("libraries", codegenConfig.supportedLibraries());
            providerMetadata.put("template",codegenConfig.templateDir());
            providerMetadata.put("tag",codegenConfig.getTag());
        });
        return providersMetadata;
    }


    public static void main(String[] args) throws IOException {

        FileUtils.deleteDirectory(new File("/Users/martin/Downloads/gen"));

        CodegenConfigurator codegenConfigurator = new CodegenConfigurator();
        codegenConfigurator.setVerbose(true);
        codegenConfigurator.setSkipOverwrite(true);
        codegenConfigurator.setRemoveOperationIdPrefix(true);
        codegenConfigurator.setInputSpec("http://localhost:18080/kapuas/open-api/org.scleropages.kapuas.security");
        codegenConfigurator.setGeneratorName("java-customization");
        codegenConfigurator.setApiPackage("org.scleropages.kapuas.security");
        codegenConfigurator.setModelPackage("org.scleropages.kapuas.security.model");
        codegenConfigurator.setLibrary("resttemplate");
        codegenConfigurator.setOutputDir("/Users/martin/Downloads/gen");
        final ClientOptInput input = codegenConfigurator.toClientOptInput();
        List<File> generatedFiles = new DefaultGenerator().opts(input).generate();
    }

}
