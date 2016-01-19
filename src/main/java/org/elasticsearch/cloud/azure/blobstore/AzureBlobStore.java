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
import org.elasticsearch.cloud.azure.storage.AzureStorageService;
import org.elasticsearch.common.Booleans;
import org.elasticsearch.common.blobstore.BlobContainer;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.BlobStore;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.repositories.RepositoryName;
import org.elasticsearch.repositories.RepositorySettings;

import java.net.URISyntaxException;

import static org.elasticsearch.cloud.azure.storage.AzureStorageService.Storage.*;
import static org.elasticsearch.repositories.azure.AzureRepository.*;

/**
 *
 */
public class AzureBlobStore extends AbstractComponent implements BlobStore {

    private final AzureStorageService client;

    private final String container;
    private final String repositoryName;
    private final Boolean encrypt;

    private final String encryptionKey;
    private final String encryptionSalt;
    private final AzureEncrypt encrypter;

    @Inject
    public AzureBlobStore(RepositoryName name, Settings settings, RepositorySettings repositorySettings,
                          AzureStorageService client) throws URISyntaxException, StorageException {
        super(settings);
        this.client = client;

        this.encrypt = repositorySettings.settings().getAsBoolean("encrypt", settings.getAsBoolean(ENCRYPT, ENCRYPT_DEFAULT));
        this.encryptionKey = repositorySettings.settings().get("encryption_secret", settings.get(ENCRYPTION_KEY, ENCRYPTION_KEY_DEFAULT));
        this.encryptionSalt = repositorySettings.settings().get("encryption_salt", settings.get(ENCRYPTION_SALT, ENCRYPTION_SALT_DEFAULT));
        if(this.encrypt) {
            this.encrypter = new AzureEncrypt(this.encryptionKey, this.encryptionSalt);
        } else { this.encrypter = null; }

        this.container = repositorySettings.settings().get("container", settings.get(CONTAINER, CONTAINER_DEFAULT));
        this.repositoryName = name.getName();
    }

    @Override
    public String toString() {
        return container;
    }

    public AzureStorageService client() {
        return client;
    }

    public String container() {
        return container;
    }

    public Boolean isEncrypted() { return encrypt; }

    public String getEncryptionKey() { return encryptionKey; }

    public String getEncryptionSalt() { return encryptionSalt; }

    @Override
    public BlobContainer blobContainer(BlobPath path) {
        return new AzureBlobContainer(repositoryName, path, this, encrypter);
    }

    @Override
    public void delete(BlobPath path) {
        String keyPath = path.buildAsString("/");
        if (!keyPath.isEmpty()) {
            keyPath = keyPath + "/";
        }

        try {
            client.deleteFiles(container, keyPath);
        } catch (URISyntaxException | StorageException e) {
            logger.warn("can not remove [{}] in container {{}}: {}", keyPath, container, e.getMessage());
        }
    }

    @Override
    public void close() {
    }
}
