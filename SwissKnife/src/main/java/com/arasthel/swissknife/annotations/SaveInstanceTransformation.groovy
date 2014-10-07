package com.arasthel.swissknife.annotations

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.arasthel.swissknife.utils.AnnotationUtils
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Created by Dexafree on 02/10/14.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class SaveInstanceTransformation implements ASTTransformation, Opcodes {

    private ClassNode declaringClass
    private static ClassNode VIEW_TYPE = ClassHelper.make(View.class)

    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        AnnotationNode annotation = astNodes[0];
        FieldNode annotatedField = astNodes[1];

        declaringClass = annotatedField.declaringClass

        ClassNode annotatedFieldClassNode = annotatedField.getType()

        String annotatedFieldName = annotatedField.name

        boolean isView = annotatedFieldClassNode.isDerivedFrom(VIEW_TYPE)


        /*
         * First of all, we must check that the annotated field is a View, and if it's not,
         * it must be able to be written to a Bundle object
         * If it's not, we throw an Exception showing the field and its type
         */
        if(!isView && !AnnotationUtils.canImplementSaveState(declaringClass, annotatedField)){
            throw new Exception("Annotated field must be able to be written to a Bundle object. Field: $annotatedFieldName .Type: $annotatedFieldClassNode.name");
        }


        /*
         * Here we check if the user has passed any specific name to the SaveInstance annotation
         * If he has, the size will be 1, so we recover the passed String
         * Else, in order to avoid possible collisions with his own defined saveInstanceState defined
         * constants, we will add the "SWISSKNIFE_" prefix, and name the variable after the annotated field
         */
        String id = null

        if(annotation.members.size() > 0){
            id = annotation.members.value.text
        } else {
            id = "SWISSKNIFE_$annotatedFieldName"
        }


        /*
         * ****************************************
         * * BEGIN OF ONSAVEINSTANCESTATE SECTION *
         * ****************************************
         */

        /*
         * We get the onSaveInstanceState method
         * If it hasn't been explicitly declared at the class, we must first generate one
         * If it has already been declared at the class, we get it in order to make further modifications
         */
        MethodNode onSaveInstanceState = AnnotationUtils.getSaveStateMethod(declaringClass)

        /*
         * Here we recover the name of the variable which is passed to the onSaveInstanceState method
         * and get its name, in order to generate the statements accordingly
         */
        String bundleName = onSaveInstanceState.parameters[0].name

        Statement insertStatement = null;

        /*
         * Depending on the annotated method's class, we must get the correct statement in order
         * to add it to the onSaveInstanceState method
         */
        if(isView) {
            insertStatement = createViewSaveStateExpression(bundleName, id, annotatedField);
        } else {
            String bundleMethod = getBundleMethod(annotatedField)

            insertStatement = createSaveStateExpression(bundleName, bundleMethod, id, annotatedFieldName);
        }

        /*
         * After getting the correct method, we add it to the class
         */
        List<Statement> statementsList = ((BlockStatement)onSaveInstanceState.getCode()).getStatements()
        statementsList.add(insertStatement)


        /*
         * **************************************
         * * END OF ONSAVEINSTANCESTATE SECTION *
         * **************************************
         */

        /*
         * *********************************
         * * BEGIN OF RESTORESTATE SECTION *
         * *********************************
         */

        /*
         * We get the restoreState method
         * If it hasn't been created on a previous iteration, we must first generate one
         * If it has already been declared at the class, we get it in order to make further modifications
         */
        MethodNode restoreMethod = AnnotationUtils.getRestoreStateMethod(declaringClass)

        Statement statement = null;

        /*
         * Depending on the annotated method's class, we must get the correct statement in order
         * to add it to the onSaveInstanceState method
         */
        if(isView) {
            statement = createViewRestoreStatement(annotatedField, id);
        } else {
            statement = createRestoreStatement(annotatedField, id);
        }

        /*
         * After getting the correct method, we add it to the class
         */
        List<Statement> statementList = ((BlockStatement) restoreMethod.getCode()).getStatements();
        statementList.add(statement)
    }


    /*
     * Creates the Statement which will be used for saving a variable's content in the onSaveInstanceState method
     */
    private Statement createSaveStateExpression(String bundleName, String bundleMethod, String id, String annotatedFieldName){

        String method = "put$bundleMethod"


        ExpressionStatement statement = new AstBuilder().buildFromSpec {
            expression {
                methodCall {
                    variable bundleName
                    constant method
                    argumentList {
                        constant id
                        variable annotatedFieldName
                    }
                }
            }
        }[0]

        statement

    }


    /*
     * Creates the Statement which will be used for saving a View state in the onSaveInstanceState method
     */
    private Statement createViewSaveStateExpression(String bundleName, String id, FieldNode annotatedField){

        String method = "onSaveInstanceState"

        Statement freezeTextStatement = null;

        // If view extends TextView is needed to set "freezesText" to true
        if(AnnotationUtils.isSubtype(annotatedField.getType().getTypeClass(), TextView.class)) {
            freezeTextStatement = new AstBuilder().buildFromSpec {
                expression {
                    methodCall {
                        variable annotatedField.name
                        constant "setFreezesText"
                        argumentList {
                            constant true
                        }
                    }
                }
            }[0];
        }

        BlockStatement statement = new AstBuilder().buildFromSpec {
            block {
                expression {
                    declaration {
                        variable id
                        token "="
                        constant null
                    }
                }
                expression {
                    binary {
                        variable id
                        token "="
                        methodCall {
                            variable annotatedField.name
                            constant method
                            argumentList {}
                        }
                    }
                }
                expression {
                    methodCall {
                        variable bundleName
                        constant "putParcelable"
                        argumentList {
                            constant id
                            variable id
                        }
                    }
                }
            }
        }[0]

        if(freezeTextStatement) {
            statement.getStatements().add(0, freezeTextStatement)
        }

        statement

    }


    /*
     * Creates the Statement which will be used for restoring a View state in the restoreState method
     */
    private Statement createViewRestoreStatement(FieldNode annotatedField, String id) {

        BlockStatement statement = new AstBuilder().buildFromSpec {
            block {
                expression {
                    declaration {
                        variable id
                        token "="
                        constant null
                    }
                }
                expression {
                    binary {
                        variable id
                        token "="
                        methodCall {
                            variable "savedState"
                            constant "getParcelable"
                            argumentList {
                                constant id
                            }
                        }
                    }
                }
                expression {
                    methodCall {
                        variable annotatedField.name
                        constant "onRestoreInstanceState"
                        argumentList {
                            variable id
                        }
                    }
                }
            }
        }[0]

        statement

    }


    /*
     * Creates the Statement which will be used for restoring a variable's content in the restoreState method
     */
    private Statement createRestoreStatement(FieldNode annotatedField, String id) {

        String bundleMethod = getBundleMethod(annotatedField)

        String getBundleMethod = "get$bundleMethod"

        BlockStatement statement =
                new AstBuilder().buildFromSpec {
                    block {
                        expression {
                            binary {
                                variable annotatedField.name
                                token "="
                                methodCall {
                                    variable "savedState"
                                    constant getBundleMethod
                                    argumentList {
                                        constant id
                                    }
                                }
                            }
                        }
                    }
                }[0]

        statement

    }

    /*
     * Returns the corresponding method in order to add the content to the Bundle
     *
     * Example:
     * String -> StringArray
     * boolean[] -> BooleanArray
     * Parcelable -> Parcelable
     * Parcelable[] -> ParcelableArray
     * ArrayList<? extends Parcelable> -> ParcelableArrayList
     */
    private String getBundleMethod(FieldNode annotatedField){

        String method = null

        /*
         * We must first check if the annotated field is an array, in order to react accordingly
         */
        def isArray = annotatedField.getType().isArray()


        if(isArray){
            method = processArray(annotatedField)
        } else {
            method = processCommonVariable(annotatedField)
        }

        method

    }


    /*
     * Returns the Bundle method for a variable declared as an array
     */
    private String processArray(FieldNode annotatedField){

        String method
        String type = annotatedField.getType()

        /*
         * As String is a special case (it's not primitive type), we first check if it's that case
         */
        if(type.contains("String[]")){
            method = "String"
        } else {

            /*
             * If it's not a String, then we will check if it's a Parcelable object, so we get the
             * array's Type Class and check if it implements Parcelable
             */
            ClassNode arrayTypeClass = annotatedField.originType.componentType

            if(AnnotationUtils.doesClassImplementInterface(arrayTypeClass, "android.os.Parcelable")){
                type = "Parcelable"
            } else {

                /*
                 * If the current variable is neither a String[] or a Parcelable, we get the Type
                 * and simply capitalize the first character, because it will be a primitive Type
                 */
                type = type.substring(0, type.length()-2)

                if (Character.isLowerCase(type.charAt(0))) {

                    char first = Character.toUpperCase(type.charAt(0))

                    type = "$first" + type.substring(1)
                }
            }
        }

        /*
         * As the variable is an array, we must append the "Array" suffix
         */
        method = type+"Array"

        method
    }


    /*
     * Returns the Bundle method for a variable that has not been declared as an array
     */
    private String processCommonVariable(FieldNode annotatedField){

        String method

        ClassNode annotatedFieldClassNode = annotatedField.getType()

        Class[] classes = [String.class, int.class, byte.class, char.class, double.class,
                           boolean.class, float.class, long.class, short.class, CharSequence.class,
                           Bundle.class]



        /*
         * First we check if the variable is one of the Class objects declared at classes
         */
        classes.each {
            if (it.name == annotatedFieldClassNode.name && method == null) method = it.name
        }


        /*
         * If the variable's class was not in the declared Classs objects, it might be an ArrayList,
         * a Parcelable or a Serializable
         */
        if(method == null){

            // We create a dummy ArrayList in order to check if is instance of the variable's class
            ArrayList dummyAL = new ArrayList()

            if(annotatedFieldClassNode.name == dummyAL.class.name) method = "ArrayList"

            /*
             * If the variable is an ArrayList, we start processing it in order to get it's Generic Type
             * If the generic wasn't a valid one, method would be null again
             */
            if(method == "ArrayList"){
                method = getGenericFromArrayList(annotatedField)
            }
        }


        /*
         * If the object was not one of the declared classes or a valid ArrayList, it might be a
         * Parcelable object or a Serializable one
         */
        if(method == null){

            if(AnnotationUtils.doesClassImplementInterface(annotatedFieldClassNode, "android.os.Parcelable"))
                method = "Parcelable"
            else if (AnnotationUtils.doesClassImplementInterface(annotatedFieldClassNode, "java.io.Serializable"))
                method = "Serializable"

        }


        /*
         * If a valid type has been found, we must check that the first character is uppercase, in order
         * to add later the "put/get" prefix
         */
        if(method != null) {

            if (Character.isLowerCase(method.charAt(0))) {
                char first = Character.toUpperCase(method.charAt(0))
                method = "$first" + method.substring(1)
            }


            /*
             * If the Type name contains '.', we'll only need the last part
             */
            if (method.contains(".")) {
                String[] splits = method.split("\\.")
                method = splits[splits.length - 1]
            }

        }


        //Uncomment for debug
        //println(method)

        method

    }


    /*
     * Given an annotated ArrayList, this function returns a String which contains:
     * ArrayList-generic + "ArrayList"
     */
    private String getGenericFromArrayList(FieldNode annotatedField){

        String generic = "ArrayList"


        /*
         * First we retrieve the Generics Types found inside the ArrayList and iterate through them
         */
        GenericsType[] generics = declaringClass.getDeclaredField(annotatedField.name).type.genericsTypes


        generics.each {

            ClassNode genericClassNode = it.type


            // As we will modify the 'generic' variable, this ensures that it only will be modified once
            if(generic == "ArrayList"){

                /*
                 * If the Generic implements the Parcelable interface, the method will be ParcelableArrayList
                 */
                if(AnnotationUtils.doesClassImplementInterface(genericClassNode, "android.os.Parcelable")) {

                    generic = "Parcelable" + generic

                } else {

                    /*
                     * If the Generic is not a Parcelable, it must be one of the following classes
                     * in order to be able to be written to a Bundle object
                     */
                    switch(genericClassNode.name){
                        case Integer.class.name:
                            generic = Integer.class.name+generic
                            break

                        case Boolean.class.name:
                            generic = Boolean.class.name+generic
                            break

                        case Byte.class.name:
                            generic = Byte.class.name+generic
                            break

                        case Character.class.name:
                            generic = Character.class.name+generic
                            break

                        case CharSequence.class.name:
                            generic = CharSequence.class.name+generic
                            break

                        case Double.class.name:
                            generic = Double.class.name+generic
                            break

                        case Float.class.name:
                            generic = Float.class.name+generic
                            break

                        case Long.class.name:
                            generic = Long.class.name+generic
                            break

                        case String.class.name:
                            generic = String.class.name+generic
                            break

                        case Short.class.name:
                            generic = Short.class.name+generic
                            break

                        default:
                            break
                    }
                }
            }
        }


        /*
         * If a valid generic Type has not been found, we will set the variable again to null
         */
        if(generic == "ArrayList") generic = null

        generic
    }


}