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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Entity
@Table(name = "uid_leaf_alloc")
public class LeafAllocEntity {


    private String bizTag;
    private Long maxId;
    private Integer step;
    private String description;
    private Date date;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "biz_tag", columnDefinition = "varchar(128)  NOT NULL DEFAULT ''")
    public String getBizTag() {
        return bizTag;
    }

    @Column(name = "max_id", columnDefinition = "bigint(20) NOT NULL DEFAULT '1'")
    public Long getMaxId() {
        return maxId;
    }

    @Column(name = "step", columnDefinition = "int(11) NOT NULL")
    public Integer getStep() {
        return step;
    }

    @Column(name = "description", columnDefinition = "varchar(256)  DEFAULT NULL")
    public String getDescription() {
        return description;
    }

    @Column(name = "update_time", columnDefinition = "timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    public Date getDate() {
        return date;
    }

    public void setBizTag(String bizTag) {
        this.bizTag = bizTag;
    }

    public void setMaxId(Long maxId) {
        this.maxId = maxId;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
