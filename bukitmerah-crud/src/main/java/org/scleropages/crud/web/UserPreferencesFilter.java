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
 * Martin Mao           2013-12-2-下午3:23:43     	     Created
 *  
 * 
 ********************************************************************************/
package org.scleropages.crud.web;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;
import java.io.IOException;
import java.util.Locale;

/**
 * @author <a href="mailto:dev.martinmao@gmail.com">Martin Mao</a>
 *
 */
public class UserPreferencesFilter extends OncePerRequestFilter {

	/**
	 * Session scope attribute that holds the locale set by the user. By setting
	 * this key to the same one that Struts uses, we get synchronization in
	 * Struts w/o having to do extra work or have two session-level variables.
	 */
	public static final String PREFERRED_LOCALE_KEY = UserPreferencesFilter.class.getName() + ".LOCALE";

	public static final String PREFERRED_THEMES_KEY = UserPreferencesFilter.class.getName() + ".THEMES";

	private static final String[] THEMES_LIST = new String[] { "default", "blue", "darkblue", "grey", "light",
			"light2" };

	protected void doThemesPreference(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String preferredTheme = request.getParameter("theme");
		if (StringUtils.hasText(preferredTheme) && ArrayUtils.contains(THEMES_LIST, preferredTheme)) {
			HttpSession session = request.getSession(false);
			session.setAttribute(PREFERRED_THEMES_KEY, preferredTheme);
		}
	}

	/**
	 * This method looks for a "locale" request parameter. If it finds one, it
	 * sets it as the preferred locale and also configures it to work with JSTL.
	 * 
	 * @param request
	 *            the current request
	 * @param response
	 *            the current response
	 * @param chain
	 *            the chain
	 * @throws IOException
	 *             when something goes wrong
	 * @throws ServletException
	 *             when a communication failure happens
	 */
	public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		doLocalPreference(request, response, chain);
		doThemesPreference(request, response, chain);
		
		chain.doFilter(request, response);

		postFilterExecution(request, response, chain);
	}

	/**
	 * This method looks for a "locale" request parameter. If it finds one, it
	 * sets it as the preferred locale and also configures it to work with JSTL.
	 * 
	 * @param request
	 *            the current request
	 * @param response
	 *            the current response
	 * @param chain
	 *            the chain
	 * @throws IOException
	 *             when something goes wrong
	 * @throws ServletException
	 *             when a communication failure happens
	 */
	protected void doLocalPreference(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String locale = request.getParameter("locale");
		Locale preferredLocale = null;

		if (locale != null) {
			int indexOfUnderscore = locale.indexOf('_');
			if (indexOfUnderscore != -1) {
				String language = locale.substring(0, indexOfUnderscore);
				String country = locale.substring(indexOfUnderscore + 1);
				preferredLocale = new Locale(language, country);
			} else {
				preferredLocale = new Locale(locale);
			}
		}

		HttpSession session = request.getSession(false);

		if (session != null) {
			if (preferredLocale == null) {
				preferredLocale = (Locale) session.getAttribute(PREFERRED_LOCALE_KEY);
			} else {
				session.setAttribute(PREFERRED_LOCALE_KEY, preferredLocale);
				Config.set(session, Config.FMT_LOCALE, preferredLocale);
			}

			if (preferredLocale != null && !(request instanceof LocaleRequestWrapper)) {
				request = new LocaleRequestWrapper(request, preferredLocale);
				LocaleContextHolder.setLocale(preferredLocale);
			}
		}


	}

	protected void postFilterExecution(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		// Reset thread-bound LocaleContext.
		LocaleContextHolder.setLocaleContext(null);
	}
}
