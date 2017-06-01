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
import griffon.plugins.ebean.EbeanServerCallback;
import griffon.plugins.ebean.EbeanServerHandler;
import griffon.plugins.ebean.EbeanServerFactory;
import griffon.plugins.ebean.EbeanServerStorage;
import griffon.plugins.ebean.exceptions.RuntimeEbeanServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultEbeanServerHandler implements EbeanServerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultEbeanServerHandler.class);
    private static final String ERROR_SESSION_FACTORY_NAME_BLANK = "Argument 'ebeanServerName' must not be blank";
    private static final String ERROR_CALLBACK_NULL = "Argument 'callback' must not be null";

    private final EbeanServerFactory ebeanServerFactory;
    private final EbeanServerStorage ebeanServerStorage;

    @Inject
    public DefaultEbeanServerHandler(@Nonnull EbeanServerFactory ebeanServerFactory, @Nonnull EbeanServerStorage ebeanServerStorage) {
        this.ebeanServerFactory = requireNonNull(ebeanServerFactory, "Argument 'ebeanServerFactory' must not be null");
        this.ebeanServerStorage = requireNonNull(ebeanServerStorage, "Argument 'ebeanStorage' must not be null");
    }

    @Nullable
    @Override
    public <R> R withEbean(@Nonnull EbeanServerCallback<R> callback) throws RuntimeEbeanServerException {
        return withEbean(DefaultEbeanServerFactory.KEY_DEFAULT, callback);
    }

    @Nullable
    @Override
    @SuppressWarnings("ThrowFromFinallyBlock")
    public <R> R withEbean(@Nonnull String ebeanServerName, @Nonnull EbeanServerCallback<R> callback) throws RuntimeEbeanServerException {
        requireNonBlank(ebeanServerName, ERROR_SESSION_FACTORY_NAME_BLANK);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        EbeanServer ebeanServer = getEbeanServer(ebeanServerName);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing statements on ebeanServer '{}'", ebeanServerName);
        }

        try {
            return callback.handle(ebeanServerName, ebeanServer);
        } catch (Exception e) {
            throw new RuntimeEbeanServerException(ebeanServerName, e);
        }
    }

    @Override
    public void closeEbean() {
        closeEbean(DefaultEbeanServerFactory.KEY_DEFAULT);
    }

    @Override
    public void closeEbean(@Nonnull String ebeanServerName) {
        requireNonBlank(ebeanServerName, ERROR_SESSION_FACTORY_NAME_BLANK);
        EbeanServer ebean = ebeanServerStorage.get(ebeanServerName);
        if (ebean != null) {
            ebeanServerFactory.destroy(ebeanServerName, ebean);
            ebeanServerStorage.remove(ebeanServerName);
        }
    }

    @Nonnull
    private EbeanServer getEbeanServer(@Nonnull String ebeanServerName) {
        EbeanServer ebeanServer = ebeanServerStorage.get(ebeanServerName);
        if (ebeanServer == null) {
            ebeanServer = ebeanServerFactory.create(ebeanServerName);
            ebeanServerStorage.set(ebeanServerName, ebeanServer);
        }
        return ebeanServer;
    }
}
