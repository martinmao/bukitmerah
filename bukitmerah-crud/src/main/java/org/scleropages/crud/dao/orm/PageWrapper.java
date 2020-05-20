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
package org.scleropages.crud.dao.orm;


import org.scleropages.crud.web.Servlets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class PageWrapper<T> implements Page<T> {

	private PageImpl<T> pageImpl;

	private Map<String, Object> searchParams;

	public PageWrapper(PageImpl<T> pageImpl, Map<String, Object> searchParams) {
		Assert.notNull(pageImpl, "wrappered page must not be null.");
		this.pageImpl = pageImpl;
		this.searchParams = searchParams;
	}

	public String getSearchParams() {
		return Servlets.encodeParameterStringWithPrefix(searchParams, SearchFilter.SearchFilterBuilder.SEARCH_PREFIX);
	}

	@Override
	public int getNumber() {
		return pageImpl.getNumber();
	}

	@Override
	public int getSize() {
		return pageImpl.getSize();
	}

	@Override
	public int getNumberOfElements() {
		return pageImpl.getNumberOfElements();
	}

	@Override
	public List<T> getContent() {
		return pageImpl.getContent();
	}

	@Override
	public boolean hasContent() {
		return pageImpl.hasContent();
	}

	@Override
	public Sort getSort() {
		return pageImpl.getSort();
	}

	@Override
	public boolean isFirst() {
		return pageImpl.isFirst();
	}

	@Override
	public boolean isLast() {
		return pageImpl.isLast();
	}

	@Override
	public boolean hasNext() {
		return pageImpl.hasNext();
	}

	@Override
	public boolean hasPrevious() {
		return pageImpl.hasPrevious();
	}

	@Override
	public Pageable nextPageable() {
		return pageImpl.nextPageable();
	}

	@Override
	public Pageable previousPageable() {
		return pageImpl.previousPageable();
	}

	@Override
	public Iterator<T> iterator() {
		return pageImpl.iterator();
	}

	@Override
	public int getTotalPages() {
		return pageImpl.getTotalPages();
	}

	@Override
	public long getTotalElements() {
		return pageImpl.getTotalElements();
	}

	@Override
	public <U> Page<U> map(Function<? super T, ? extends U> converter) {
		return null;
	}
}
