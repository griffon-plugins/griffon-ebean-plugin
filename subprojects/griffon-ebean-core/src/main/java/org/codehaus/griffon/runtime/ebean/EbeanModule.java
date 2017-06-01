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

import griffon.core.Configuration;
import griffon.core.addon.GriffonAddon;
import griffon.core.injection.Module;
import griffon.inject.DependsOn;
import griffon.plugins.ebean.EbeanServerHandler;
import griffon.plugins.ebean.EbeanServerFactory;
import griffon.plugins.ebean.EbeanServerStorage;
import org.codehaus.griffon.runtime.core.injection.AbstractModule;
import org.codehaus.griffon.runtime.util.ResourceBundleProvider;
import org.kordamp.jipsy.ServiceProviderFor;

import javax.inject.Named;
import java.util.ResourceBundle;

import static griffon.util.AnnotationUtils.named;

/**
 * @author Andres Almiray
 */
@DependsOn("datasource")
@Named("ebean")
@ServiceProviderFor(Module.class)
public class EbeanModule extends AbstractModule {
    @Override
    protected void doConfigure() {
        // tag::bindings[]
        bind(ResourceBundle.class)
            .withClassifier(named("ebean"))
            .toProvider(new ResourceBundleProvider("Ebean"))
            .asSingleton();

        bind(Configuration.class)
            .withClassifier(named("ebean"))
            .to(DefaultEbeanConfiguration.class)
            .asSingleton();

        bind(EbeanServerStorage.class)
            .to(DefaultEbeanServerStorage.class)
            .asSingleton();

        bind(EbeanServerFactory.class)
            .to(DefaultEbeanServerFactory.class)
            .asSingleton();

        bind(EbeanServerHandler.class)
            .to(DefaultEbeanServerHandler.class)
            .asSingleton();

        bind(GriffonAddon.class)
            .to(EbeanAddon.class)
            .asSingleton();
        // end::bindings[]
    }
}
