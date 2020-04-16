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
 * Martin Mao           May 21, 2016 1:40:01 AM     	     Created
 *  
 * 
 ********************************************************************************/

package org.scleropages.crud.dao.orm.jpa.entity.embeddable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 * 
 */
@Embeddable
public class UniqueCodeAndName {

	private String name;

	private String code;

	@Column(name = "name_", nullable = false)
	public String getName() {
		return name;
	}

	@Column(name = "code_", nullable = false, unique = true)
	public String getCode() {
		return code;
	}
	
	public void setCodeAndName(String code){
		setCode(code);
		setName(code);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
