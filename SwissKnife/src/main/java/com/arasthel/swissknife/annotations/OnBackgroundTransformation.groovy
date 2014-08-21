package com.arasthel.swissknife.annotations

import com.arasthel.swissknife.SwissKnife
import com.arasthel.swissknife.utils.AstNodeToScriptVisitor
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Created by Arasthel on 16/08/14.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class OnBackgroundTransformation implements ASTTransformation, Opcodes {

    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        MethodNode annotatedMethod = astNodes[1];
        AnnotationNode annotation = astNodes[0];
        ClassNode declaringClass = annotatedMethod.declaringClass;

        BlockStatement originalCode = annotatedMethod.getCode();
        declaringClass.addMethod(createNewMethod(annotatedMethod, originalCode));

        createRunnable(annotatedMethod);
    }

    private MethodNode createNewMethod(MethodNode annotatedMethod, BlockStatement originalCode) {
        return new MethodNode(annotatedMethod.name+"\$background", ACC_PUBLIC, ClassHelper.VOID_TYPE, annotatedMethod.parameters, null, originalCode);
    }

    private void createRunnable(MethodNode annotatedMethod) {
        BlockStatement blockStatement = new BlockStatement();

        ArgumentListExpression argumentListExpression = new ArgumentListExpression();
        argumentListExpression.addExpression(new VariableExpression("this"));
        argumentListExpression.addExpression(new ConstantExpression(annotatedMethod.name + "\$background"));

        for(Parameter parameter : annotatedMethod.parameters) {
            argumentListExpression.addExpression(new VariableExpression(parameter.name));
        }

        StaticMethodCallExpression staticMethodCallExpression = new StaticMethodCallExpression(ClassHelper.make(SwissKnife.class), "runOnBackground", argumentListExpression);

        ExpressionStatement expressionStatement = new ExpressionStatement(staticMethodCallExpression);

        blockStatement.addStatement(expressionStatement);

        annotatedMethod.setCode(blockStatement);
    }

}

