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
 */
public interface AzureClient {

    boolean doesContainerExist(String container) throws URISyntaxException, StorageException ;

    void removeContainer(String container) throws URISyntaxException, StorageException ;

    void createContainer(String container) throws URISyntaxException, StorageException ;

    void deleteFiles(String container, String path) throws URISyntaxException, StorageException ;

    boolean blobExists(String container, String blob) throws URISyntaxException, StorageException ;

    void deleteBlob(String container, String blob) throws URISyntaxException, StorageException ;

    InputStream getInputStream(String container, String blob) throws URISyntaxException, StorageException ;

    OutputStream getOutputStream(String container, String blob) throws URISyntaxException, StorageException;

    ImmutableMap<String, BlobMetaData> listBlobsByPrefix(String container, String keyPath, String prefix) throws URISyntaxException, StorageException;

}
