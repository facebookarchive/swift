/**
 * Copyright 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.annotator;

import org.apache.commons.lang.StringEscapeUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes({"com.facebook.swift.service.ThriftService", "com.facebook.swift.codec.ThriftStruct"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class JavaDocProcessor extends AbstractProcessor
{
    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement) {
                    note("Processing compile-time metadata of %s", element);
                    export((TypeElement) element);
                }
            }
        }

        return false;
    }

    private void export(TypeElement typeElement)
    {
        if (typeElement.getQualifiedName().toString().contains("$swift_docs")) {
            return;
        }

        switch (typeElement.getKind()) {
            case CLASS:
            case INTERFACE:
                break;
            default:
                warn("Non-class was annotated: %s %s", typeElement.getKind(), typeElement);

                return;
        }

        FileObject file;

        try {
            file = filer.createSourceFile(typeElement.getQualifiedName() + "$swift_docs", typeElement);
        }
        catch (IOException e) {
            error("Failed to create %s$swift_docs file", typeElement);

            return;
        }

        List<String> serviceDocumentation = getComment(typeElement);
        Map<String, List<String>> methodDocumentation = new LinkedHashMap<>();

        for (Element member : elementUtils.getAllMembers(typeElement)) {
            if (member instanceof ExecutableElement && isAnnotatedWith(member, "com.facebook.swift.service.ThriftMethod")) {
                ExecutableElement executableElement = (ExecutableElement) member;
                String methodName = executableElement.getSimpleName().toString();
                List<String> methodComment = getComment(member);

                methodDocumentation.put(methodName, methodComment);
            }
        }

        try (PrintStream out = new PrintStream(file.openOutputStream())) {
            // need to do the indexOf() stuff in order to handle nested classes properly
            String binaryName = elementUtils.getBinaryName(typeElement).toString();
            String className = binaryName.substring(binaryName.lastIndexOf('.') + 1);

            out.printf("package %s;%n", elementUtils.getPackageOf(typeElement).getQualifiedName());
            out.println();
            out.println("import com.facebook.swift.codec.ThriftDocumentation;");
            out.println();

            if (!serviceDocumentation.isEmpty()) {
                out.println("@ThriftDocumentation({");

                for (String doc : serviceDocumentation) {
                    out.printf("    \"%s\",%n", StringEscapeUtils.escapeJava(doc));
                }

                out.println("})");
            }

            out.printf("class %s$swift_docs%n", className);
            out.println("{");

            for (Map.Entry<String, List<String>> entry : methodDocumentation.entrySet()) {
                String methodName = entry.getKey();
                List<String> docs = entry.getValue();

                if (!docs.isEmpty()) {
                    out.println("    @ThriftDocumentation({");

                    for (String doc : docs) {
                        out.printf("        \"%s\",%n", StringEscapeUtils.escapeJava(doc));
                    }

                    out.println("    })");
                }

                out.printf("    private void %s() {}%n", methodName);
                out.println();
            }

            out.println("}");
        }
        catch (IOException e) {
            error("Failed to write to %s$swift_docs file", typeElement);
        }
    }

    private boolean isAnnotatedWith(Element element, String annotation)
    {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotation.equals(annotationMirror.getAnnotationType().toString())) {
                return true;
            }
        }

        return false;
    }

    private List<String> getComment(Element element)
    {
        String docComment = elementUtils.getDocComment(element);

        if (docComment == null) {
            return Collections.emptyList();
        }

        if (docComment.startsWith(" ")) {
            docComment = docComment.substring(1);
        }

        return Arrays.asList(docComment.split("\n ?"));
    }

    private void note(String format, Object... args)
    {
        log(Diagnostic.Kind.NOTE, format, args);
    }

    private void warn(String format, Object... args)
    {
        log(Diagnostic.Kind.WARNING, format, args);
    }

    private void error(String format, Object... args)
    {
        log(Diagnostic.Kind.ERROR, format, args);
    }

    private void log(Diagnostic.Kind kind, String format, Object... args)
    {
        String message = format;

        if (args.length > 0) {
            try {
                message = String.format(format, args);
            }
            catch (Exception e) {
                message = format + ": " + Arrays.asList(args) + " (" + e.getMessage() + ")";
            }
        }

        messager.printMessage(kind, message);
    }
}
