package com.arasthel.swissknife.utils

import android.os.Bundle
import android.view.View
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types

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



    private static MethodNode createRestoreStateMethod(){

        BlockStatement blockStatement = new BlockStatement()

        Parameter[] parameters =  [new Parameter(ClassHelper.make(Bundle.class), "savedState")];

        int tokenType = Types.EQUAL;

        ExpressionStatement expressionStatement = new ExpressionStatement(
                new DeclarationExpression(
                        new VariableExpression("o", ClassHelper.make(Object.class)),
                        new Token(tokenType, "=", -1, -1),
                        new ConstantExpression(null)));

        blockStatement.addStatement(expressionStatement)

        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(TypeChecked.class));
        annotationNode.addMember("value", new PropertyExpression(
                new ClassExpression(ClassHelper.make(TypeCheckingMode.class)),
                new ConstantExpression(TypeCheckingMode.SKIP)));

        MethodNode node = new MethodNode("restoreSavedState",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                ClassHelper.VOID_TYPE,
                parameters,
                null,
                blockStatement);

        node.addAnnotation(annotationNode);

        return node;
    }

    private static MethodNode createSaveStateMethod(){

        BlockStatement blockStatement = new BlockStatement()

        Parameter[] parameters =  [new Parameter(ClassHelper.make(Bundle.class), "savedState")];

        ExpressionStatement expressionStatement =
                new AstBuilder().buildFromSpec {
                    expression{
                        methodCall {
                            variable "super"
                            constant "onSaveInstanceState"
                            argumentList {
                                variable "savedState"
                            }
                        }
                    }
                }[0]

        blockStatement.addStatement(expressionStatement)

        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(TypeChecked.class));
        annotationNode.addMember("value", new PropertyExpression(
                new ClassExpression(ClassHelper.make(TypeCheckingMode.class)),
                new ConstantExpression(TypeCheckingMode.SKIP)));


        AnnotationNode overrideAnnotation = new AnnotationNode(ClassHelper.make(Override.class))

        MethodNode node = new MethodNode("onSaveInstanceState",
                Opcodes.ACC_PUBLIC,
                ClassHelper.VOID_TYPE,
                parameters,
                null,
                blockStatement);


        node.addAnnotation(annotationNode);
        node.addAnnotation(overrideAnnotation)

        return node;

    }


    public static boolean isSubtype(ClassNode original, Class compared) {
        while(original.name != compared.name) {
            original = original.getSuperclass();
            if(original == Object || original == null) {
                return false;
            }
        }
        return true;
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

    public static MethodNode getSaveStateMethod(ClassNode declaringClass){
        Parameter[] parameters = [new Parameter(ClassHelper.make(Bundle.class), "outState")]
        MethodNode saveStateMethod = declaringClass.getDeclaredMethod("onSaveInstanceState", parameters)
        if(saveStateMethod == null){
            saveStateMethod = createSaveStateMethod()
            declaringClass.addMethod(saveStateMethod)
        }


        def typechecked = false

        saveStateMethod.annotations.each {
            if(it.getClassNode().name == "groovy.transform.TypeChecked") typechecked = true
        }

        if(!typechecked) addTypeCheckedAnnotation(saveStateMethod)


        saveStateMethod

    }

    public static MethodNode getRestoreStateMethod(ClassNode declaringClass) {
        Parameter[] parameters = [new Parameter(ClassHelper.make(Bundle.class), "savedState")]

        MethodNode restoreStateMethod = declaringClass.getMethod("restoreSavedState", parameters)
        if (restoreStateMethod == null) {
            restoreStateMethod = createRestoreStateMethod()
            declaringClass.addMethod(restoreStateMethod)
        }
        restoreStateMethod
    }

    private static addTypeCheckedAnnotation(MethodNode saveStateMethod){
        AnnotationNode annotationNode = new AnnotationNode(ClassHelper.make(TypeChecked.class));
        annotationNode.addMember("value", new PropertyExpression(
                new ClassExpression(ClassHelper.make(TypeCheckingMode.class)),
                new ConstantExpression(TypeCheckingMode.SKIP)));

        saveStateMethod.addAnnotation(annotationNode)
    }


    public static boolean canImplementSaveState(ClassNode declaringClass, FieldNode annotatedField){

        def canImplement = false

        Class[] classes = [String.class, int.class, byte.class, char.class, double.class, boolean.class,
                           float.class, long.class, short.class, Integer.class, CharSequence.class,
                           Bundle.class]



        ClassNode originalClassNode = annotatedField.getType()


        classes.each {
            if (it.name == originalClassNode.name) canImplement = true
        }

        if(!canImplement){

            def containsGenerics = false

            ArrayList dummyAL = new ArrayList()

            if(originalClassNode.name == dummyAL.class.name) containsGenerics = true

            if(containsGenerics){
                GenericsType[] generics = declaringClass.getDeclaredField(annotatedField.name).type.genericsTypes

                generics.each {

                    ClassNode genericClassNode = it.type

                    // Here we don't check Serializable because Bundle does not support putSerializableArrayList
                    if(!canImplement) canImplement = doesClassImplementInterface(genericClassNode, "android.os.Parcelable")

                    if(!canImplement){

                        switch(genericClassNode.name){

                            case [Integer.class.name, Boolean.class.name, Byte.class.name,
                                  Character.class.name, CharSequence.class.name, Double.class.name,
                                  Float.class.name, Long.class.name, String.class.name,
                                  Short.class.name]:
                                canImplement = true
                                break
                            default:
                                canImplement = false
                                break

                        }
                    }
                }
            }

        }

        if(!canImplement) canImplement = doesClassImplementInterface(originalClassNode, "android.os.Parcelable") ||
                doesClassImplementInterface(originalClassNode, "java.io.Serializable")

        canImplement

    }

    public static boolean doesClassImplementInterface(Class original, String desiredInterface){

        def interfaces = original.getInterfaces()

        def implementsInterface = false

        interfaces.each {
            if(it.getName().equalsIgnoreCase(desiredInterface)) implementsInterface = true
        }

        implementsInterface

    }

    public static boolean doesClassImplementInterface(ClassNode original, String desiredInterface){

        def interfaces = original.getInterfaces()

        def implementsInterface = false

        interfaces.each {
            if(it.getName().equalsIgnoreCase(desiredInterface)) implementsInterface = true
        }

        implementsInterface

    }


}
