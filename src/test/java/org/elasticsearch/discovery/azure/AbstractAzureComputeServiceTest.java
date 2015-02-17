/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.discovery.azure;

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.cloud.azure.management.AzureComputeService;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.test.ElasticsearchIntegrationTest;

public abstract class AbstractAzureComputeServiceTest extends ElasticsearchIntegrationTest {

    private Class<? extends AzureComputeService> mock;

    public AbstractAzureComputeServiceTest(Class<? extends AzureComputeService> mock) {
        // We want to inject the Azure API Mock
        this.mock = mock;
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        ImmutableSettings.Builder settings = ImmutableSettings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("plugins." + PluginsService.LOAD_PLUGIN_FROM_CLASSPATH, true);
        return settings.build();
    }

    protected void checkNumberOfNodes(int expected) {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().execute().actionGet();
        assertNotNull(nodeInfos);
        assertNotNull(nodeInfos.getNodes());
        assertEquals(expected, nodeInfos.getNodes().length);
    }

    protected Settings settingsBuilder() {
        ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder()
                .put("discovery.type", "azure")
                .put("cloud.azure.api.impl", mock)
                // We add a fake subscription_id to start mock compute service
                .put("cloud.azure.subscription_id", "fake")
                .put("cloud.azure.refresh_interval", "5s")
                .put("cloud.azure.keystore", "dummy")
                .put("cloud.azure.password", "dummy")
                .put("cloud.azure.service_name", "dummy")
                .put("cloud.azure.refresh_interval", "5s")
                // We need the network to make the mock working
                .put("node.mode", "network")
                // Make the tests run faster
                .put("discovery.zen.join.timeout", "100ms")
                .put("discovery.zen.ping.timeout", "10ms")
                .put("discovery.initial_state_timeout", "300ms");

        return builder.build();
    }
}
