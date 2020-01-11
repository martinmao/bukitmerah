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
package org.scleropages.crud.web.ui;

import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class BindingResults {

	private static final String FILED_SEPARATOR = ".";

	private static final String MSG_SEPARATOR = ": ";

	public static void assertBindingResultsFieldErrors(BindingResult bindingResult) {
		Assert.notNull(bindingResult, "bindingResult must not be null.");
		if (!bindingResult.hasFieldErrors())
			return;
		StringBuilder sb = new StringBuilder();
		for (FieldError fieldError : bindingResult.getFieldErrors()) {
			sb.append(fieldError.getField());
			sb.append(MSG_SEPARATOR);
			sb.append(fieldError.getDefaultMessage());
			sb.append(FILED_SEPARATOR);
		}
		throw new IllegalArgumentException(sb.toString());
	}
}
