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

package org.elasticsearch.cloud.azure;

import org.elasticsearch.cloud.azure.management.AzureComputeService.Management;
import org.elasticsearch.cloud.azure.storage.AzureStorageService.Storage;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.SettingsFilter;

/**
 * Filtering security azure settings
 */
public class AzureSettingsFilter implements SettingsFilter.Filter {

    @Override
    public void filter(ImmutableSettings.Builder settings) {
        // Cloud management API settings we need to hide
        settings.remove(Management.KEYSTORE_PATH);
        settings.remove(Management.KEYSTORE_PASSWORD);
        settings.remove(Management.KEYSTORE_TYPE);
        settings.remove(Management.SUBSCRIPTION_ID);

        // Deprecated Cloud management API settings we need to hide
        // TODO Remove in 3.0.0
        settings.remove(Management.KEYSTORE_DEPRECATED);
        settings.remove(Management.PASSWORD_DEPRECATED);
        settings.remove(Management.SUBSCRIPTION_ID_DEPRECATED);

        // Cloud storage API settings needed to be hidden
        settings.remove(Storage.ACCOUNT);
        settings.remove(Storage.KEY);

        // Deprecated Cloud storage API settings needed to be hidden
        // TODO Remove in 3.0.0
        settings.remove(Storage.ACCOUNT_DEPRECATED);
        settings.remove(Storage.KEY_DEPRECATED);
    }
}
