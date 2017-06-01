/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.ebean;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.ServerConfig;
import griffon.core.GriffonApplication;
import griffon.core.injection.Injector;
import griffon.plugins.datasource.DataSourceFactory;
import griffon.plugins.datasource.DataSourceStorage;
import griffon.plugins.ebean.EbeanBootstrap;
import griffon.plugins.ebean.EbeanServerFactory;
import griffon.util.GriffonClassUtils;
import org.codehaus.griffon.runtime.core.storage.AbstractObjectFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static griffon.util.ConfigUtils.getConfigValue;
import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultEbeanServerFactory extends AbstractObjectFactory<EbeanServer> implements EbeanServerFactory {
    private static final String ERROR_SESSION_FACTORY_NAME_BLANK = "Argument 'ebeanServerName' must not be blank";
    private final Set<String> ebeanServerNames = new LinkedHashSet<>();

    private static final String[] CUSTOM_PROPERTIES = {
        "connect_on_startup",
        "schema"
    };

    @Inject
    private DataSourceFactory dataSourceFactory;

    @Inject
    private DataSourceStorage dataSourceStorage;

    @Inject
    private Injector injector;

    @Inject
    public DefaultEbeanServerFactory(@Nonnull @Named("ebean") griffon.core.Configuration configuration, @Nonnull GriffonApplication application) {
        super(configuration, application);
        ebeanServerNames.add(KEY_DEFAULT);

        if (configuration.containsKey(getPluralKey())) {
            Map<String, Object> ebeanFactories = (Map<String, Object>) configuration.get(getPluralKey());
            ebeanServerNames.addAll(ebeanFactories.keySet());
        }
    }

    @Nonnull
    @Override
    public Set<String> getEbeanServerNames() {
        return ebeanServerNames;
    }

    @Nonnull
    @Override
    public Map<String, Object> getConfigurationFor(@Nonnull String ebeanServerName) {
        requireNonBlank(ebeanServerName, ERROR_SESSION_FACTORY_NAME_BLANK);
        return narrowConfig(ebeanServerName);
    }

    @Nonnull
    @Override
    protected String getSingleKey() {
        return "ebeanServer";
    }

    @Nonnull
    @Override
    protected String getPluralKey() {
        return "ebeanServers";
    }

    @Nonnull
    @Override
    public EbeanServer create(@Nonnull String name) {
        Map<String, Object> config = narrowConfig(name);
        event("EbeanConnectStart", asList(name, config));

        EbeanServer ebeanserver = createEbeanServer(config, name);
        for (Object o : injector.getInstances(EbeanBootstrap.class)) {
            ((EbeanBootstrap) o).init(name, ebeanserver);
        }

        event("EbeanConnectEnd", asList(name, config, ebeanserver));
        return ebeanserver;
    }

    @Override
    public void destroy(@Nonnull String name, @Nonnull EbeanServer instance) {
        requireNonNull(instance, "Argument 'instance' must not be null");
        Map<String, Object> config = narrowConfig(name);
        event("EbeanDisconnectStart", asList(name, config, instance));

        for (Object o : injector.getInstances(EbeanBootstrap.class)) {
            ((EbeanBootstrap) o).destroy(name, instance);
        }

        closeDataSource(name);

        event("EbeanDisconnectEnd", asList(name, config));
    }

    @Nonnull

    protected EbeanServer createEbeanServer(@Nonnull Map<String, Object> config, @Nonnull String ebeanServerName) {
        String schemaCreate = getConfigValue(config, "schema", "create");
        boolean ddl = "create".equalsIgnoreCase(schemaCreate);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName(ebeanServerName);
        serverConfig.setRegister(true);
        serverConfig.setDefaultServer(KEY_DEFAULT.equals(ebeanServerName));
        serverConfig.setDdlGenerate(ddl);
        serverConfig.setDdlRun(ddl);
        serverConfig.setDataSource(getDataSource(ebeanServerName));

        for (Map.Entry<String, Object> e : config.entrySet()) {
            if (Arrays.binarySearch(CUSTOM_PROPERTIES, e.getKey()) != -1) {
                continue;
            }
            GriffonClassUtils.setPropertyValue(serverConfig, e.getKey(), e.getValue());
        }

        return com.avaje.ebean.EbeanServerFactory.create(serverConfig);
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
