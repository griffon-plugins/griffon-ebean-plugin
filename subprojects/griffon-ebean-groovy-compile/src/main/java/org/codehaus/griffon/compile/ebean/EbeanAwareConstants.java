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
package org.codehaus.griffon.compile.ebean;

import org.codehaus.griffon.compile.core.BaseConstants;
import org.codehaus.griffon.compile.core.MethodDescriptor;

import static org.codehaus.griffon.compile.core.MethodDescriptor.annotatedMethod;
import static org.codehaus.griffon.compile.core.MethodDescriptor.annotatedType;
import static org.codehaus.griffon.compile.core.MethodDescriptor.annotations;
import static org.codehaus.griffon.compile.core.MethodDescriptor.args;
import static org.codehaus.griffon.compile.core.MethodDescriptor.method;
import static org.codehaus.griffon.compile.core.MethodDescriptor.throwing;
import static org.codehaus.griffon.compile.core.MethodDescriptor.type;
import static org.codehaus.griffon.compile.core.MethodDescriptor.typeParams;
import static org.codehaus.griffon.compile.core.MethodDescriptor.types;

/**
 * @author Andres Almiray
 */
public interface EbeanAwareConstants extends BaseConstants {
    String DATABASE_TYPE = "io.ebean.Database";
    String DATABASE_HANDLER_TYPE = "griffon.plugins.ebean.DatabaseHandler";
    String DATABASE_CALLBACK_TYPE = "griffon.plugins.ebean.DatabaseCallback";
    String RUNTIME_DATABASE_EXCEPTION_TYPE = "griffon.plugins.ebean.exceptions.RuntimeDatabaseException";
    String DATABASE_HANDLER_PROPERTY = "databaseHandler";
    String DATABASE_HANDLER_FIELD_NAME = "this$" + DATABASE_HANDLER_PROPERTY;

    String METHOD_WITH_EBEAN = "withEbean";
    String METHOD_CLOSE_EBEAN = "closeEbean";
    String SESSION_FACTORY_NAME = "databaseName";
    String CALLBACK = "callback";

    MethodDescriptor[] METHODS = new MethodDescriptor[]{
        method(
            type(VOID),
            METHOD_CLOSE_EBEAN
        ),
        method(
            type(VOID),
            METHOD_CLOSE_EBEAN,
            args(annotatedType(types(type(ANNOTATION_NONNULL)), JAVA_LANG_STRING))
        ),

        annotatedMethod(
            annotations(ANNOTATION_NONNULL),
            type(R),
            typeParams(R),
            METHOD_WITH_EBEAN,
            args(annotatedType(annotations(ANNOTATION_NONNULL), DATABASE_CALLBACK_TYPE, R)),
            throwing(type(RUNTIME_DATABASE_EXCEPTION_TYPE))
        ),
        annotatedMethod(
            types(type(ANNOTATION_NONNULL)),
            type(R),
            typeParams(R),
            METHOD_WITH_EBEAN,
            args(
                annotatedType(annotations(ANNOTATION_NONNULL), JAVA_LANG_STRING),
                annotatedType(annotations(ANNOTATION_NONNULL), DATABASE_CALLBACK_TYPE, R)),
            throwing(type(RUNTIME_DATABASE_EXCEPTION_TYPE))
        )
    };
}
