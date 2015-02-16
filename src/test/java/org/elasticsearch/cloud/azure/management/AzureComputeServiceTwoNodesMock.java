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

package org.elasticsearch.cloud.azure.management;

import com.microsoft.windowsazure.management.compute.models.*;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;

import java.net.InetAddress;

/**
 * Mock Azure API with two started nodes
 */
public class AzureComputeServiceTwoNodesMock extends AzureComputeServiceAbstractMock {

    NetworkService networkService;

    @Inject
    protected AzureComputeServiceTwoNodesMock(Settings settings, NetworkService networkService) {
        super(settings);
        this.networkService = networkService;
    }

    @Override
    public HostedServiceGetDetailedResponse getServiceDetails() {
        HostedServiceGetDetailedResponse response = new HostedServiceGetDetailedResponse();
        HostedServiceGetDetailedResponse.Deployment deployment = new HostedServiceGetDetailedResponse.Deployment();

        // Fake the deployment
        deployment.setName("dummy");
        deployment.setDeploymentSlot(DeploymentSlot.Production);
        deployment.setStatus(DeploymentStatus.Running);

        // Fake a first instance
        RoleInstance instance1 = new RoleInstance();
        instance1.setInstanceName("dummy1");

        // Fake the private IP
        instance1.setIPAddress(InetAddress.getLoopbackAddress());

        // Fake the public IP
        InstanceEndpoint endpoint1 = new InstanceEndpoint();
        endpoint1.setName("elasticsearch");
        endpoint1.setVirtualIPAddress(InetAddress.getLoopbackAddress());
        endpoint1.setPort(9400);
        instance1.setInstanceEndpoints(Lists.newArrayList(endpoint1));

        // Fake a first instance
        RoleInstance instance2 = new RoleInstance();
        instance2.setInstanceName("dummy1");

        // Fake the private IP
        instance2.setIPAddress(InetAddress.getLoopbackAddress());

        // Fake the public IP
        InstanceEndpoint endpoint2 = new InstanceEndpoint();
        endpoint2.setName("elasticsearch");
        endpoint2.setVirtualIPAddress(InetAddress.getLoopbackAddress());
        endpoint2.setPort(9401);
        instance2.setInstanceEndpoints(Lists.newArrayList(endpoint2));

        deployment.setRoleInstances(Lists.newArrayList(instance1, instance2));

        response.setDeployments(Lists.newArrayList(deployment));

        return response;
    }
}
