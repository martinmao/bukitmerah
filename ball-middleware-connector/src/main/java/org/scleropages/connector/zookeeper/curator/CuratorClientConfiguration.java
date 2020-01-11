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
package org.scleropages.connector.zookeeper.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
@ConditionalOnProperty(name = "zookeeper.client.enabled")
public class CuratorClientConfiguration {


    @Value("#{ @environment['zookeeper.client.embedded-server.port'] ?: 2181}")
    private int testingPort;
    @Value("#{ @environment['zookeeper.client.embedded-server.auto-start'] ?: true}")
    private boolean testingStart;

    @Bean
    @ConditionalOnClass({TestingServer.class})
    @ConditionalOnProperty(name = "zookeeper.client.embedded-server.enabled")
    public TestingServer testingServer() throws Exception {
        TestingServer testingServer = new TestingServer(testingPort, testingStart);
        return testingServer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("zookeeper.client")
    public CuratorOptions curatorOptions() {
        CuratorOptions curatorOptions = new CuratorOptions();
        return curatorOptions;
    }


    @Bean
    @ConditionalOnMissingBean
    public RetryPolicy retryPolicy(CuratorOptions curatorOptions) {
        return new ExponentialBackoffRetry(
                curatorOptions.getBaseSleepTimeMs(),
                curatorOptions.getMaxRetries(),
                curatorOptions.getMaxSleepMs());
    }


    @Bean
    @ConditionalOnMissingBean
    public CuratorFramework curatorFramework(CuratorOptions curatorOptions, RetryPolicy retryPolicy) {

        return CuratorFrameworkFactory.builder().
                connectString(curatorOptions.getConnectString()).
                namespace(curatorOptions.getNamespace()).
                retryPolicy(retryPolicy).
                sessionTimeoutMs(curatorOptions.getSessionTimeoutMs()).
                connectionTimeoutMs(curatorOptions.getConnectionTimeoutMs()).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public CuratorRecipesClient curatorClient(CuratorFramework curatorFramework, CuratorOptions curatorOptions) {
        CuratorRecipesClient curatorClient = new CuratorRecipesClient(curatorFramework, curatorOptions);
        return curatorClient;
    }


}
