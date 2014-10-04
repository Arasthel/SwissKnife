package com.arasthel.swissknife.utils

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import groovy.transform.Field
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.commons.Method
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types

import java.lang.reflect.ParameterizedType;

/**
 * Created by Arasthel on 17/08/14.
 */
public class AnnotationUtils {

    public static MethodNode getInjectViewsMethod(ClassNode declaringClass) {
        Parameter[] parameters = [new Parameter(ClassHelper.make(Object.class), "view")];

        MethodNode injectMethod = declaringClass.getMethod("injectViews", parameters);
        if (injectMethod == null) {
            injectMethod = createInjectMethod();
            declaringClass.addMethod(injectMethod);
        }

        return injectMethod;
    }

    private static MethodNode createInjectMethod() {

        BlockStatement blockStatement = new BlockStatement();

        int tokenType = Types.EQUAL;

         ExpressionStatement expressionStatement = new ExpressionStatement(
            new DeclarationExpression(
                    new VariableExpression("v", ClassHelper.make(View.class)),
                    new Token(tokenType, "=", -1, -1),
                    new ConstantExpression(null)));


        blockStatement.addStatement(expressionStatement);

        Parameter[] parameters =  [new Parameter(ClassHelper.make(Object.class), "view")];

        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(TypeChecked.class));
        annotationNode.addMember("value", new PropertyExpression(
                new ClassExpression(ClassHelper.make(TypeCheckingMode.class)),
                new ConstantExpression(TypeCheckingMode.SKIP)));

        MethodNode node = new MethodNode("injectViews",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                ClassHelper.VOID_TYPE,
                parameters,
                null,
                blockStatement);

        node.addAnnotation(annotationNode);

        return node;
    }

    public static ExpressionStatement createInjectExpression(String id) {

        return new AstBuilder().buildFromSpec {
            expression {
                binary {
                    variable "v"
                    token "="
                    staticMethodCall(Finder.class, "findView") {
                        argumentList {
                            variable "view"
                            constant id
                        }
                    }
                }
            }
        }[0];
    }

    public static boolean isSubtype(Class original, Class compared) {
        while(original.name != compared.name) {
            original = original.getSuperclass();
            if(original == Object || original == null) {
                return false;
            }
        }
        return true;
    }

}
