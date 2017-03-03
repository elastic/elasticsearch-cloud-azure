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

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.elasticsearch.common.blobstore.BlobMetaData;
import org.elasticsearch.common.blobstore.support.PlainBlobMetaData;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.repositories.RepositoryException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 */
public class AzureClientImpl extends AbstractComponent implements AzureClient {

    private CloudBlobClient client;

    public AzureClientImpl(Settings settings, CloudBlobClient client) {
        super(settings);
        this.client = client;
    }

    public boolean doesContainerExist(String container) {
        try {
            CloudBlobContainer blobContainer = client.getContainerReference(container);
            return blobContainer.exists();
        } catch (Exception e) {
            logger.error("can not access container [{}]", container);
        }
        return false;
    }

    public void removeContainer(String container) throws URISyntaxException, StorageException {
        CloudBlobContainer blobContainer = client.getContainerReference(container);
        // TODO Should we set some timeout and retry options?
        /*
        BlobRequestOptions options = new BlobRequestOptions();
        options.setTimeoutIntervalInMs(1000);
        options.setRetryPolicyFactory(new RetryNoRetry());
        blobContainer.deleteIfExists(options, null);
        */
        logger.trace("removing container [{}]", container);
        blobContainer.deleteIfExists();
    }

    public void createContainer(String container) throws URISyntaxException, StorageException {
        try {
            CloudBlobContainer blobContainer = client.getContainerReference(container);
            logger.trace("creating container [{}]", container);
            blobContainer.createIfNotExists();
        } catch (IllegalArgumentException e) {
            logger.trace("fails creating container [{}]", container, e.getMessage());
            throw new RepositoryException(container, e.getMessage());
        }
    }

    public void deleteFiles(String container, String path) throws URISyntaxException, StorageException {
        logger.trace("delete files container [{}], path [{}]", container, path);

        // Container name must be lower case.
        CloudBlobContainer blobContainer = client.getContainerReference(container);
        if (blobContainer.exists()) {
            // We list the blobs using a flat blob listing mode
            for (ListBlobItem blobItem : blobContainer.listBlobs(path, true)) {
                String blobName = blobNameFromUri(blobItem.getUri());
                logger.trace("removing blob [{}] full URI was [{}]", blobName, blobItem.getUri());
                deleteBlob(container, blobName);
            }
        }

    }

    /**
     * Extract the blob name from a URI like https://myservice.azure.net/container/path/to/myfile
     * It should remove the container part (first part of the path) and gives path/to/myfile
     * @param uri URI to parse
     * @return The blob name relative to the container
     */
    public static String blobNameFromUri(URI uri) {
        String path = uri.getPath();

        // We remove the container name from the path
        // The 3 magic number cames from the fact if path is /container/path/to/myfile
        // First occurrence is empty "/"
        // Second occurrence is "container
        // Last part contains "path/to/myfile" which is what we want to get
        String[] splits = path.split("/", 3);

        // We return the remaining end of the string
        return splits[2];
    }

    public boolean blobExists(String container, String blob) throws URISyntaxException, StorageException {
        // Container name must be lower case.
        CloudBlobContainer blobContainer = client.getContainerReference(container);
        if (blobContainer.exists()) {
            CloudBlockBlob azureBlob = blobContainer.getBlockBlobReference(blob);
            return azureBlob.exists();
        }

        return false;
    }

    public void deleteBlob(String container, String blob) throws URISyntaxException, StorageException {
        logger.trace("delete blob for container [{}], blob [{}]", container, blob);

        // Container name must be lower case.
        CloudBlobContainer blobContainer = client.getContainerReference(container);
        if (blobContainer.exists()) {
            logger.trace("container [{}]: blob [{}] found. removing.", container, blob);
            CloudBlockBlob azureBlob = blobContainer.getBlockBlobReference(blob);
            azureBlob.delete();
        }
    }

    public InputStream getInputStream(String container, String blob) throws URISyntaxException, StorageException {
        logger.trace("reading container [{}], blob [{}]", container, blob);
        return client.getContainerReference(container).getBlockBlobReference(blob).openInputStream();
    }

    public OutputStream getOutputStream(String container, String blob) throws URISyntaxException, StorageException {
        logger.trace("writing container [{}], blob [{}]", container, blob);
        return client.getContainerReference(container).getBlockBlobReference(blob).openOutputStream();
    }

    public ImmutableMap<String, BlobMetaData> listBlobsByPrefix(String container, String keyPath, String prefix) throws URISyntaxException, StorageException {
        logger.trace("listing container [{}], keyPath [{}], prefix [{}]", container, keyPath, prefix);
        ImmutableMap.Builder<String, BlobMetaData> blobsBuilder = ImmutableMap.builder();

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

}
