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
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.LocationMode;
import com.microsoft.azure.storage.blob.*;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cloud.azure.AzureSettingsFilter;
import org.elasticsearch.common.blobstore.BlobMetaData;
import org.elasticsearch.common.blobstore.support.PlainBlobMetaData;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.repositories.RepositoryException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Hashtable;

import static org.elasticsearch.cloud.azure.storage.AzureStorageService.Storage.*;

/**
 *
 */
public class AzureStorageServiceImpl extends AbstractLifecycleComponent<AzureStorageServiceImpl>
    implements AzureStorageService {

    private final String[] accounts;
    private final String[] keys;
    private final Map<String, CloudBlobClient> clients;
    
    @Inject
    public AzureStorageServiceImpl(Settings settings, SettingsFilter settingsFilter) {
        super(settings);
        settingsFilter.addFilter(new AzureSettingsFilter());

        this.accounts = settings.getAsArray(ACCOUNT, settings.getAsArray(ACCOUNT_DEPRECATED));
        this.keys = settings.getAsArray(KEY, settings.getAsArray(KEY_DEPRECATED));
        this.clients = new Hashtable<String, CloudBlobClient>();

        if (this.accounts.length != this.keys.length) {
            throw new IllegalArgumentException("Azure cloud plug-in accounts and keys arrays must be the same length");
        }
    }

    private CloudBlobClient CreateClient(String account, String key)
    {
        try {
            String blob = "http://" + account + ".blob.core.windows.net/";
            logger.trace("creating new Azure storage client using account [{}], key [{}], blob [{}]", account, key, blob);

            String storageConnectionString =
                    "DefaultEndpointsProtocol=http;"
                            + "AccountName="+ account +";"
                            + "AccountKey=" + key;

            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

            // Create the blob client.
            CloudBlobClient client = storageAccount.createCloudBlobClient();
            return client;
        } catch (Exception e) {
            logger.error("can not create azure storage client: {}", e.getMessage());
            return null;
        }
    }
    
    private CloudBlobClient getSelectedClient(String account, LocationMode mode) {
        if (account == null) {
            // for backwards compatibility, not specifying an account means to use the first one in the list
            account = this.accounts[0];
        }
        
        CloudBlobClient client = this.clients.get(account);

        if (client == null) {
            for (int i = 0; i < this.accounts.length; i++) {
                if (this.accounts[i].equals(account)) {
                    client = this.CreateClient(account, this.keys[i]);
                    if (client != null) {
                        this.clients.put(account, client);
                    }
                    break;
                }
            }
        }
        
        if (client != null)
        {
            // NOTE: for now, just set the location mode in case it is different; 
            // only one mode per storage account can be active at a time
            client.getDefaultRequestOptions().setLocationMode(mode);
            return client;
        }

        throw new IllegalArgumentException("Azure cloud plug-in cannot create storage client; account not found");
    }
    
    @Override
    public boolean doesContainerExist(String account, LocationMode mode, String container) {
        try {
            CloudBlobClient client = this.getSelectedClient(account, mode);
            CloudBlobContainer blob_container = client.getContainerReference(container);
            return blob_container.exists();
        } catch (Exception e) {
            logger.error("can not access container [{}]", container);
        }
        return false;
    }

    @Override
    public void removeContainer(String account, LocationMode mode, String container) throws URISyntaxException, StorageException {
        CloudBlobClient client = this.getSelectedClient(account, mode);
        CloudBlobContainer blob_container = client.getContainerReference(container);
        // TODO Should we set some timeout and retry options?
        /*
        BlobRequestOptions options = new BlobRequestOptions();
        options.setTimeoutIntervalInMs(1000);
        options.setRetryPolicyFactory(new RetryNoRetry());
        blob_container.deleteIfExists(options, null);
        */
        logger.trace("removing container [{}]", container);
        blob_container.deleteIfExists();
    }

    @Override
    public void createContainer(String account, LocationMode mode, String container) throws URISyntaxException, StorageException {
        try {
            CloudBlobClient client = this.getSelectedClient(account, mode);
            CloudBlobContainer blob_container = client.getContainerReference(container);
            logger.trace("creating container [{}]", container);
            blob_container.createIfNotExists();
        } catch (IllegalArgumentException e) {
            logger.trace("fails creating container [{}]", container, e.getMessage());
            throw new RepositoryException(container, e.getMessage());
        }
    }

    @Override
    public void deleteFiles(String account, LocationMode mode, String container, String path) throws URISyntaxException, StorageException {
        logger.trace("delete files container [{}], path [{}]", container, path);

        // Container name must be lower case.
        CloudBlobClient client = this.getSelectedClient(account, mode);
        CloudBlobContainer blob_container = client.getContainerReference(container);
        if (blob_container.exists()) {
            for (ListBlobItem blobItem : blob_container.listBlobs(path)) {
                logger.trace("removing blob [{}]", blobItem.getUri());
                deleteBlob(account, mode, container, blobItem.getUri().toString());
            }
        }
    }

    @Override
    public boolean blobExists(String account, LocationMode mode, String container, String blob) throws URISyntaxException, StorageException {
        // Container name must be lower case.
        CloudBlobClient client = this.getSelectedClient(account, mode);
        CloudBlobContainer blob_container = client.getContainerReference(container);
        if (blob_container.exists()) {
            CloudBlockBlob azureBlob = blob_container.getBlockBlobReference(blob);
            return azureBlob.exists();
        }

        return false;
    }

    @Override
    public void deleteBlob(String account, LocationMode mode, String container, String blob) throws URISyntaxException, StorageException {
        logger.trace("delete blob for container [{}], blob [{}]", container, blob);

        // Container name must be lower case.
        CloudBlobClient client = this.getSelectedClient(account, mode);
        CloudBlobContainer blob_container = client.getContainerReference(container);
        if (blob_container.exists()) {
            logger.trace("container [{}]: blob [{}] found. removing.", container, blob);
            CloudBlockBlob azureBlob = blob_container.getBlockBlobReference(blob);
            azureBlob.delete();
        }
    }

    @Override
    public InputStream getInputStream(String account, LocationMode mode, String container, String blob) throws URISyntaxException, StorageException {
        logger.trace("reading container [{}], blob [{}]", container, blob);
        CloudBlobClient client = this.getSelectedClient(account, mode);
        return client.getContainerReference(container).getBlockBlobReference(blob).openInputStream();
    }

    @Override
    public OutputStream getOutputStream(String account, LocationMode mode, String container, String blob) throws URISyntaxException, StorageException {
        logger.trace("writing container [{}], blob [{}]", container, blob);
        CloudBlobClient client = this.getSelectedClient(account, mode);
        return client.getContainerReference(container).getBlockBlobReference(blob).openOutputStream();
    }

    @Override
    public ImmutableMap<String, BlobMetaData> listBlobsByPrefix(String account, LocationMode mode, String container, String keyPath, String prefix) throws URISyntaxException, StorageException {
        // NOTE: this should be here: if (prefix == null) prefix = "";
        // however, this is really inefficient since deleteBlobsByPrefix enumerates everything and 
        // then does a prefix match on the result; it should just call listBlobsByPrefix with the prefix!
        
        logger.debug("listing container [{}], keyPath [{}], prefix [{}]", container, keyPath, prefix);
        ImmutableMap.Builder<String, BlobMetaData> blobsBuilder = ImmutableMap.builder();

        CloudBlobClient client = this.getSelectedClient(account, mode);
        CloudBlobContainer blobContainer = client.getContainerReference(container);
        if (blobContainer.exists()) {
            for (ListBlobItem blobItem : blobContainer.listBlobs(keyPath + (prefix == null ? "" : prefix))) {
                URI uri = blobItem.getUri();
                logger.trace("blob url [{}]", uri);

                // uri.getPath is of the form /container/keyPath.* and we want to strip off the /container/
                // this requires 1 + container.length() + 1, with each 1 corresponding to one of the /
                String blobPath = uri.getPath().substring(1 + container.length() + 1);

                CloudBlockBlob blob = blobContainer.getBlockBlobReference(blobPath);

                // fetch the blob attributes from Azure (getBlockBlobReference does not do this)
                // this is needed to retrieve the blob length (among other metadata) from Azure Storage
                blob.downloadAttributes();

                BlobProperties properties = blob.getProperties();
                String name = blobPath.substring(keyPath.length());
                logger.trace("blob url [{}], name [{}], size [{}]", uri, name, properties.getLength());
                blobsBuilder.put(name, new PlainBlobMetaData(name, properties.getLength()));
            }
        }

        return blobsBuilder.build();
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
