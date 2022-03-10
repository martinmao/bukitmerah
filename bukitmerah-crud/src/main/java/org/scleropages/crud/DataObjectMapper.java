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
package org.scleropages.crud;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Base mapstruct (https://mapstruct.org/) definitions for data object -> model (model -> data object) mappings:
 * <B>D=data object,M=model</B>
 * <pre>
 *     @ Mapper(config = DataObjectMapper.DefaultConfig.class)
 *     public interface CarDataMapper extends DataObjectMapper&lt;CarCreateRequest,Car&gt{
 *
 *     }
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */

public interface DataObjectMapper<D, M> {


    @MapperConfig(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedSourcePolicy = ReportingPolicy.WARN,
            unmappedTargetPolicy = ReportingPolicy.WARN,
            typeConversionPolicy = ReportingPolicy.ERROR)
    interface DefaultConfig {

    }

    D mapForTransport(M model);

    M mapForBizInvoke(D data);

    Iterable<D> mapForTransports(Iterable<M> models);

    Iterable<M> mapForInvokes(Iterable<D> models);
}
