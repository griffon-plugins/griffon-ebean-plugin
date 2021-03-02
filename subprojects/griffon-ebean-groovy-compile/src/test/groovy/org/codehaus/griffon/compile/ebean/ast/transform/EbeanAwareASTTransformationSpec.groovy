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
package org.codehaus.griffon.compile.ebean.ast.transform

import griffon.plugins.ebean.DatabaseHandler
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
        @griffon.transform.ebean.EbeanAware
        class Bean { }
        new Bean()
        ''')

        then:
        bean instanceof DatabaseHandler
        DatabaseHandler.methods.every { Method target ->
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
        import griffon.plugins.ebean.DatabaseCallback
        import griffon.plugins.ebean.exceptions.RuntimeDatabaseException
        import griffon.plugins.ebean.DatabaseHandler
        import griffon.annotations.core.Nonnull
        
        @griffon.transform.ebean.EbeanAware
        class EbeanHandlerBean implements DatabaseHandler {
            @Override
            public <R> R withEbean(@Nonnull DatabaseCallback<R> callback) throws RuntimeDatabaseException {
                return null
            }
            @Override
            public <R> R withEbean(@Nonnull String databaseName, @Nonnull DatabaseCallback<R> callback) throws RuntimeDatabaseException {
                return null
            }
            @Override
            void closeEbean(){}
            @Override
            void closeEbean(@Nonnull String databaseName){}
        }
        new EbeanHandlerBean()
        ''')

        then:
        bean instanceof DatabaseHandler
        DatabaseHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }
}
