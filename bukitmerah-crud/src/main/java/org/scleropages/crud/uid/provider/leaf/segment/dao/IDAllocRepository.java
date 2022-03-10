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
package org.scleropages.crud.uid.provider.leaf.segment.dao;

import com.google.common.collect.Lists;
import org.jooq.Record3;
import org.scleropages.crud.dao.orm.jpa.GenericRepository;
import org.scleropages.crud.dao.orm.jpa.complement.JooqRepository;
import org.scleropages.crud.jooq.tables.UidLeafAlloc;
import org.scleropages.crud.jooq.tables.records.UidLeafAllocRecord;
import org.scleropages.crud.uid.provider.leaf.segment.model.LeafAlloc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.scleropages.crud.jooq.Tables.UID_LEAF_ALLOC;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface IDAllocRepository extends IDAllocDao, GenericRepository<LeafAllocEntity, String>, JooqRepository<UidLeafAlloc, UidLeafAllocRecord, LeafAllocEntity> {

    @Override
    @Transactional(readOnly = true)
    default List<LeafAlloc> getAllLeafAllocs() {
        List<LeafAlloc> leafAllocs = Lists.newArrayList();
        dslContext().select(UID_LEAF_ALLOC.BIZ_TAG, UID_LEAF_ALLOC.MAX_ID, UID_LEAF_ALLOC.STEP, UID_LEAF_ALLOC.UPDATE_TIME).from(UID_LEAF_ALLOC).fetch().forEach(r -> {
            LeafAlloc leafAlloc = new LeafAlloc();
            mapBasicProperties(leafAlloc, r.get(UID_LEAF_ALLOC.BIZ_TAG), r.get(UID_LEAF_ALLOC.MAX_ID), r.get(UID_LEAF_ALLOC.STEP));
            leafAlloc.setUpdateTime(r.getValue(UID_LEAF_ALLOC.UPDATE_TIME).toString());
        });
        return leafAllocs;
    }

    default void mapBasicProperties(LeafAlloc leafAlloc, String bizTag, Long maxId, Integer step) {
        leafAlloc.setKey(bizTag);
        leafAlloc.setMaxId(maxId);
        leafAlloc.setStep(step);
    }

    @Override
    @Transactional
    default LeafAlloc updateMaxIdAndGetLeafAlloc(String tag) {
        dslContext().update(UID_LEAF_ALLOC).set(UID_LEAF_ALLOC.MAX_ID, UID_LEAF_ALLOC.MAX_ID.add(UID_LEAF_ALLOC.STEP)).where(UID_LEAF_ALLOC.BIZ_TAG.eq(tag)).execute();
        return getLeafAllocInternal(tag);
    }


    @Override
    @Transactional
    default LeafAlloc updateMaxIdByCustomStepAndGetLeafAlloc(LeafAlloc leafAlloc) {
        String tag = leafAlloc.getKey();
        dslContext().update(UID_LEAF_ALLOC).set(UID_LEAF_ALLOC.MAX_ID, UID_LEAF_ALLOC.MAX_ID.add(leafAlloc.getStep())).where(UID_LEAF_ALLOC.BIZ_TAG.eq(tag)).execute();
        return getLeafAllocInternal(tag);
    }

    @Transactional(readOnly = true)
    default LeafAlloc getLeafAllocInternal(String tag) {
        Record3<String, Long, Integer> r = dslContext().select(UID_LEAF_ALLOC.BIZ_TAG, UID_LEAF_ALLOC.MAX_ID, UID_LEAF_ALLOC.STEP).from(UID_LEAF_ALLOC).where(UID_LEAF_ALLOC.BIZ_TAG.eq(tag)).fetchOne();
        LeafAlloc leafAlloc = new LeafAlloc();
        mapBasicProperties(leafAlloc, r.get(UID_LEAF_ALLOC.BIZ_TAG), r.get(UID_LEAF_ALLOC.MAX_ID), r.get(UID_LEAF_ALLOC.STEP));
        return leafAlloc;
    }

    @Override
    @Transactional(readOnly = true)
    default List<String> getAllTags() {
        return dslContext().select(UID_LEAF_ALLOC.BIZ_TAG).from(UID_LEAF_ALLOC).fetchInto(String.class);
    }
}
