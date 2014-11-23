package com.arasthel.swissknife.annotations

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.android.ast.AstUtils
import com.arasthel.swissknife.utils.AnnotationUtils
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.ASTHelper
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Created by Dexafree on 02/10/14.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ToJsonTransformation implements ASTTransformation, Opcodes {

    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        AnnotationNode annotation = astNodes[0];
        ClassNode annotatedClass = astNodes[1];

        MethodNode methodNode = null

        if(!annotatedClass.hasMethod("asSimpleJson", [new Parameter(ClassHelper.make(Closure.class), "closure")] as Parameter[])) {
            methodNode = createAsSimpleJsonMethod(annotatedClass)
        } else {
            methodNode = annotatedClass.getMethod("asSimpleJson", [new Parameter(ClassHelper.make(Closure.class), "closure")] as Parameter[])
        }

        BlockStatement block = methodNode.getCode() as BlockStatement

        block.addStatement(createMapStatement())

        (annotation.members.includes as ListExpression).expressions.each {
            String property = it.property.getValue() as String
            block.addStatement(createPutExpression(property))
        }

        block.addStatement(createCallClosureExpression())

        block.addStatement(createReturnExpression())
    }

    private MethodNode createAsSimpleJsonMethod(ClassNode classNode) {
        MethodNode methodNode = new MethodNode("asSimpleJson",
                ACC_PUBLIC,
                ClassHelper.make(Map.class),
                [new Parameter(ClassHelper.make(Closure.class), "closure")] as Parameter[], null, null)

        classNode.addMethod(methodNode)

        return methodNode
    }

    private ExpressionStatement createReturnExpression() {
        return new AstBuilder().buildFromSpec {
            expression {
                returnStatement {
                    variable "map"
                }
            }
        }[0]
    }

    private ExpressionStatement createCallClosureExpression() {
        return new AstBuilder().buildFromSpec {
            expression {
                methodCall {
                    variable "closure"
                    constant "call"
                    argumentList {}
                }
            }
        }[0]
    }

    private ExpressionStatement createPutExpression(String propertyName) {
        ExpressionStatement statement = new AstBuilder().buildFromSpec {
            expression {
                methodCall {
                    variable "map"
                    constant "put"
                    argumentList {
                        constant propertyName
                        variable propertyName
                    }
                }
            }
        }[0]

        MethodCallExpression methodCallExpression = statement.getExpression() as MethodCallExpression
        methodCallExpression.setSafe(false)

        return statement
    }

    private ExpressionStatement createMapStatement() {
        return new AstBuilder().buildFromSpec {
            expression {
                declaration {
                    variable "map"
                    token "="
                    constructorCall HashMap.class, {
                        argumentList {}
                    }
                }
            }
        }[0]
    }
}