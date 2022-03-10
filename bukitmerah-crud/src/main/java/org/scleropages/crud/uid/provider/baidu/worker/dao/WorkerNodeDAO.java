/*
 * Copyright (c) 2017 Baidu, Inc. All Rights Reserve.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scleropages.crud.uid.provider.baidu.worker.dao;


import org.scleropages.crud.dao.orm.jpa.GenericRepository;
import org.scleropages.crud.dao.orm.jpa.complement.JooqRepository;
import org.scleropages.crud.jooq.tables.UidWorkerNode;
import org.scleropages.crud.jooq.tables.records.UidWorkerNodeRecord;
import org.scleropages.crud.uid.provider.baidu.worker.entity.WorkerNodeEntity;
import org.springframework.stereotype.Repository;

/**
 * DAO for M_WORKER_NODE
 *
 * @author yutianbao
 */
@Repository
public interface WorkerNodeDAO extends GenericRepository<WorkerNodeEntity, Long>, JooqRepository<UidWorkerNode, UidWorkerNodeRecord, WorkerNodeEntity> {

    /**
     * Get {@link WorkerNodeEntity} by node host
     *
     * @param host
     * @param port
     * @return
     */
    default WorkerNodeEntity getWorkerNodeByHostPort(String host, String port) {
        return getByHostNameAndPort(host, port);
    }

    WorkerNodeEntity getByHostNameAndPort(String host, String port);

    /**
     * Add {@link WorkerNodeEntity}
     *
     * @param workerNodeEntity
     */
    default void addWorkerNode(WorkerNodeEntity workerNodeEntity) {
        save(workerNodeEntity);
    }

}
