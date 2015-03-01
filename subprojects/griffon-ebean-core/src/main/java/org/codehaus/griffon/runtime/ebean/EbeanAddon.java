/*
 * Copyright 2014-2015 the original author or authors.
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
import griffon.core.GriffonApplication;
import griffon.core.env.Metadata;
import griffon.inject.DependsOn;
import griffon.plugins.ebean.EbeanServerCallback;
import griffon.plugins.ebean.EbeanServerHandler;
import griffon.plugins.ebean.EbeanServerFactory;
import griffon.plugins.ebean.EbeanServerStorage;
import griffon.plugins.monitor.MBeanManager;
import org.codehaus.griffon.runtime.core.addon.AbstractGriffonAddon;
import org.codehaus.griffon.runtime.jmx.EbeanServerStorageMonitor;

import javax.annotation.Nonnull;
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
    private EbeanServerHandler ebeanServerHandler;

    @Inject
    private EbeanServerFactory ebeanServerFactory;

    @Inject
    private EbeanServerStorage ebeanServerStorage;

    @Inject
    private MBeanManager mbeanManager;

    @Inject
    private Metadata metadata;

    @Override
    public void init(@Nonnull GriffonApplication application) {
        mbeanManager.registerMBean(new EbeanServerStorageMonitor(metadata, ebeanServerStorage));
    }

    public void onStartupStart(@Nonnull GriffonApplication application) {
        for (String ebeanServerName : ebeanServerFactory.getEbeanServerNames()) {
            Map<String, Object> config = ebeanServerFactory.getConfigurationFor(ebeanServerName);
            if (getConfigValueAsBoolean(config, "connect_on_startup", false)) {
                ebeanServerHandler.withEbean(ebeanServerName, new EbeanServerCallback<Void>() {
                    @Override
                    public Void handle(@Nonnull String ebeanServerName, @Nonnull EbeanServer ebeanServer) {
                        return null;
                    }
                });
            }
        }
    }

    public void onShutdownStart(@Nonnull GriffonApplication application) {
        for (String ebeanServerName : ebeanServerFactory.getEbeanServerNames()) {
            ebeanServerHandler.closeEbean(ebeanServerName);
        }
    }
}
