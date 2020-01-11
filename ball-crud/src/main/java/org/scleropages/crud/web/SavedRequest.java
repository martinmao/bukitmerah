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
package org.scleropages.crud.web;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class SavedRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private String method;
	private String queryString;
	private String requestURI;

	/**
	 * Constructs a new instance from the given HTTP request.
	 *
	 * @param request
	 *            the current request to save.
	 */
	public SavedRequest(HttpServletRequest request) {
		this.method = request.getMethod();
		this.queryString = request.getQueryString();
		this.requestURI = request.getRequestURI();
	}

	public String getMethod() {
		return method;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getRequestURI() {
		return requestURI;
	}

	public String getRequestUrl() {
		StringBuilder requestUrl = new StringBuilder(getRequestURI());
		if (getQueryString() != null) {
			requestUrl.append("?").append(getQueryString());
		}
		return requestUrl.toString();
	}

	@Override
	public String toString() {
		return getRequestUrl();
	}
}
