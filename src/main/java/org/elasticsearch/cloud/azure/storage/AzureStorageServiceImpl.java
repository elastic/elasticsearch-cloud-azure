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

package org.elasticsearch.cloud.azure.storage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cloud.azure.AzureSettingsFilter;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.unit.TimeValue;

import static org.elasticsearch.cloud.azure.storage.AzureStorageService.Storage.ACCOUNT;
import static org.elasticsearch.cloud.azure.storage.AzureStorageService.Storage.KEY;

/**
 *
 */
public class AzureStorageServiceImpl extends AbstractLifecycleComponent<AzureStorageServiceImpl>
    implements AzureStorageService {

    @Inject
    public AzureStorageServiceImpl(Settings settings, SettingsFilter settingsFilter) {
        super(settings);
        settingsFilter.addFilter(new AzureSettingsFilter());
    }

    @Override
    public synchronized AzureClientImpl client() {

        String account = componentSettings.get("account", settings.get(ACCOUNT));
        String key = componentSettings.get("key", settings.get(KEY));

        return client(account, key);
    }

    @Override
    public synchronized AzureClientImpl client(String account, String key) {

        CloudBlobClient cloudBlobClient = null;
        TimeValue timeout = settings.getAsTime(Storage.TIMEOUT, TimeValue.timeValueMinutes(-1));
        String blob = "https://" + account + ".blob.core.windows.net/";

        try {
            if (account != null) {
                logger.trace("creating new Azure storage client using account [{}], key [{}], blob [{}]", account, key, blob);

                String storageConnectionString =
                        "DefaultEndpointsProtocol=https;"
                                + "AccountName="+ account +";"
                                + "AccountKey=" + key;

                // Retrieve storage account from connection-string.
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                // Create the blob client.
                cloudBlobClient = storageAccount.createCloudBlobClient();

                // Set timeout option. See cloud.azure.storage.timeout or cloud.azure.storage.xxx.timeout
                if (timeout.getSeconds() > 0) {
                    cloudBlobClient.getDefaultRequestOptions().setTimeoutIntervalInMs(safeTimeInMsToInt(timeout));
                }
            }
        } catch (Exception e) {
            // Can not start Azure Storage Client
            logger.error("can not start azure storage client: {}", e.getMessage());
        }

        return new AzureClientImpl(settings, cloudBlobClient);
    }

    // Needed for Java < Java 8
    public static int safeTimeInMsToInt(TimeValue timeout) {
        if (timeout.getMillis() < Integer.MIN_VALUE || timeout.getMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Can not convert [" + timeout +
                    "]. It can not be longer than 2,147,483,647ms.");
        }
        return (int) timeout.getMillis();
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        logger.debug("starting azure storage client instance");
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        logger.debug("stopping azure storage client instance");
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }
}
