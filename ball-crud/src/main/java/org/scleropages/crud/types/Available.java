/**
 *
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*******************************************************************************
 *
 *
 * MODIFICATION DESCRIPTION
 *
 * Name                 Date                	     Description 
 * ============         =====================        ============
 * Martin Mao           May 21, 2016 10:01:04 PM     	     Created
 *
 *
 ********************************************************************************/

package org.scleropages.crud.types;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Transient;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface Available {

    @Transient
    @JsonIgnore
    @JSONField(serialize=false)
    void enable();

    @Transient
    @JsonIgnore
    @JSONField(serialize=false)
    void disable();

    @Transient
    @JsonIgnore
    @JSONField(serialize=false)
    boolean isAvailable();
}
