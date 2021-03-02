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
import griffon.core.GriffonApplication;
import griffon.core.injection.Injector;
import griffon.plugins.datasource.DataSourceFactory;
import griffon.plugins.datasource.DataSourceStorage;
import griffon.plugins.ebean.DatabaseFactory;
import griffon.plugins.ebean.EbeanBootstrap;
import griffon.plugins.ebean.events.DatabaseConnectEndEvent;
import griffon.plugins.ebean.events.DatabaseConnectStartEvent;
import griffon.plugins.ebean.events.DatabaseDisconnectEndEvent;
import griffon.plugins.ebean.events.DatabaseDisconnectStartEvent;
import griffon.util.GriffonClassUtils;
import io.ebean.Database;
import io.ebean.config.DatabaseConfig;
import org.codehaus.griffon.runtime.core.storage.AbstractObjectFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static griffon.util.ConfigUtils.getConfigValue;
import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultDatabaseFactory extends AbstractObjectFactory<Database> implements DatabaseFactory {
    private static final String ERROR_SESSION_FACTORY_NAME_BLANK = "Argument 'databaseName' must not be blank";
    private static final String[] CUSTOM_PROPERTIES = {
        "connect_on_startup",
        "schema"
    };
    private final Set<String> databaseNames = new LinkedHashSet<>();
    @Inject
    private DataSourceFactory dataSourceFactory;

    @Inject
    private DataSourceStorage dataSourceStorage;

    @Inject
    private Injector injector;

    @Inject
    public DefaultDatabaseFactory(@Nonnull @Named("ebean") griffon.core.Configuration configuration, @Nonnull GriffonApplication application) {
        super(configuration, application);
        databaseNames.add(KEY_DEFAULT);

        if (configuration.containsKey(getPluralKey())) {
            Map<String, Object> ebeanFactories = (Map<String, Object>) configuration.get(getPluralKey());
            databaseNames.addAll(ebeanFactories.keySet());
        }
    }

    @Nonnull
    @Override
    public Set<String> getDatabaseNames() {
        return databaseNames;
    }

    @Nonnull
    @Override
    public Map<String, Object> getConfigurationFor(@Nonnull String databaseName) {
        requireNonBlank(databaseName, ERROR_SESSION_FACTORY_NAME_BLANK);
        return narrowConfig(databaseName);
    }

    @Nonnull
    @Override
    protected String getSingleKey() {
        return "database";
    }

    @Nonnull
    @Override
    protected String getPluralKey() {
        return "databases";
    }

    @Nonnull
    @Override
    public Database create(@Nonnull String name) {
        Map<String, Object> config = narrowConfig(name);
        event(DatabaseConnectStartEvent.of(name, config));

        Database ebeanserver = createDatabase(config, name);
        for (Object o : injector.getInstances(EbeanBootstrap.class)) {
            ((EbeanBootstrap) o).init(name, ebeanserver);
        }

        event(DatabaseConnectEndEvent.of(name, config, ebeanserver));
        return ebeanserver;
    }

    @Override
    public void destroy(@Nonnull String name, @Nonnull Database instance) {
        requireNonNull(instance, "Argument 'instance' must not be null");
        Map<String, Object> config = narrowConfig(name);
        event(DatabaseDisconnectStartEvent.of(name, config, instance));

        for (Object o : injector.getInstances(EbeanBootstrap.class)) {
            ((EbeanBootstrap) o).destroy(name, instance);
        }

        closeDataSource(name);

        event(DatabaseDisconnectEndEvent.of(name, config));
    }

    @Nonnull
    protected Database createDatabase(@Nonnull Map<String, Object> config, @Nonnull String databaseName) {
        String schemaCreate = getConfigValue(config, "schema", "create");
        boolean ddl = "create".equalsIgnoreCase(schemaCreate);

        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setDataSource(getDataSource(databaseName));
        databaseConfig.setDdlGenerate(ddl);
        databaseConfig.setDdlRun(ddl);

        for (Map.Entry<String, Object> e : config.entrySet()) {
            if (Arrays.binarySearch(CUSTOM_PROPERTIES, e.getKey()) != -1) {
                continue;
            }
            GriffonClassUtils.setPropertyValue(databaseConfig, e.getKey(), e.getValue());
        }

        return io.ebean.DatabaseFactory.create(databaseConfig);
    }

    protected void closeDataSource(@Nonnull String dataSourceName) {
        DataSource dataSource = dataSourceStorage.get(dataSourceName);
        if (dataSource != null) {
            dataSourceFactory.destroy(dataSourceName, dataSource);
            dataSourceStorage.remove(dataSourceName);
        }
    }

    @Nonnull
    protected DataSource getDataSource(@Nonnull String dataSourceName) {
        DataSource dataSource = dataSourceStorage.get(dataSourceName);
        if (dataSource == null) {
            dataSource = dataSourceFactory.create(dataSourceName);
            dataSourceStorage.set(dataSourceName, dataSource);
        }
        return dataSource;
    }
}
