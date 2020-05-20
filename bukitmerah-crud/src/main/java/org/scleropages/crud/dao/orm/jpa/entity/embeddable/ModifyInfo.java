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
package org.scleropages.crud.dao.orm.jpa.entity.embeddable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Date;

/**
 * @author <a href="mailto:dev.martinmao@gmail.com">Martin Mao</a>
 *
 */
@Embeddable
public class ModifyInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final ModifyInfo createNow() {
        return new ModifyInfo();
    }

    public final ModifyInfo modifyNow() {
        if (null == createTime)
            throw new IllegalStateException("createTime must not be null.");
        setModifyTime(new Date());
        return this;
    }

    public ModifyInfo() {
        this(new Date(), new Date());
    }

    public ModifyInfo(Date createTime, Date modifyTime) {
        this.createTime = createTime;
        this.modifyTime = modifyTime;
    }

    private Date createTime;

    private Date modifyTime;

    private String modifyDesc;

    @Column(name = "create_dt")
    public Date getCreateTime() {
        return createTime;
    }


    @Column(name = "mod_dt")
    public Date getModifyTime() {
        return modifyTime;
    }

    @Column(name = "mod_desc")
    public String getModifyDesc() {
        return modifyDesc;
    }

    public void setModifyDesc(String modifyDesc) {
        this.modifyDesc = modifyDesc;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }

}
