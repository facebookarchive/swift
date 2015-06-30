/*
 * Copyright (C) 2013 Facebook, Inc.
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
package com.facebook.swift.javadoc;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
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

@SupportedAnnotationTypes({"com.facebook.swift.service.ThriftService",
                           "com.facebook.swift.codec.ThriftStruct",
                           "com.facebook.swift.codec.ThriftUnion",
                           "com.facebook.swift.codec.ThriftEnum"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class JavaDocProcessor extends AbstractProcessor
{
    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    private class ClassData {
        List<String> classDoc;
        Map<String, FieldOrMethodDoc> memberDoc;
        Map<String, Integer> orderMap;
        String packageName;
    }

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
        Map<String, ClassData> classesData = new LinkedHashMap<>();

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement) {
                    note("Processing compile-time metadata of %s", element);
                    export((TypeElement) element, classesData);
                }
            }
        }

        for (Map.Entry<String, ClassData> classMap : classesData.entrySet()) {
            String className = classMap.getKey();
            ClassData classData = classMap.getValue();

            if (classData.classDoc == null && classData.memberDoc == null && classData.orderMap == null) {
                continue;
            }
            FileObject file;
            try {
                file = filer.createSourceFile(className + "$swift_meta", null);
            } catch (IOException e) {
                error("Failed to create %s$swift_meta file: %s", className, e.toString());
                return false;
            }

            try (PrintStream out = new PrintStream(file.openOutputStream())) {
                out.printf("package %s;%n", classData.packageName);
                out.println();
                out.println("import com.facebook.swift.codec.ThriftDocumentation;");
                out.println("import com.facebook.swift.codec.ThriftOrder;");
                out.println();

                printDoc(out, classData.classDoc, 0);

                // need to do the indexOf() stuff in order to handle nested classes properly
                out.printf("class %s$swift_meta%n", className.substring(className.lastIndexOf('.') + 1));
                out.println("{");

                if (classData.memberDoc != null) {
                    for (Map.Entry<String, FieldOrMethodDoc> entry : classData.memberDoc.entrySet()) {
                        String name = entry.getKey();
                        FieldOrMethodDoc docs = entry.getValue();

                        printDoc(out, docs.getDoc(), 1);
                        if (docs.getFieldOrMethod() == FieldOrMethodDoc.FieldOrMethod.METHOD) {
                            if (classData.orderMap != null) {
                                Integer order = classData.orderMap.get(name);
                                if (order != null) {
                                    out.printf("    @ThriftOrder(%d)%n", order);
                                }
                            }
                            out.printf("    private void %s() {}%n", name);
                        } else if (docs.getFieldOrMethod() == FieldOrMethodDoc.FieldOrMethod.FIELD) {
                            out.printf("    private int %s;%n", name);
                        }
                        out.println();
                    }
                }

                out.println("}");
            } catch (IOException e) {
                error("Failed to write to %s$swift_meta file: %s", className, e.toString());
            }
        }

        return false;
    }

    private static class FieldOrMethodDoc
    {
        public enum FieldOrMethod
        {
            FIELD, METHOD
        }

        private FieldOrMethod fOrM;
        private List<String> doc;

        public FieldOrMethod getFieldOrMethod() { return fOrM; }
        private List<String> getDoc() { return doc; }

        public FieldOrMethodDoc(FieldOrMethod fieldOrMethod, List<String> doc) {
            this.fOrM = fieldOrMethod;
            this.doc = doc;
        }
    }

    private void putMemberDoc(ClassData classData, String key, FieldOrMethodDoc value) {
        if (classData.memberDoc == null) {
            classData.memberDoc = new LinkedHashMap<>();
        }
        if (!classData.memberDoc.containsKey(key)) {
            classData.memberDoc.put(key, value);
        }
    }

    private void putMethodOrder(ClassData classData, String key, Integer value) {
        if (classData.orderMap == null) {
            classData.orderMap = new LinkedHashMap<>();
        }
        if (!classData.orderMap.containsKey(key)) {
            classData.orderMap.put(key, value);
        }
    }

    private ClassData getOrCreate(Map<String, ClassData> classesData, String key) {
        if (!classesData.containsKey(key)) {
            classesData.put(key, new ClassData());
        }
        return classesData.get(key);
    }

    @SuppressWarnings("PMD.UselessParentheses")
    private void export(TypeElement typeElement, Map<String, ClassData> classesData)
    {
        String thisClassName = typeElement.getQualifiedName().toString();
        if (thisClassName.contains("$swift_meta")) {
            return;
        }

        switch (typeElement.getKind()) {
            case CLASS:
            case INTERFACE:
            case ENUM:
                break;
            default:
                warn("Non-class was annotated: %s %s", typeElement.getKind(), typeElement);

                return;
        }

        List<String> classComment = getComment(typeElement);
        if (!classComment.isEmpty()) {
            ClassData thisClassData = getOrCreate(classesData, thisClassName);
            thisClassData.classDoc = classComment;
            thisClassData.packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        }

        // offset auto-generated order numbers so they don't collide with hand-written ones
        int orderCounter = 10000;
        for (Element member : elementUtils.getAllMembers(typeElement)) {
            if (member instanceof ExecutableElement) {
                boolean isMethod = isAnnotatedWith(member, "com.facebook.swift.service.ThriftMethod");
                boolean isField = isAnnotatedWith(member, "com.facebook.swift.codec.ThriftField");
                if (isMethod || isField) {
                    // service method or method accessor for a struct field
                    String className = getClassName(member.getEnclosingElement());
                    String methodName = member.getSimpleName().toString();
                    ClassData d = getOrCreate(classesData, className);
                    putMemberDoc(d, methodName,
                            new FieldOrMethodDoc(FieldOrMethodDoc.FieldOrMethod.METHOD, getComment(member)));
                    if (d.packageName == null) {
                        d.packageName = elementUtils.getPackageOf(member).getQualifiedName().toString();
                    }
                    if (isMethod) {
                        putMethodOrder(d, methodName, orderCounter++);
                    }
                }
            } else if ((member instanceof VariableElement &&
                    isAnnotatedWith(member, "com.facebook.swift.codec.ThriftField")) ||
                    member.getKind() == ElementKind.ENUM_CONSTANT) {
                // field or enum constant
                String className = getClassName(member.getEnclosingElement());
                String fieldName = member.getSimpleName().toString();
                ClassData d = getOrCreate(classesData, className);
                putMemberDoc(d, fieldName,
                        new FieldOrMethodDoc(FieldOrMethodDoc.FieldOrMethod.FIELD, getComment(member)));
                if (d.packageName == null) {
                    d.packageName = elementUtils.getPackageOf(member).getQualifiedName().toString();
                }
            }
        }
    }

    // Returns class name in the format com.package.ClassName$Inner1$Inner2
    String getClassName(Element e) {
        // e has to be a TypeElement
        TypeElement te = (TypeElement)e;
        String packageName = elementUtils.getPackageOf(te).getQualifiedName().toString();
        String className = te.getQualifiedName().toString();
        if (className.startsWith(packageName + ".")) {
            String classAndInners = className.substring(packageName.length() + 1);
            className = packageName + "." + classAndInners.replace('.', '$');
        }
        return className;
    }

    // Derived from Apache Commons org.apache.commons.lang.StringEscapeUtils.escapeJava,
    // to be replaced by
    // com.google.common.escape.SourceCodeEscapers.javaCharEscaper().escape
    // in Guava 15 when released
    private static String escapeJavaString(String input)
    {
        int len = input.length();
        // assume (for performance, not for correctness) that string will not expand by more than 10 chars
        StringBuilder out = new StringBuilder(len + 10);
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c >= 32 && c <= 0x7f) {
                if (c == '"') {
                    out.append('\\');
                    out.append('"');
                } else if (c == '\\') {
                    out.append('\\');
                    out.append('\\');
                } else {
                    out.append(c);
                }
            } else {
                out.append('\\');
                out.append('u');
                // one liner hack to have the hex string of length exactly 4
                out.append(Integer.toHexString(c | 0x10000).substring(1));
            }
        }
        return out.toString();
    }

    private void printDoc(PrintStream out, List<String> docs, int indentLevel)
    {
        String indent = indentLevel > 0 ? String.format("%" + indentLevel*4 + "s", "") : "";
        if (docs != null && !docs.isEmpty()) {
            out.println(indent + "@ThriftDocumentation({");

            for (String doc : docs) {
                out.printf("%s    \"%s\",%n", indent, escapeJavaString(doc));
            }

            out.println(indent + "})");
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
