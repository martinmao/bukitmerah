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
package org.scleropages.crud.dao.orm.jpa;

import com.google.common.collect.Lists;
import org.scleropages.core.mapper.JsonMapper2;
import org.scleropages.crud.dao.orm.PageableBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * user code please don't use this class. use {@link Pages#serializablePageable(Pageable)} to create this for serialize.
 * this class don't validate arguments.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class PageRequestImpl extends PageRequest implements Pageable, Serializable {

    private static final Pageable RECOVERABLE_PAGEABLE = PageRequest.of(1, 1);
    private static final Sort RECOVERABLE_SORT = Sort.unsorted();
    private static final Sort.Order RECOVERABLE_ORDER = Sort.Order.asc("dummy");

    private int pageNoImpl = PageableBuilder.DEFAULT_PAGE_NO;
    private int pageSizeImpl = PageableBuilder.DEFAULT_PAGE_SIZE;
    private SortImpl sortImpl;

    private boolean unpagedImpl = false;

    private Pageable nativePageRequest = RECOVERABLE_PAGEABLE;


    public PageRequestImpl(Pageable pageable) {
        super(1, 1, Sort.unsorted());
        if (pageable.isUnpaged()) {
            unpagedImpl = true;
        } else {
            this.pageNoImpl = pageable.getPageNumber();
            this.pageSizeImpl = pageable.getPageSize();
            this.sortImpl = new SortImpl(pageable.getSort());
        }
    }

    public PageRequestImpl() {
        super(1, 1, Sort.unsorted());
    }

    private Pageable recoverPageableIfNecessary() {
        if (unpagedImpl) {
            return Pageable.unpaged();
        }
        if (Objects.equals(RECOVERABLE_PAGEABLE, nativePageRequest)) {
            nativePageRequest = PageRequest.of(pageNoImpl, pageSizeImpl, sortImpl.recoverSortIfNecessary());
        }
        return nativePageRequest;
    }


    public static class SortImpl extends Sort implements Serializable {

        private List<OrderImpl> orderImpls = Lists.newArrayList();

        private Sort nativeSort = RECOVERABLE_SORT;

        public SortImpl() {
            super(null);
        }

        public SortImpl(Sort sort) {
            super(null);
            sort.forEach(order -> {
                orderImpls.add(new OrderImpl(order));
            });
        }

        private Sort recoverSortIfNecessary() {
            if (Objects.equals(RECOVERABLE_SORT, nativeSort)) {
                List<Order> _orders = Lists.newArrayList();
                orderImpls.forEach(orderImpl -> {
                    _orders.add(orderImpl.recoverOrderIfNecessary());
                });
                nativeSort = Sort.by(_orders);
            }
            return nativeSort;
        }


        @Override
        @Transient
        public Sort descending() {
            return recoverSortIfNecessary().descending();
        }

        @Override
        @Transient
        public Sort ascending() {
            return recoverSortIfNecessary().ascending();
        }

        @Override
        @Transient
        public boolean isSorted() {
            return recoverSortIfNecessary().isSorted();
        }

        @Override
        @Transient
        public boolean isUnsorted() {
            return recoverSortIfNecessary().isUnsorted();
        }

        @Override
        @Transient
        public Sort and(Sort sort) {
            return recoverSortIfNecessary().and(sort);
        }

        @Override
        @Transient
        public Order getOrderFor(String property) {
            return recoverSortIfNecessary().getOrderFor(property);
        }

        @Override
        @Transient
        public Iterator<Order> iterator() {
            return recoverSortIfNecessary().iterator();
        }

        @Override
        public boolean equals(Object obj) {
            return recoverSortIfNecessary().equals(obj);
        }

        @Override
        public int hashCode() {
            return recoverSortIfNecessary().hashCode();
        }

        @Override
        public String toString() {
            return recoverSortIfNecessary().toString();
        }

        public List<OrderImpl> getOrderImpls() {
            return orderImpls;
        }

        public void setOrderImpls(List<OrderImpl> orderImpls) {
            this.orderImpls = orderImpls;
        }


    }

    public static class OrderImpl extends Sort.Order implements Serializable {

        private Sort.Direction direction;
        private String property;
        private boolean ignoreCase;
        private Sort.NullHandling nullHandling;

        private Sort.Order nativeOrder = RECOVERABLE_ORDER;

        public OrderImpl() {
            super(null, "dummy");
        }

        public OrderImpl(Sort.Order nativeOrder) {
            super(nativeOrder.getDirection(), nativeOrder.getProperty(), nativeOrder.getNullHandling());
            this.direction = nativeOrder.getDirection();
            this.property = nativeOrder.getProperty();
            this.ignoreCase = nativeOrder.isIgnoreCase();
            this.nullHandling = nativeOrder.getNullHandling();
        }

        private Sort.Order recoverOrderIfNecessary() {
            if (Objects.equals(RECOVERABLE_ORDER, nativeOrder)) {
                nativeOrder = new Sort.Order(this.direction, this.property, this.nullHandling);
            }
            return nativeOrder;
        }


        @Override
        @Transient
        public boolean isAscending() {
            return recoverOrderIfNecessary().isAscending();
        }

        @Override
        @Transient
        public boolean isDescending() {
            return recoverOrderIfNecessary().isDescending();
        }

        @Override
        @Transient
        public Sort.Order with(Sort.Direction direction) {
            return recoverOrderIfNecessary().with(direction);
        }

        @Override
        @Transient
        public Sort.Order withProperty(String property) {
            return recoverOrderIfNecessary().withProperty(property);
        }

        @Override
        @Transient
        public Sort withProperties(String... properties) {
            return recoverOrderIfNecessary().withProperties(properties);
        }

        @Override
        @Transient
        public Sort.Order ignoreCase() {
            return recoverOrderIfNecessary().ignoreCase();
        }

        @Override
        @Transient
        public Sort.Order with(Sort.NullHandling nullHandling) {
            return recoverOrderIfNecessary().with(nullHandling);
        }

        @Override
        @Transient
        public Sort.Order nullsFirst() {
            return recoverOrderIfNecessary().nullsFirst();
        }

        @Override
        @Transient
        public Sort.Order nullsLast() {
            return recoverOrderIfNecessary().nullsLast();
        }

        @Override
        @Transient
        public Sort.Order nullsNative() {
            return recoverOrderIfNecessary().nullsNative();
        }

        @Override
        public int hashCode() {
            return recoverOrderIfNecessary().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return recoverOrderIfNecessary().equals(obj);
        }

        @Override
        public String toString() {
            return recoverOrderIfNecessary().toString();
        }

        @Override
        public Sort.Direction getDirection() {
            return direction;
        }

        public void setDirection(Sort.Direction direction) {
            this.direction = direction;
        }

        @Override
        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        @Override
        public boolean isIgnoreCase() {
            return ignoreCase;
        }

        public void setIgnoreCase(boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
        }

        @Override
        public Sort.NullHandling getNullHandling() {
            return nullHandling;
        }

        public void setNullHandling(Sort.NullHandling nullHandling) {
            this.nullHandling = nullHandling;
        }
    }

    protected PageRequestImpl(int page, int size, Sort sort) {
        super(page, size, sort);
    }

    @Override
    @Transient
    public Sort getSort() {
        return recoverPageableIfNecessary().getSort();
    }

    @Override
    public Pageable next() {
        return recoverPageableIfNecessary().next();
    }

    @Override
    public PageRequest previous() {
        Pageable pageable = recoverPageableIfNecessary();
        if (pageable instanceof PageRequest)
            return ((PageRequest) recoverPageableIfNecessary()).previous();
        throw new UnsupportedOperationException();
    }

    @Override
    public Pageable first() {
        return recoverPageableIfNecessary().first();
    }

    @Override
    public boolean equals(Object obj) {
        return recoverPageableIfNecessary().equals(obj);
    }

    @Override
    public int hashCode() {
        return recoverPageableIfNecessary().hashCode();
    }

    @Override
    public String toString() {
        return recoverPageableIfNecessary().toString();
    }

    public int getPageNoImpl() {
        return pageNoImpl;
    }

    public void setPageNoImpl(int pageNoImpl) {
        this.pageNoImpl = pageNoImpl;
    }

    public int getPageSizeImpl() {
        return pageSizeImpl;
    }

    public void setPageSizeImpl(int pageSizeImpl) {
        this.pageSizeImpl = pageSizeImpl;
    }

    public SortImpl getSortImpl() {
        return sortImpl;
    }

    public void setSortImpl(SortImpl sortImpl) {
        this.sortImpl = sortImpl;
    }

    public boolean isUnpagedImpl() {
        return unpagedImpl;
    }

    public void setUnpagedImpl(boolean unpagedImpl) {
        this.unpagedImpl = unpagedImpl;
    }

    public static void main(String[] args) {
        testPage(PageRequest.of(1, 15, Sort.Direction.DESC, "a", "b", "c", "d"));
        testPage(Pageable.unpaged());
        testPage(PageRequest.of(1, 15));
        testPage(PageRequest.of(1, 15, Sort.by(Sort.Order.asc("a"), Sort.Order.desc("b"))));
        testPage(PageRequest.of(1, 15, Sort.by("a", "b", "c")));

    }

    private static void testPage(Pageable sp) {
        Pageable lp = Pages.serializablePageable(sp);
        String text = JsonMapper2.toJson(lp);
        System.out.println(text);
        Pageable rlp = JsonMapper2.fromJson(text, PageRequestImpl.class);
        System.out.println(rlp);
    }
}
