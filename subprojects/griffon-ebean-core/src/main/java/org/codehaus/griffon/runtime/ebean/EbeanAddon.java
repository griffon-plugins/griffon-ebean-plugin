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
package org.codehaus.griffon.runtime.ebean;

import griffon.annotations.core.Nonnull;
import griffon.annotations.inject.DependsOn;
import griffon.core.GriffonApplication;
import griffon.core.env.Metadata;
import griffon.core.events.StartupStartEvent;
import griffon.plugins.ebean.DatabaseCallback;
import griffon.plugins.ebean.DatabaseFactory;
import griffon.plugins.ebean.DatabaseHandler;
import griffon.plugins.ebean.DatabaseStorage;
import griffon.plugins.monitor.MBeanManager;
import io.ebean.Database;
import org.codehaus.griffon.runtime.core.addon.AbstractGriffonAddon;
import org.codehaus.griffon.runtime.ebean.monitor.DatabaseStorageMonitor;

import javax.application.event.EventHandler;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static griffon.util.ConfigUtils.getConfigValueAsBoolean;

/**
 * @author Andres Almiray
 */
@DependsOn("datasource")
@Named("ebean")
public class EbeanAddon extends AbstractGriffonAddon {
    @Inject
    private DatabaseHandler databaseHandler;

    @Inject
    private DatabaseFactory databaseFactory;

    @Inject
    private DatabaseStorage databaseStorage;

    @Inject
    private MBeanManager mbeanManager;

    @Inject
    private Metadata metadata;

    @Override
    public void init(@Nonnull GriffonApplication application) {
        mbeanManager.registerMBean(new DatabaseStorageMonitor(metadata, databaseStorage));
    }

    @EventHandler
    public void handleStartupStartEvent(@Nonnull StartupStartEvent event) {
        for (String databaseName : databaseFactory.getDatabaseNames()) {
            Map<String, Object> config = databaseFactory.getConfigurationFor(databaseName);
            if (getConfigValueAsBoolean(config, "connect_on_startup", false)) {
                databaseHandler.withEbean(databaseName, new DatabaseCallback<Void>() {
                    @Override
                    public Void handle(@Nonnull String databaseName, @Nonnull Database database) {
                        return null;
                    }
                });
            }
        }
    }

    @Override
    public void onShutdown(@Nonnull GriffonApplication application) {
        for (String databaseName : databaseFactory.getDatabaseNames()) {
            databaseHandler.closeEbean(databaseName);
        }
    }
}
