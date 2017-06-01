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
package org.codehaus.griffon.compile.ebean.ast.transform

import griffon.plugins.ebean.EbeanServerHandler
import spock.lang.Specification

import java.lang.reflect.Method

/**
 * @author Andres Almiray
 */
class EbeanAwareASTTransformationSpec extends Specification {
    def 'EbeanAwareASTTransformation is applied to a bean via @EbeanAware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''
        @griffon.transform.EbeanAware
        class Bean { }
        new Bean()
        ''')

        then:
        bean instanceof EbeanServerHandler
        EbeanServerHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                candidate.returnType == target.returnType &&
                candidate.parameterTypes == target.parameterTypes &&
                candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }

    def 'EbeanAwareASTTransformation is not applied to a EbeanHandler subclass via @EbeanAware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''
        import griffon.plugins.ebean.EbeanServerCallback
        import griffon.plugins.ebean.exceptions.RuntimeEbeanServerException
        import griffon.plugins.ebean.EbeanServerHandler

        import javax.annotation.Nonnull
        @griffon.transform.EbeanAware
        class EbeanHandlerBean implements EbeanServerHandler {
            @Override
            public <R> R withEbean(@Nonnull EbeanServerCallback<R> callback) throws RuntimeEbeanServerException {
                return null
            }
            @Override
            public <R> R withEbean(@Nonnull String ebeanServerName, @Nonnull EbeanServerCallback<R> callback) throws RuntimeEbeanServerException {
                return null
            }
            @Override
            void closeEbean(){}
            @Override
            void closeEbean(@Nonnull String ebeanServerName){}
        }
        new EbeanHandlerBean()
        ''')

        then:
        bean instanceof EbeanServerHandler
        EbeanServerHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }
}
