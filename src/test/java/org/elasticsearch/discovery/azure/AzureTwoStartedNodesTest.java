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

import org.elasticsearch.cloud.azure.management.AzureComputeService.Discovery;
import org.elasticsearch.cloud.azure.management.AzureComputeService.Management;
import org.elasticsearch.cloud.azure.management.AzureComputeServiceTwoNodesMock;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;

@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.TEST,
        numDataNodes = 0,
        transportClientRatio = 0.0,
        numClientNodes = 0)
public class AzureTwoStartedNodesTest extends AbstractAzureComputeServiceTest {

    public AzureTwoStartedNodesTest() {
        super(AzureComputeServiceTwoNodesMock.class);
    }

    @Test
    public void two_nodes_should_run_using_private_ip() {
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder()
                .put(Management.SERVICE_NAME, "dummy")
                .put(Discovery.HOST_TYPE, "private_ip")
                .put(super.settingsBuilder());

        logger.info("--> start first node");
        internalCluster().startNode(settings);
        assertThat(client().admin().cluster().prepareState().setMasterNodeTimeout("1s").execute().actionGet().getState().nodes().masterNodeId(), notNullValue());

        logger.info("--> start another node");
        internalCluster().startNode(settings);
        assertThat(client().admin().cluster().prepareState().setMasterNodeTimeout("1s").execute().actionGet().getState().nodes().masterNodeId(), notNullValue());

        // We expect having 2 nodes as part of the cluster, let's test that
        checkNumberOfNodes(2);
    }

    @Test
    public void two_nodes_should_run_using_public_ip() {
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder()
                .put(Management.SERVICE_NAME, "dummy")
                .put(Discovery.HOST_TYPE, "public_ip")
                .put(super.settingsBuilder());

        logger.info("--> start first node");
        internalCluster().startNode(settings);
        assertThat(client().admin().cluster().prepareState().setMasterNodeTimeout("1s").execute().actionGet().getState().nodes().masterNodeId(), notNullValue());

        logger.info("--> start another node");
        internalCluster().startNode(settings);
        assertThat(client().admin().cluster().prepareState().setMasterNodeTimeout("1s").execute().actionGet().getState().nodes().masterNodeId(), notNullValue());

        // We expect having 2 nodes as part of the cluster, let's test that
        checkNumberOfNodes(2);
    }
}
