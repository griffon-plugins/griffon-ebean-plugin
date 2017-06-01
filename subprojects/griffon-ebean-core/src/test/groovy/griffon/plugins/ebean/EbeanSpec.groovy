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
package griffon.plugins.ebean

import com.avaje.ebean.EbeanServer
import griffon.core.GriffonApplication
import griffon.core.RunnableWithArgs
import griffon.core.test.GriffonUnitRule
import griffon.inject.BindTo
import griffon.plugins.ebean.exceptions.RuntimeEbeanServerException
import org.avaje.agentloader.AgentLoader
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Inject

@Unroll
class EbeanSpec extends Specification {
    static {
        System.setProperty('org.slf4j.simpleLogger.defaultLogLevel', 'trace')
        AgentLoader.loadAgentFromClasspath('avaje-ebeanorm-agent', 'debug=1')
    }

    @Rule
    public final GriffonUnitRule griffon = new GriffonUnitRule()

    @Inject
    private EbeanServerHandler ebeanHandler

    @Inject
    private GriffonApplication application

    void 'Open and close default ebean'() {
        given:
        List eventNames = [
            'EbeanConnectStart', 'DataSourceConnectStart',
            'DataSourceConnectEnd', 'EbeanConnectEnd',
            'EbeanDisconnectStart', 'DataSourceDisconnectStart',
            'DataSourceDisconnectEnd', 'EbeanDisconnectEnd'
        ]
        List events = []
        eventNames.each { name ->
            application.eventRouter.addEventListener(name, ({ Object... args ->
                events << [name: name, args: args]
            } as RunnableWithArgs))
        }

        when:
        ebeanHandler.withEbean { String ebeanServerName, EbeanServer ebeanServer ->
            true
        }
        ebeanHandler.closeEbean()
        // second call should be a NOOP
        ebeanHandler.closeEbean()

        then:
        events.size() == 8
        events.name == eventNames
    }

    void 'Connect to default EbeanServer'() {
        expect:
        ebeanHandler.withEbean { String ebeanServerName, EbeanServer ebeanServer ->
            ebeanServerName == 'default' && ebeanServer
        }
    }

    void 'Bootstrap init is called'() {
        given:
        assert !bootstrap.initWitness

        when:
        ebeanHandler.withEbean { String ebeanServerName, EbeanServer ebeanServer -> }

        then:
        bootstrap.initWitness
        !bootstrap.destroyWitness
    }

    void 'Bootstrap destroy is called'() {
        given:
        assert !bootstrap.initWitness
        assert !bootstrap.destroyWitness

        when:
        ebeanHandler.withEbean { String ebeanServerName, EbeanServer ebeanServer -> }
        ebeanHandler.closeEbean()

        then:
        bootstrap.initWitness
        bootstrap.destroyWitness
    }

    void 'Can connect to #name EbeanServer'() {
        expect:
        ebeanHandler.withEbean(name) { String ebeanServerName, EbeanServer ebeanServer ->
            ebeanServerName == name && ebeanServer
        }

        where:
        name       | _
        'default'  | _
        'internal' | _
        'people'   | _
    }

    void 'Bogus EbeanServer name (#name) results in error'() {
        when:
        ebeanHandler.withEbean(name) { String ebeanServerName, EbeanServer ebeanServer ->
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
        List peopleIn = ebeanHandler.withEbean() { String ebeanServerName, EbeanServer ebeanServer ->
            [[id: 1, name: 'Danno', lastname: 'Ferrin'],
             [id: 2, name: 'Andres', lastname: 'Almiray'],
             [id: 3, name: 'James', lastname: 'Williams'],
             [id: 4, name: 'Guillaume', lastname: 'Laforge'],
             [id: 5, name: 'Jim', lastname: 'Shingler'],
             [id: 6, name: 'Alexander', lastname: 'Klein'],
             [id: 7, name: 'Rene', lastname: 'Groeschke']].each { data ->
                ebeanServer.save(new Person(data))
            }
        }

        List peopleOut = ebeanHandler.withEbean() { String ebeanServerName, EbeanServer ebeanServer ->
            ebeanServer.find(Person).findList()*.asMap()
        }

        then:
        peopleIn == peopleOut
    }

    void 'A runtime exception is thrown within ebean handling'() {
        when:
        ebeanHandler.withEbean { String ebeanServerName, EbeanServer ebeanServer ->
            ebeanServer.save(null)
        }

        then:
        thrown(RuntimeEbeanServerException)
    }

    @BindTo(EbeanBootstrap)
    private TestEbeanBootstrap bootstrap = new TestEbeanBootstrap()
}
