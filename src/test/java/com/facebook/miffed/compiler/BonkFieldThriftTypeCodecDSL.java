/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.miffed.compiler;

import com.google.common.base.Throwables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
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
import static me.qmx.jitescript.util.CodegenUtils.p;

public class BonkFieldThriftTypeCodecDSL implements Opcodes
{
    private static final String PACKAGE = "$thrift";
    private static final AtomicLong counter = new AtomicLong();
    private static final boolean debug = true;
    private final DynamicClassLoader classLoader;

    public BonkFieldThriftTypeCodecDSL(DynamicClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    public <T> ThriftTypeCodec<T> genClass(Class<T> structClass)
    {
        String className = PACKAGE + "/" + p(structClass) + "$" + counter.incrementAndGet();
        byte[] byteCode = dump(structClass, className);

        if (debug) {
            ClassReader reader = new ClassReader(byteCode);
            CheckClassAdapter.verify(reader, true, new PrintWriter(System.out));
        }
        Class<?> codecClass = classLoader.defineClass(className.replace('/', '.'), byteCode);
        try {
            return (ThriftTypeCodec<T>) codecClass.getField("INSTANCE").get(null);
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }


    public static byte[] dump(Class<?> structClass, String className)
    {
        ClassDefinition classDefinition = new ClassDefinition(
                a(PUBLIC, SUPER),
                className,
                type(Object.class),
                type(ThriftTypeCodec.class, structClass));

        // public static final BonkFieldThriftTypeCodec INSTANCE = new BonkFieldThriftTypeCodec();
        {
            FieldDefinition instanceField = new FieldDefinition(a(PUBLIC, STATIC, FINAL), "INSTANCE", type(className));
            classDefinition.addField(instanceField);

            classDefinition.addMethod(new MethodDefinition(a(STATIC), "<clinit>", type(void.class))
                    .newObject(type(className))
                    .dup()
                    .invokeConstructor(type(className))
                    .putStaticField(type(className), instanceField)
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

        // public Class<BonkField> getType()
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC), "getType", type(Class.class, structClass))
                    .loadConstant(structClass)
                    .retObject());
        }

        // public BonkField read(TProtocolReader protocol) throws Exception
        {
            MethodDefinition read = new MethodDefinition(a(PUBLIC), "read", type(structClass), arg("protocol", TProtocolReader.class))
                    .addException(Exception.class);

            // declare and init local variables here
            read.addStringLocalVariable("message", "bar");
            read.addIntLocalVariable("type", 22);

            // protocol.readStructBegin();
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readStructBegin", void.class);

            // while (protocol.nextField())
            read.visitLabel("while-begin");
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "nextField", boolean.class);
            read.ifNotGoto("while-end");

            // switch (protocol.getFieldId())
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "getFieldId", short.class);
            read.switchStatement("default", caseStatement(1, "message-field"), caseStatement(2, "type-field"));

            // case 1:
            read.visitLabel("message-field")
                    .loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readString", String.class)
                    .storeVariable("message")
                    .gotoLabel("while-begin");

            // case 2:
            read.visitLabel("type-field")
                    .loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readI32", int.class)
                    .storeVariable("type")
                    .gotoLabel("while-begin");

            // default:
            read.visitLabel("default")
                    .loadVariable("protocol").invokeVirtual(TProtocolReader.class, "skipFieldData", void.class)
                    .gotoLabel("while-begin");

            // end of while loop
            read.visitLabel("while-end");

            // protocol.readStructEnd();
            read.loadVariable("protocol").invokeVirtual(TProtocolReader.class, "readStructEnd", void.class);

            // BonkField bonkField = new BonkField();
            read.addLocalVariable("bonkField", structClass);
            read.newObject(structClass)
                    .dup()
                    .invokeConstructor(structClass)
                    .storeVariable("bonkField");

            read.loadVariable("bonkField")
                    .loadVariable("message")
                    .putField(structClass, "message", String.class);

            read.loadVariable("bonkField")
                    .loadVariable("type")
                    .putField(structClass, "type", int.class);

            read.loadVariable("bonkField")
                    .retObject();

            classDefinition.addMethod(read);
        }

        // public void write(BonkField struct, TProtocolWriter protocol) throws Exception
        {
            MethodDefinition write = new MethodDefinition(a(PUBLIC), "write", null, arg("struct", structClass), arg("protocol", TProtocolWriter.class));
            classDefinition.addMethod(write);

            write.loadVariable("protocol")
                    .loadConstant("bonk")
                    .invokeVirtual(TProtocolWriter.class, "writeStructBegin", void.class, String.class);

            write.loadVariable("protocol")
                    .loadConstant("message")
                    .loadConstant(1)
                    .loadVariable("struct")
                    .getField(structClass, "message", String.class)
                    .invokeVirtual(TProtocolWriter.class, "writeString", void.class, String.class, short.class, String.class);

            write.loadVariable("protocol")
                    .loadConstant("type")
                    .loadConstant(2)
                    .loadVariable("struct")
                    .getField(structClass, "type", int.class)
                    .invokeVirtual(TProtocolWriter.class, "writeI32", void.class, String.class, short.class, int.class);

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
                    .invokeVirtual(type(className), "read", type(structClass), type(TProtocolReader.class))
                    .retObject()
            );
        }

        // public synthetic bridge void write(Object struct, TProtocolWriter protocol) throws Exception
        {
            classDefinition.addMethod(new MethodDefinition(a(PUBLIC, BRIDGE, SYNTHETIC), "write", null, arg("struct", Object.class), arg("protocol", TProtocolWriter.class))
                    .addException(Exception.class)
                    .loadThis()
                    .loadVariable("struct", type(structClass))
                    .loadVariable("protocol")
                    .invokeVirtual(type(className), "write", type(void.class), type(structClass), type(TProtocolWriter.class))
                    .ret()
            );
        }

        ClassNode classNode = classDefinition.getClassNode();

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }
}
