package com.facebook.miffed.compiler;

import com.google.common.base.Throwables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

import static me.qmx.jitescript.util.CodegenUtils.ci;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;

public class BonkFieldThriftTypeCodecDump implements Opcodes
{
    private static final String PACKAGE = "$thrift";
    private static final AtomicLong counter = new AtomicLong();
    private static final boolean debug = true;
    private final DynamicClassLoader classLoader;

    public BonkFieldThriftTypeCodecDump(DynamicClassLoader classLoader)
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
        String classIdentifier = "L" + className + ";";

        ClassWriter cw = new ClassWriter(COMPUTE_FRAMES);
        FieldVisitor fv;
        MethodVisitor mv;

        // public class BonkFieldThriftTypeCodec implements ThriftTypeCodec<BonkField>
        cw.visit(V1_7,
                ACC_PUBLIC + ACC_SUPER,
                className,
                ci(Object.class) + "L" + p(ThriftTypeCodec.class) + "<" + ci(structClass) + ">;",
                p(Object.class),
                new String[]{p(ThriftTypeCodec.class)});

        // public static final BonkFieldThriftTypeCodec INSTANCE = new BonkFieldThriftTypeCodec();
        {
            fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "INSTANCE", classIdentifier, null, null);
            fv.visitEnd();

            // static { INSTANCE = new BonkFieldThriftTypeCodec(); }
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", sig(void.class), null, null);
            mv.visitCode();

            // new BonkFieldThriftTypeCodec()
            mv.visitTypeInsn(NEW, className);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", sig(void.class));

            // INSTANCE =...
            mv.visitFieldInsn(PUTSTATIC, className, "INSTANCE", classIdentifier);

            // return (implicit)
            mv.visitInsn(RETURN);

            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // default constructor
        {
            mv = cw.visitMethod(ACC_PUBLIC,
                    "<init>",
                    sig(void.class),
                    null,
                    null);

            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, p(Object.class), "<init>", sig(void.class));
            mv.visitInsn(RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // public Class<BonkField> getType()
        {
            mv = cw.visitMethod(ACC_PUBLIC,
                    "getType",
                    sig(Class.class),
                    "()L" + p(Class.class) + "<" + ci(structClass) + ">;",
                    null);
            mv.visitCode();
            mv.visitLdcInsn(Type.getType(ci(structClass)));
            mv.visitInsn(ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // public BonkField read(TProtocolReader protocol) throws Exception
        {
            mv = cw.visitMethod(ACC_PUBLIC,
                    "read",
                    sig(structClass, TProtocolReader.class),
                    null,
                    new String[]{p(Exception.class)});
            mv.visitCode();

            // String message = null;
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, 2);
            // int type = 0;
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 3);

            // protocol.readStructBegin();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolReader.class), "readStructBegin", sig(void.class));

            // implicit label before loop
            Label whileLoopBegin = new Label();
            mv.visitLabel(whileLoopBegin);

            // while (protocol.nextField())
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolReader.class), "nextField", sig(boolean.class));
            Label whileLoopEnd = new Label();
            mv.visitJumpInsn(IFEQ, whileLoopEnd);

            // switch labels...
            Label case1 = new Label();
            Label case2 = new Label();
            Label defaultCase = new Label();

            // switch (protocol.getFieldId())
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolReader.class), "getFieldId", sig(short.class));
            mv.visitLookupSwitchInsn(defaultCase, new int[]{1, 2}, new Label[]{case1, case2});

            // case 1:
            mv.visitLabel(case1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolReader.class), "readString", sig(String.class));
            mv.visitVarInsn(ASTORE, 2);
            mv.visitJumpInsn(GOTO, whileLoopBegin);

            // case 2:
            mv.visitLabel(case2);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolReader.class), "readI32", sig(int.class));
            mv.visitVarInsn(ISTORE, 3);
            mv.visitJumpInsn(GOTO, whileLoopBegin);

            // default:
            mv.visitLabel(defaultCase);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolReader.class), "skipFieldData", sig(void.class));

            // goto back to top of loop
            mv.visitJumpInsn(GOTO, whileLoopBegin);

            // done with while loop
            mv.visitLabel(whileLoopEnd);

            // protocol.readStructEnd();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolReader.class), "readStructEnd", sig(void.class));

            // BonkField bonkField = new BonkField();
            mv.visitTypeInsn(NEW, p(structClass));
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, p(structClass), "<init>", sig(void.class));
            mv.visitVarInsn(ASTORE, 4);

            // bonkField.message = message;
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(PUTFIELD, p(structClass), "message", ci(String.class));

            // bonkField.type = type;
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitFieldInsn(PUTFIELD, p(structClass), "type", ci(int.class));

            // return bonkField;
            mv.visitVarInsn(ALOAD, 4);
            mv.visitInsn(ARETURN);

            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // public void write(BonkField value, TProtocolWriter protocol) throws Exception
        {
            mv = cw.visitMethod(ACC_PUBLIC,
                    "write",
                    sig(void.class, structClass, TProtocolWriter.class),
                    null,
                    new String[]{p(Exception.class)});
            mv.visitCode();

            // protocol.writeStructBegin("bonk");
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn("bonk");
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolWriter.class), "writeStructBegin", sig(void.class, String.class));

            // protocol.writeString("message", (short) 1, value.message);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn("message");
            mv.visitInsn(ICONST_1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, p(structClass), "message", ci(String.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolWriter.class), "writeString", sig(void.class, String.class, short.class, String.class));

            // protocol.writeI32("type", (short) 2, value.type);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn("type");
            mv.visitInsn(ICONST_2);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, p(structClass), "type", ci(int.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolWriter.class), "writeI32", sig(void.class, String.class, short.class, int.class));

            // protocol.writeStructEnd();
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, p(TProtocolWriter.class), "writeStructEnd", sig(void.class));

            // return (implicit)
            mv.visitInsn(RETURN);

            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // public synthetic bridge Object read(TProtocolReader protocol) throws Exception
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC,
                    "read",
                    sig(Object.class, TProtocolReader.class),
                    null,
                    new String[]{p(Exception.class)});

            // return this.read(protocol);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    className,
                    "read",
                    sig(structClass, TProtocolReader.class));
            mv.visitInsn(ARETURN);

            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // public synthetic bridge void write(Object value, TProtocolWriter protocol) throws Exception
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC,
                    "write",
                    sig(void.class, Object.class, TProtocolWriter.class),
                    null,
                    new String[]{p(Exception.class)});
            mv.visitCode();

            // this.write((BonkField) value, protocol);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, p(structClass));
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    className,
                    "write",
                    sig(void.class, structClass, TProtocolWriter.class));

            // return;
            mv.visitInsn(RETURN);

            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }
}
