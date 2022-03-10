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

import org.scleropages.crud.uid.UidGenerator;
import org.scleropages.crud.uid.provider.baidu.UidGeneratorProvider;
import org.scleropages.crud.uid.provider.baidu.impl.CachedUidGenerator;
import org.scleropages.crud.uid.provider.baidu.impl.DefaultUidGenerator;
import org.scleropages.crud.uid.provider.baidu.worker.DisposableWorkerIdAssigner;
import org.scleropages.crud.uid.provider.baidu.worker.WorkerIdAssigner;
import org.scleropages.crud.uid.provider.baidu.worker.dao.WorkerNodeDAO;
import org.scleropages.crud.uid.provider.baidu.worker.entity.WorkerNodeEntity;
import org.scleropages.crud.uid.provider.leaf.LeafUidProvider;
import org.scleropages.crud.uid.provider.leaf.SegmentService;
import org.scleropages.crud.uid.provider.leaf.segment.SegmentIDGenImpl;
import org.scleropages.crud.uid.provider.leaf.segment.dao.IDAllocRepository;
import org.scleropages.crud.uid.provider.leaf.segment.dao.LeafAllocEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
@ConditionalOnProperty(prefix = "uid", name = "enabled", havingValue = "true")
public class UidAutoConfiguration {

    @Configuration
    @ConditionalOnExpression("${uid.provider.enabled:uid-generator} || ${uid.provider.enabled:all}")
    @EnableConfigurationProperties({UidGeneratorConfigureProperties.class})
    public static class UidGeneratorConfiguration {

        @Configuration
        @EntityScan(basePackageClasses = {WorkerNodeEntity.class})
        @EnableJpaRepositories(basePackageClasses = {WorkerNodeDAO.class})
        @AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
        public static class RepositoryConfiguration {

        }

        @Bean
        public UidGenerator uidGenerator(org.scleropages.crud.uid.provider.baidu.UidGenerator uidGenerator) {
            UidGeneratorProvider generator = new UidGeneratorProvider();
            generator.setUidGenerator(uidGenerator);
            return generator;
        }

        @Bean
        public org.scleropages.crud.uid.provider.baidu.UidGenerator nativeUidGenerator(UidGeneratorConfigureProperties properties) {
            if (null != properties.isCachedEnabled() && properties.isCachedEnabled()) {
                CachedUidGenerator cachedUidGenerator = new CachedUidGenerator();
                applyDefaultProperties(cachedUidGenerator, properties);
                applyCachedProperties(cachedUidGenerator, properties);
                return cachedUidGenerator;
            }
            DefaultUidGenerator defaultUidGenerator = new DefaultUidGenerator();
            applyDefaultProperties(defaultUidGenerator, properties);
            return defaultUidGenerator;
        }


        @Bean
        public WorkerIdAssigner workerIdAssigner() {
            return new DisposableWorkerIdAssigner();
        }


        private void applyDefaultProperties(DefaultUidGenerator defaultUidGenerator, UidGeneratorConfigureProperties properties) {
            if (null != properties.getEpochStr())
                defaultUidGenerator.setEpochStr(properties.getEpochStr());
            if (null != properties.getSeqBits())
                defaultUidGenerator.setSeqBits(properties.getSeqBits());
            if (null != properties.getTimeBits())
                defaultUidGenerator.setTimeBits(properties.getTimeBits());
            if (null != properties.getWorkerBits())
                defaultUidGenerator.setWorkerBits(properties.getWorkerBits());
            defaultUidGenerator.setWorkerIdAssigner(workerIdAssigner());
        }

        private void applyCachedProperties(CachedUidGenerator cachedUidGenerator, UidGeneratorConfigureProperties properties) {
            UidGeneratorConfigureProperties.CachedProperties cached = properties.getCached();
            if (null == cached)
                return;
            if (null != cached.getBoostPower())
                cachedUidGenerator.setBoostPower(cached.getBoostPower());
            if (null != cached.getPaddingFactor())
                cachedUidGenerator.setPaddingFactor(cached.getPaddingFactor());
        }
    }


    @Configuration
    @ConditionalOnExpression("${uid.provider.enabled:leaf-segment} || ${uid.provider.enabled:all}")
    @EnableConfigurationProperties({LeafSegmentConfigureProperties.class})
    public static class LeafConfiguration {


        @Configuration
        @EntityScan(basePackageClasses = {LeafAllocEntity.class})
        @EnableJpaRepositories(basePackageClasses = {IDAllocRepository.class})
        @AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
        public static class RepositoryConfiguration {

        }

        @Bean
        public SegmentService segmentService(@Autowired IDAllocRepository idAllocRepository) {
            SegmentIDGenImpl segmentIDGen = new SegmentIDGenImpl();
            segmentIDGen.setDao(idAllocRepository);
            SegmentService segmentService = new SegmentService();
            segmentService.setIdGen(segmentIDGen);
            return segmentService;
        }


        @Bean
        public LeafUidProvider leafUidGenerator(@Autowired SegmentService segmentService) {
            LeafUidProvider leafUidGenerator = new LeafUidProvider(segmentService);
            return leafUidGenerator;
        }
    }
}
