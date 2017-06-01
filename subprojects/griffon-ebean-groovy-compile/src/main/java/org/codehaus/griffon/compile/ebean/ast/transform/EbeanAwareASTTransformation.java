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
package org.codehaus.griffon.compile.ebean.ast.transform;

import griffon.plugins.ebean.EbeanServerHandler;
import griffon.transform.EbeanAware;
import org.codehaus.griffon.compile.core.AnnotationHandler;
import org.codehaus.griffon.compile.core.AnnotationHandlerFor;
import org.codehaus.griffon.compile.core.ast.transform.AbstractASTTransformation;
import org.codehaus.griffon.compile.ebean.EbeanAwareConstants;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static org.codehaus.griffon.compile.core.ast.GriffonASTUtils.injectInterface;

/**
 * Handles generation of code for the {@code @EbeanAware} annotation.
 *
 * @author Andres Almiray
 */
@AnnotationHandlerFor(EbeanAware.class)
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class EbeanAwareASTTransformation extends AbstractASTTransformation implements EbeanAwareConstants, AnnotationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EbeanAwareASTTransformation.class);
    private static final ClassNode EBEAN_SERVER_HANDLER_CNODE = makeClassSafe(EbeanServerHandler.class);
    private static final ClassNode EBEAN_SERVER_AWARE_CNODE = makeClassSafe(EbeanAware.class);

    /**
     * Convenience method to see if an annotated node is {@code @EbeanAware}.
     *
     * @param node the node to check
     * @return true if the node is an event publisher
     */
    public static boolean hasEbeanAwareAnnotation(AnnotatedNode node) {
        for (AnnotationNode annotation : node.getAnnotations()) {
            if (EBEAN_SERVER_AWARE_CNODE.equals(annotation.getClassNode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the bulk of the processing, mostly delegating to other methods.
     *
     * @param nodes  the ast nodes
     * @param source the source unit for the nodes
     */
    public void visit(ASTNode[] nodes, SourceUnit source) {
        checkNodesForAnnotationAndType(nodes[0], nodes[1]);
        addEbeanHandlerIfNeeded(source, (AnnotationNode) nodes[0], (ClassNode) nodes[1]);
    }

    public static void addEbeanHandlerIfNeeded(SourceUnit source, AnnotationNode annotationNode, ClassNode classNode) {
        if (needsDelegate(classNode, source, METHODS, EbeanAware.class.getSimpleName(), EBEAN_SERVER_HANDLER_TYPE)) {
            LOG.debug("Injecting {} into {}", EBEAN_SERVER_HANDLER_TYPE, classNode.getName());
            apply(classNode);
        }
    }

    /**
     * Adds the necessary field and methods to support ebean handling.
     *
     * @param declaringClass the class to which we add the support field and methods
     */
    public static void apply(@Nonnull ClassNode declaringClass) {
        injectInterface(declaringClass, EBEAN_SERVER_HANDLER_CNODE);
        Expression ebeanHandler = injectedField(declaringClass, EBEAN_SERVER_HANDLER_CNODE, EBEAN_SERVER_HANDLER_FIELD_NAME);
        addDelegateMethods(declaringClass, EBEAN_SERVER_HANDLER_CNODE, ebeanHandler);
    }
}