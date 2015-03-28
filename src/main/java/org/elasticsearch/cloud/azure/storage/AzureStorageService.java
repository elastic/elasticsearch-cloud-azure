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
import org.elasticsearch.common.blobstore.BlobMetaData;
import org.elasticsearch.common.collect.ImmutableMap;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

/**
 * Azure Storage Service interface
 * @see AzureStorageServiceImpl for Azure REST API implementation
 */
public interface AzureStorageService {
    static public final class Storage {
        @Deprecated
        public static final String ACCOUNT_DEPRECATED = "cloud.azure.storage_account";
        @Deprecated
        public static final String KEY_DEPRECATED = "cloud.azure.storage_key";

        public static final String API_IMPLEMENTATION = "cloud.azure.storage.api.impl";
        public static final String ACCOUNT = "cloud.azure.storage.account";
        public static final String KEY = "cloud.azure.storage.key";
        public static final String CONTAINER = "repositories.azure.container";
        public static final String BASE_PATH = "repositories.azure.base_path";
        public static final String CHUNK_SIZE = "repositories.azure.chunk_size";
        public static final String COMPRESS = "repositories.azure.compress";
    }

    boolean doesContainerExist(String container);

    void removeContainer(String container) throws URISyntaxException, StorageException;

    void createContainer(String container) throws URISyntaxException, StorageException;

    void deleteFiles(String container, String path) throws URISyntaxException, StorageException;

    boolean blobExists(String container, String blob) throws URISyntaxException, StorageException;

    void deleteBlob(String container, String blob) throws URISyntaxException, StorageException;

    InputStream getInputStream(String container, String blob) throws URISyntaxException, StorageException;

    OutputStream getOutputStream(String container, String blob) throws URISyntaxException, StorageException;

    ImmutableMap<String,BlobMetaData> listBlobsByPrefix(String container, String keyPath, String prefix) throws URISyntaxException, StorageException;
}
