/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

import com.facebook.miffed.metadata.ThriftCatalog;
import com.facebook.miffed.metadata.ThriftFieldMetadata;
import com.facebook.miffed.metadata.ThriftStructMetadata;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.facebook.miffed.compiler.Access.BRIDGE;
import static com.facebook.miffed.compiler.Access.FINAL;
import static com.facebook.miffed.compiler.Access.PUBLIC;
import static com.facebook.miffed.compiler.Access.STATIC;
import static com.facebook.miffed.compiler.Access.SUPER;
import static com.facebook.miffed.compiler.Access.SYNTHETIC;
import static com.facebook.miffed.compiler.Access.a;
import static com.facebook.miffed.compiler.CaseStatement.caseStatement;
import static com.facebook.miffed.compiler.NamedParameterDefinition.arg;
import static com.facebook.miffed.compiler.ParameterizedType.type;

public class ThriftCodecCompiler
{
    private static final String PACKAGE = "$thrift";
    private static final AtomicLong counter = new AtomicLong(42);
    private static final boolean debug = true;
    private final ThriftCatalog catalog;
    private final DynamicClassLoader classLoader;

    public ThriftCodecCompiler(ThriftCatalog catalog, DynamicClassLoader classLoader)
    {
        this.catalog = catalog;
        this.classLoader = classLoader;
    }

    public <T> ThriftTypeCodec<T> generateThriftTypeCodec(Class<T> type)
    {
        ThriftStructMetadata<?> structMetadata = catalog.getThriftStructMetadata(type);

        Class<?> codecClass = generateClass(structMetadata);
        try {
            return (ThriftTypeCodec<T>) codecClass.getField("INSTANCE").get(null);
        }
        catch (Exception e) {
            throw new IllegalStateException("Generated class is invalid", e);
        }
    }

    private Class<?> generateClass(ThriftStructMetadata<?> metadata)
    {
        ParameterizedType structType = type(metadata.getStructClass());
        ParameterizedType codecType = type(PACKAGE + "/" + structType.getClassName() + "$" + counter.incrementAndGet());

        ClassDefinition classDefinition = new ClassDefinition(
                a(PUBLIC, SUPER),
                codecType.getClassName(),
                type(Object.class),
                type(ThriftTypeCodec.class, structType));

        // public static final StructCodec INSTANCE = new StructCodec();
        {
            FieldDefinition instanceField = new FieldDefinition(a(PUBLIC, STATIC, FINAL), "INSTANCE", codecType);
            classDefinition.addField(instanceField);

            classDefinition.addMethod(new MethodDefinition(a(STATIC), "<clinit>", type(void.class))
                    .newObject(codecType)
                    .dup()
                    .invokeConstructor(codecType)
                    .putStaticField(codecType, instanceField)
                    .ret()
            );
        }


        // default constructor
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC), "<init>", type(void.class))
                    .loadThis()
                    .invokeConstructor(type(Object.class))
                    .ret());
        }

        // public Class<Struct> getType()
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC), "getType", type(Class.class, structType))
                    .loadConstant(structType)
                    .retObject());
        }

        // public Struct read(TProtocolReader protocol) throws Exception
        {
            MethodDefinition read = new MethodDefinition(a(PUBLIC), "read", structType, arg("protocol", TProtocolReader.class))
                    .addException(Exception.class);

            // declare and init local variables here
            for (ThriftFieldMetadata field : metadata.getFields()) {
                switch (field.getType().getProtocolType()) {
                    case STRING:
                        read.addStringLocalVariable("f_" + field.getName(), null);
                        break;
                    case I32:
                        read.addIntLocalVariable("f_" + field.getName(), 0);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported field type %s" + field.getType().getProtocolType());
                }
            }

            // protocol.readStructBegin();
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readStructBegin", void.class);

            // while (protocol.nextField())
            read.visitLabel("while-begin");
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "nextField", boolean.class);
            read.ifNotGoto("while-end");

            // switch (protocol.getFieldId())
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "getFieldId", short.class);
            List<CaseStatement> cases = new ArrayList<>();
            for (ThriftFieldMetadata field : metadata.getFields()) {
                cases.add(caseStatement(field.getId(), field.getName() + "-field"));
            }
            read.switchStatement("default", cases);

            for (ThriftFieldMetadata field : metadata.getFields()) {
                // case field.id:
                read.visitLabel(field.getName() + "-field");

                // read value from protocol
                switch (field.getType().getProtocolType()) {
                    case STRING:
                        read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readString", String.class);
                        break;
                    case I32:
                        read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readI32", int.class);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported field type %s" + field.getType().getProtocolType());
                }

                // store protocol value
                read.storeVariable("f_" + field.getName());

                // go back to top of loop
                read.gotoLabel("while-begin");
            }

            // default:
            read.visitLabel("default")
                    .loadVariable("protocol").invokeVirtual(TProtocolReader.class, "skipFieldData", void.class)
                    .gotoLabel("while-begin");

            // end of while loop
            read.visitLabel("while-end");

            // protocol.readStructEnd();
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readStructEnd", void.class);

            // Struct instance = new Struct();
            read.addLocalVariable("instance", structType);
            read.newObject(structType)
                    .dup()
                    .invokeConstructor(structType)
                    .storeVariable("instance");

            // inject fields
            for (ThriftFieldMetadata field : metadata.getFields()) {
                switch (field.getType().getProtocolType()) {
                    case STRING:
                        read.loadVariable("instance")
                                .loadVariable("f_" + field.getName())
                                .putField(structType, field.getName(), type(String.class));
                        break;
                    case I32:
                        read.loadVariable("instance")
                                .loadVariable("f_" + field.getName())
                                .putField(structType, field.getName(), type(int.class));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported field type %s" + field.getType().getProtocolType());
                }
            }

            read.loadVariable("instance")
                    .retObject();

            classDefinition.addMethod(read);
        }

        // public void write(Struct struct, TProtocolWriter protocol) throws Exception
        {
            MethodDefinition write = new MethodDefinition(a(PUBLIC), "write", null, arg("struct", structType), arg("protocol", TProtocolWriter.class));
            classDefinition.addMethod(write);

            write.loadVariable("protocol")
                    .loadConstant(metadata.getStructName())
                    .invokeVirtual(TProtocolWriter.class, "writeStructBegin", void.class, String.class);

            // field extraction
            for (ThriftFieldMetadata field : metadata.getFields()) {
                write.loadVariable("protocol")
                        .loadConstant(field.getName())
                        .loadConstant(field.getId())
                        .loadVariable("struct");

                switch (field.getType().getProtocolType()) {
                    case STRING:
                        write.getField(structType, field.getName(), type(String.class));
                        write.invokeVirtual(TProtocolWriter.class, "writeString", void.class, String.class, short.class, String.class);
                        break;
                    case I32:
                        write.getField(structType, field.getName(), type(int.class));
                        write.invokeVirtual(TProtocolWriter.class, "writeI32", void.class, String.class, short.class, int.class);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported field type %s" + field.getType().getProtocolType());
                }

            }

            write.loadVariable("protocol")
                    .invokeVirtual(TProtocolWriter.class, "writeStructEnd", void.class);

            write.ret();
        }

        // public synthetic bridge Object read(TProtocolReader protocol) throws Exception
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC, BRIDGE, SYNTHETIC), "read", type(Object.class), arg("protocol", TProtocolReader.class))
                    .addException(Exception.class)
                    .loadThis()
                    .loadVariable("protocol")
                    .invokeVirtual(codecType, "read", structType, type(TProtocolReader.class))
                    .retObject()
            );
        }

        // public synthetic bridge void write(Object struct, TProtocolWriter protocol) throws Exception
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC, BRIDGE, SYNTHETIC), "write", null, arg("struct", Object.class), arg("protocol", TProtocolWriter.class))
                    .addException(Exception.class)
                    .loadThis()
                    .loadVariable("struct", structType)
                    .loadVariable("protocol")
                    .invokeVirtual(codecType, "write", type(void.class), structType, type(TProtocolWriter.class))
                    .ret()
            );
        }

        ClassNode classNode = classDefinition.getClassNode();

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        byte[] byteCode = cw.toByteArray();

        if (debug) {
            ClassReader reader = new ClassReader(byteCode);
            CheckClassAdapter.verify(reader, true, new PrintWriter(System.out));
        }
        Class<?> codecClass = classLoader.defineClass(codecType.getClassName().replace('/', '.'), byteCode);

        return codecClass;
    }
}
