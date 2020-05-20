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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Stack;

/**
 * This filter saved current request to a fixed size of stack in session
 * context.
 * 
 * @see {@link SavedRequest}
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class SavedRequestFilter extends OncePerRequestFilter {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String DEFAULT_SAVED_RQUEST_SESSION_KEY = "org.scleropages.web.SavedRequest";
	public static final int DEFAULT_SAVED_REQUEST_COUNT = 2;

	private String savedRequestSessionKey = DEFAULT_SAVED_RQUEST_SESSION_KEY;
	private int savedRequestCount = DEFAULT_SAVED_REQUEST_COUNT;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Stack<SavedRequest> savedRequests = getSavedRequestStack(request, response, filterChain);
		savedRequests.push(new SavedRequest(request));
		putSavedRequestStack(savedRequests, request, response, filterChain);
		filterChain.doFilter(request, response);
	}

	@SuppressWarnings("unchecked")
	protected Stack<SavedRequest> getSavedRequestStack(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) {
		HttpSession session=request.getSession();
		Object obj = session.getAttribute(savedRequestSessionKey);
		Stack<SavedRequest> savedRequests = null;
		if (null == obj) {
			savedRequests = createSavedRequestStack();
			if (logger.isDebugEnabled())
				logger.debug("Initialized new saved request statck of session: {}", session.getId());
		} else {
			Assert.isInstanceOf(Stack.class, obj, obj + " not a instanceof SavedRequest Stack. Make sure ["
					+ savedRequestSessionKey + "] not regist before use in session context.");
			savedRequests = (Stack<SavedRequest>) obj;
		}
		return savedRequests;
	}

	protected void putSavedRequestStack(Stack<SavedRequest> savedRequests, HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain) {
		HttpSession session=request.getSession();
		session.setAttribute(savedRequestSessionKey, savedRequests);
		if (logger.isDebugEnabled())
			logger.debug("Push and saved current request [{}] to request stack of session: {}.", savedRequests.peek(),
					session.getId());
	}

	protected Stack<SavedRequest> createSavedRequestStack() {
		return new Stack<SavedRequest>() {
			private static final long serialVersionUID = 1L;

			@Override
			public SavedRequest push(SavedRequest item) {
				while (this.size() >= savedRequestCount) {
					this.remove(0);
				}
				return super.push(item);
			}
		};
	}

	public void setSavedRequestSessionKey(String savedRequestSessionKey) {
		this.savedRequestSessionKey = savedRequestSessionKey;
	}

	public synchronized void setSavedRequestCount(int savedRequestCount) {
		Assert.isTrue(savedRequestCount > 0, "savedRequestCount must great than zero.");
		this.savedRequestCount = savedRequestCount;
	}

}
