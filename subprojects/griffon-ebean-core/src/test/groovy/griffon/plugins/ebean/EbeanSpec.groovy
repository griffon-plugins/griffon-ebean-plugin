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
package griffon.plugins.ebean

import griffon.plugins.ebean.domain.Person
import griffon.plugins.ebean.exceptions.RuntimeDatabaseException
import griffon.annotations.inject.BindTo
import griffon.core.GriffonApplication
import griffon.plugins.datasource.events.DataSourceConnectEndEvent
import griffon.plugins.datasource.events.DataSourceConnectStartEvent
import griffon.plugins.datasource.events.DataSourceDisconnectEndEvent
import griffon.plugins.datasource.events.DataSourceDisconnectStartEvent
import griffon.plugins.ebean.events.DatabaseConnectEndEvent
import griffon.plugins.ebean.events.DatabaseConnectStartEvent
import griffon.plugins.ebean.events.DatabaseDisconnectEndEvent
import griffon.plugins.ebean.events.DatabaseDisconnectStartEvent
import griffon.test.core.GriffonUnitRule
import io.ebean.Database
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import javax.application.event.EventHandler
import javax.inject.Inject

@Unroll
class EbeanSpec extends Specification {
    static {
        System.setProperty('org.slf4j.simpleLogger.defaultLogLevel', 'trace')
    }

    @Rule
    public final GriffonUnitRule griffon = new GriffonUnitRule()

    @Inject
    private DatabaseHandler databaseHandler

    @Inject
    private GriffonApplication application

    void 'Open and close default ebean'() {
        given:
        List eventNames = [
            'DatabaseConnectStartEvent', 'DataSourceConnectStartEvent',
            'DataSourceConnectEndEvent', 'DatabaseConnectEndEvent',
            'DatabaseDisconnectStartEvent', 'DataSourceDisconnectStartEvent',
            'DataSourceDisconnectEndEvent', 'DatabaseDisconnectEndEvent'
        ]
        TestEventHandler testEventHandler = new TestEventHandler()
        application.eventRouter.subscribe(testEventHandler)
        when:
        databaseHandler.withEbean { String databaseName, Database database ->
            true
        }
        databaseHandler.closeEbean()
        // second call should be a NOOP
        databaseHandler.closeEbean()

        then:
        testEventHandler.events.size() == 8
        testEventHandler.events == eventNames
    }

    void 'Connect to default Database'() {
        expect:
        databaseHandler.withEbean { String databaseName, Database database ->
            databaseName == 'default' && database
        }
    }

    void 'Bootstrap init is called'() {
        given:
        assert !bootstrap.initWitness

        when:
        databaseHandler.withEbean { String databaseName, Database database -> }

        then:
        bootstrap.initWitness
        !bootstrap.destroyWitness
    }

    void 'Bootstrap destroy is called'() {
        given:
        assert !bootstrap.initWitness
        assert !bootstrap.destroyWitness

        when:
        databaseHandler.withEbean { String databaseName, Database database -> }
        databaseHandler.closeEbean()

        then:
        bootstrap.initWitness
        bootstrap.destroyWitness
    }

    void 'Can connect to #name Database'() {
        expect:
        databaseHandler.withEbean(name) { String databaseName, Database database ->
            databaseName == name && database
        }

        where:
        name       | _
        'default'  | _
        'internal' | _
        'people'   | _
    }

    void 'Bogus Database name (#name) results in error'() {
        when:
        databaseHandler.withEbean(name) { String databaseName, Database database ->
            true
        }

        then:
        thrown(IllegalArgumentException)

        where:
        name    | _
        null    | _
        ''      | _
        'bogus' | _
    }

    void 'Execute statements on people table'() {
        when:
        List peopleIn = databaseHandler.withEbean() { String databaseName, Database database ->
            [[id: 1, name: 'Danno', lastname: 'Ferrin'],
             [id: 2, name: 'Andres', lastname: 'Almiray'],
             [id: 3, name: 'James', lastname: 'Williams'],
             [id: 4, name: 'Guillaume', lastname: 'Laforge'],
             [id: 5, name: 'Jim', lastname: 'Shingler'],
             [id: 6, name: 'Alexander', lastname: 'Klein'],
             [id: 7, name: 'Rene', lastname: 'Groeschke']].each { data ->
                database.save(new Person(data))
            }
        }

        List peopleOut = databaseHandler.withEbean() { String databaseName, Database database ->
            database.find(Person).findList()*.asMap()
        }

        then:
        peopleIn == peopleOut
    }

    void 'A runtime exception is thrown within ebean handling'() {
        when:
        databaseHandler.withEbean { String databaseName, Database database ->
            database.save(null)
        }

        then:
        thrown(RuntimeDatabaseException)
    }

    @BindTo(EbeanBootstrap)
    private TestEbeanBootstrap bootstrap = new TestEbeanBootstrap()

    private class TestEventHandler {
        List<String> events = []

        @EventHandler
        void handleDataSourceConnectStartEvent(DataSourceConnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceConnectEndEvent(DataSourceConnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceDisconnectStartEvent(DataSourceDisconnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceDisconnectEndEvent(DataSourceDisconnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDatabaseConnectStartEvent(DatabaseConnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDatabaseConnectEndEvent(DatabaseConnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDatabaseDisconnectStartEvent(DatabaseDisconnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDatabaseDisconnectEndEvent(DatabaseDisconnectEndEvent event) {
            events << event.class.simpleName
        }
    }
}
