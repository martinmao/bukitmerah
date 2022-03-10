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
package org.scleropages.crud.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@ConfigurationProperties(prefix = "uid.uid-generator")
public class UidGeneratorConfigureProperties {

    /**
     * 增量时间位数, 相对于时间纪元起点 {@link #epochStr} 的增量值(单位为秒). 默认28位, 即2^28=268435456秒,可表示最多268435456/(60*60*24*365)=8.5年
     */
    private Integer timeBits;
    /**
     * 机器id位数,每次启动时会自增该值,默认22位，即2^22=4194304次重启
     */
    private Integer workerBits;
    /**
     * 一秒内自增序列位数，默认13位，即2^13=1秒内最多支持8192并发.
     */
    private Integer seqBits;
    /**
     * 时间纪元起点,默认 2021-07-01
     */
    private String epochStr;

    /**
     * 是否启用 {@link org.scleropages.crud.uid.provider.baidu.impl.CachedUidGenerator}
     */
    private Boolean cachedEnabled;

    /**
     * cached 属性设置
     */
    private CachedProperties cached;


    public Integer getTimeBits() {
        return timeBits;
    }

    public Integer getWorkerBits() {
        return workerBits;
    }

    public Integer getSeqBits() {
        return seqBits;
    }

    public String getEpochStr() {
        return epochStr;
    }

    public Boolean isCachedEnabled() {
        return cachedEnabled;
    }

    public CachedProperties getCached() {
        return cached;
    }

    public void setTimeBits(Integer timeBits) {
        this.timeBits = timeBits;
    }

    public void setWorkerBits(Integer workerBits) {
        this.workerBits = workerBits;
    }

    public void setSeqBits(Integer seqBits) {
        this.seqBits = seqBits;
    }

    public void setEpochStr(String epochStr) {
        this.epochStr = epochStr;
    }

    public void setCachedEnabled(Boolean cachedEnabled) {
        this.cachedEnabled = cachedEnabled;
    }

    public void setCached(CachedProperties cached) {
        this.cached = cached;
    }

    public class CachedProperties {

        /**
         * RingBuffer size扩容参数, 可提高UID生成的吞吐量.
         * 默认:3， 原bufferSize=8192, 扩容后bufferSize= 8192 << 3 = 65536
         */
        private Integer boostPower;
        /**
         * 指定何时向RingBuffer中填充UID, 取值为百分比(0, 100), 默认为50
         * 举例: bufferSize=1024, paddingFactor=50 -> threshold=1024 * 50 / 100 = 512.
         * 当环上可用UID数量 < 512时, 将自动对RingBuffer进行填充补全
         */
        private Integer paddingFactor;


        public Integer getBoostPower() {
            return boostPower;
        }

        public Integer getPaddingFactor() {
            return paddingFactor;
        }

        public void setBoostPower(Integer boostPower) {
            this.boostPower = boostPower;
        }

        public void setPaddingFactor(Integer paddingFactor) {
            this.paddingFactor = paddingFactor;
        }
    }
}
