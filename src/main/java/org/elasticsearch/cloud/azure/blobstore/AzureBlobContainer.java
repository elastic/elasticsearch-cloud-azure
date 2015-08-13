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

package org.elasticsearch.cloud.azure.blobstore;

import com.microsoft.azure.storage.StorageException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.blobstore.BlobMetaData;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.support.AbstractBlobContainer;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.repositories.RepositoryException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;

/**
 *
 */
public class AzureBlobContainer extends AbstractBlobContainer {

    protected final ESLogger logger = Loggers.getLogger(AzureBlobContainer.class);
    protected final AzureBlobStore blobStore;

    protected final String keyPath;
    protected final String repositoryName;

    public AzureBlobContainer(String repositoryName, BlobPath path, AzureBlobStore blobStore) {
        super(path);
        this.blobStore = blobStore;
        String keyPath = path.buildAsString("/");
        if (!keyPath.isEmpty()) {
            keyPath = keyPath + "/";
        }
        this.keyPath = keyPath;
        this.repositoryName = repositoryName;
    }

    @Override
    public boolean blobExists(String blobName) {
        try {
            return blobStore.blobExists(blobStore.container(), buildKey(blobName));
        } catch (URISyntaxException | StorageException e) {
            logger.warn("can not access [{}] in container {{}}: {}", blobName, blobStore.container(), e.getMessage());
        }
        return false;
    }

    @Override
    public InputStream openInput(String blobName) throws IOException {
        try {
            return blobStore.getInputStream(blobStore.container(), buildKey(blobName));
        } catch (StorageException e) {
            if (e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new FileNotFoundException(e.getMessage());
            }
            throw new IOException(e);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream createOutput(String blobName) throws IOException {
        try {
            return new AzureOutputStream(blobStore.getOutputStream(blobStore.container(), buildKey(blobName)));
        } catch (StorageException e) {
            if (e.getHttpStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new FileNotFoundException(e.getMessage());
            }
            throw new IOException(e);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        } catch (IllegalArgumentException e) {
            throw new RepositoryException(repositoryName, e.getMessage());
        }
    }

    @Override
    public boolean deleteBlob(String blobName) throws IOException {
        try {
            blobStore.deleteBlob(blobStore.container(), buildKey(blobName));
            return true;
        } catch (URISyntaxException | StorageException e) {
            logger.warn("can not access [{}] in container {{}}: {}", blobName, blobStore.container(), e.getMessage());
            throw new IOException(e);
        }
    }

    @Override
    public ImmutableMap<String, BlobMetaData> listBlobsByPrefix(@Nullable String prefix) throws IOException {

        try {
            return blobStore.listBlobsByPrefix(blobStore.container(), keyPath, prefix);
        } catch (URISyntaxException | StorageException e) {
            logger.warn("can not access [{}] in container {{}}: {}", prefix, blobStore.container(), e.getMessage());
            throw new IOException(e);
        }
    }

    @Override
    public ImmutableMap<String, BlobMetaData> listBlobs() throws IOException {
        return listBlobsByPrefix(null);
    }

    protected String buildKey(String blobName) {
        return keyPath + (blobName == null ? "" : blobName);
    }
}
