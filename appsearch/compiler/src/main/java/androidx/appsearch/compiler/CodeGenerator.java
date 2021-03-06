/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appsearch.compiler;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

/**
 * Generates java code for an {@link androidx.appsearch.app.AppSearchSchema} and a translator
 * between the data class and a {@link androidx.appsearch.app.GenericDocument}.
 */
class CodeGenerator {
    @VisibleForTesting
    static final String GEN_CLASS_PREFIX = "$$__AppSearch__";

    private final ProcessingEnvironment mEnv;
    private final IntrospectionHelper mHelper;
    private final AppSearchDocumentModel mModel;

    private final String mOutputPackage;
    private final TypeSpec mOutputClass;

    public static CodeGenerator generate(
            @NonNull ProcessingEnvironment env, @NonNull AppSearchDocumentModel model)
            throws ProcessingException {
        return new CodeGenerator(env, model);
    }

    private CodeGenerator(
            @NonNull ProcessingEnvironment env, @NonNull AppSearchDocumentModel model)
            throws ProcessingException {
        // Prepare constants needed for processing
        mEnv = env;
        mHelper = new IntrospectionHelper(env);
        mModel = model;

        // Perform the actual work of generating code
        mOutputPackage = mEnv.getElementUtils().getPackageOf(mModel.getClassElement()).toString();
        mOutputClass = createClass();
    }

    public void writeToFiler() throws IOException {
        JavaFile.builder(mOutputPackage, mOutputClass).build().writeTo(mEnv.getFiler());
    }

    public void writeToFolder(@NonNull File folder) throws IOException {
        JavaFile.builder(mOutputPackage, mOutputClass).build().writeTo(folder);
    }

    private TypeSpec createClass() throws ProcessingException {
        String genClassName = GEN_CLASS_PREFIX + mModel.getClassElement().getSimpleName();
        TypeName genClassType = TypeName.get(mModel.getClassElement().asType());
        TypeName factoryType = ParameterizedTypeName.get(
                mHelper.getAppSearchClass("DataClassFactory"),
                genClassType);

        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(genClassName)
                .addOriginatingElement(mModel.getClassElement())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(factoryType);

        SchemaCodeGenerator.generate(mEnv, mModel, genClass);
        ToGenericDocumentCodeGenerator.generate(mEnv, mModel, genClass);
        FromGenericDocumentCodeGenerator.generate(mEnv, mModel, genClass);
        return genClass.build();
    }
}
