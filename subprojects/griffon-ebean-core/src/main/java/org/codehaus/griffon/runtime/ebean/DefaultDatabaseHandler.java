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
import griffon.annotations.core.Nullable;
import griffon.plugins.ebean.DatabaseCallback;
import griffon.plugins.ebean.DatabaseFactory;
import griffon.plugins.ebean.DatabaseHandler;
import griffon.plugins.ebean.DatabaseStorage;
import griffon.plugins.ebean.exceptions.RuntimeDatabaseException;
import io.ebean.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultDatabaseHandler implements DatabaseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDatabaseHandler.class);
    private static final String ERROR_SESSION_FACTORY_NAME_BLANK = "Argument 'databaseName' must not be blank";
    private static final String ERROR_CALLBACK_NULL = "Argument 'callback' must not be null";

    private final DatabaseFactory databaseFactory;
    private final DatabaseStorage databaseStorage;

    @Inject
    public DefaultDatabaseHandler(@Nonnull DatabaseFactory databaseFactory, @Nonnull DatabaseStorage databaseStorage) {
        this.databaseFactory = requireNonNull(databaseFactory, "Argument 'databaseFactory' must not be null");
        this.databaseStorage = requireNonNull(databaseStorage, "Argument 'databaseStorage' must not be null");
    }

    @Nullable
    @Override
    public <R> R withEbean(@Nonnull DatabaseCallback<R> callback) throws RuntimeDatabaseException {
        return withEbean(DefaultDatabaseFactory.KEY_DEFAULT, callback);
    }

    @Nullable
    @Override
    @SuppressWarnings("ThrowFromFinallyBlock")
    public <R> R withEbean(@Nonnull String databaseName, @Nonnull DatabaseCallback<R> callback) throws RuntimeDatabaseException {
        requireNonBlank(databaseName, ERROR_SESSION_FACTORY_NAME_BLANK);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        Database database = getDatabase(databaseName);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing statements on database '{}'", databaseName);
        }

        try {
            return callback.handle(databaseName, database);
        } catch (Exception e) {
            throw new RuntimeDatabaseException(databaseName, e);
        }
    }

    @Override
    public void closeEbean() {
        closeEbean(DefaultDatabaseFactory.KEY_DEFAULT);
    }

    @Override
    public void closeEbean(@Nonnull String databaseName) {
        requireNonBlank(databaseName, ERROR_SESSION_FACTORY_NAME_BLANK);
        Database ebean = databaseStorage.get(databaseName);
        if (ebean != null) {
            databaseFactory.destroy(databaseName, ebean);
            databaseStorage.remove(databaseName);
        }
    }

    @Nonnull
    private Database getDatabase(@Nonnull String databaseName) {
        Database database = databaseStorage.get(databaseName);
        if (database == null) {
            database = databaseFactory.create(databaseName);
            databaseStorage.set(databaseName, database);
        }
        return database;
    }
}
