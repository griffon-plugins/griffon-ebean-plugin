/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2021 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.ebean.monitor;

import griffon.annotations.core.Nonnull;
import griffon.core.env.Metadata;
import griffon.plugins.ebean.DatabaseStorage;
import io.ebean.Database;
import org.codehaus.griffon.runtime.monitor.AbstractObjectStorageMonitor;

/**
 * @author Andres Almiray
 */
public class DatabaseStorageMonitor extends AbstractObjectStorageMonitor<Database> implements DatabaseStorageMonitorMXBean {
    public DatabaseStorageMonitor(@Nonnull Metadata metadata, @Nonnull DatabaseStorage delegate) {
        super(metadata, delegate);
    }

    @Override
    protected String getStorageName() {
        return "ebean";
    }
}
