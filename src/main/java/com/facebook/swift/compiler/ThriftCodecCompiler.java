/*
 * Copyright 2004-present Facebook. All Rights Reserved.
 */
package com.facebook.swift.compiler;

import com.facebook.swift.compiler.byteCode.CaseStatement;
import com.facebook.swift.compiler.byteCode.ClassDefinition;
import com.facebook.swift.compiler.byteCode.FieldDefinition;
import com.facebook.swift.compiler.byteCode.MethodDefinition;
import com.facebook.swift.compiler.byteCode.ParameterizedType;
import com.facebook.swift.metadata.ThriftFieldMetadata;
import com.facebook.swift.metadata.ThriftStructMetadata;
import com.facebook.swift.metadata.ThriftType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.facebook.swift.compiler.byteCode.Access.BRIDGE;
import static com.facebook.swift.compiler.byteCode.Access.FINAL;
import static com.facebook.swift.compiler.byteCode.Access.PUBLIC;
import static com.facebook.swift.compiler.byteCode.Access.STATIC;
import static com.facebook.swift.compiler.byteCode.Access.SUPER;
import static com.facebook.swift.compiler.byteCode.Access.SYNTHETIC;
import static com.facebook.swift.compiler.byteCode.Access.a;
import static com.facebook.swift.compiler.byteCode.CaseStatement.caseStatement;
import static com.facebook.swift.compiler.byteCode.NamedParameterDefinition.arg;
import static com.facebook.swift.compiler.byteCode.ParameterizedType.type;

public class ThriftCodecCompiler {
  private static final String PACKAGE = "$thrift";
  private static final boolean debug = true;
  private final CompiledThriftCodec compiledThriftCodec;
  private final DynamicClassLoader classLoader;

  public ThriftCodecCompiler(CompiledThriftCodec compiledThriftCodec, DynamicClassLoader classLoader) {
    this.compiledThriftCodec = compiledThriftCodec;
    this.classLoader = classLoader;
  }

  public <T> ThriftTypeCodec<T> generateThriftTypeCodec(Class<T> type) {
    ThriftStructMetadata<?> structMetadata =
        compiledThriftCodec.getCatalog().getThriftStructMetadata(type);

    Class<?> codecClass = generateClass(structMetadata);
    try {
      return (ThriftTypeCodec<T>) codecClass.getField("INSTANCE").get(null);
    } catch (Exception e) {
      throw new IllegalStateException("Generated class is invalid", e);
    }
  }

  private Class<?> generateClass(ThriftStructMetadata<?> metadata) {
    ParameterizedType structType = type(metadata.getStructClass());
    ParameterizedType codecType = toCodecType(metadata);

    ClassDefinition classDefinition = new ClassDefinition(
        a(PUBLIC, SUPER),
        codecType.getClassName(),
        type(Object.class),
        type(ThriftTypeCodec.class, structType)
    );

    // public static final StructCodec INSTANCE = new StructCodec();
    {
      FieldDefinition instanceField = new FieldDefinition(
          a(PUBLIC, STATIC, FINAL),
          "INSTANCE",
          codecType
      );
      classDefinition.addField(instanceField);

      classDefinition.addMethod(
          new MethodDefinition(a(STATIC), "<clinit>", type(void.class))
              .newObject(codecType)
              .dup()
              .invokeConstructor(codecType)
              .putStaticField(codecType, instanceField)
              .ret()
      );
    }

    // default constructor
    {
      classDefinition.addMethod(
          new MethodDefinition(a(PUBLIC), "<init>", type(void.class))
              .loadThis()
              .invokeConstructor(type(Object.class))
              .ret()
      );
    }

    // public Class<Struct> getType()
    {
      classDefinition.addMethod(
          new MethodDefinition(a(PUBLIC), "getType", type(Class.class, structType))
              .loadConstant(structType)
              .retObject()
      );
    }

    // public Struct read(TProtocolReader protocol) throws Exception
    {
      MethodDefinition read = new MethodDefinition(
          a(PUBLIC),
          "read",
          structType,
          arg("protocol", TProtocolReader.class)
      ).addException(Exception.class);

      // declare and init local variables here
      for (ThriftFieldMetadata field : metadata.getFields()) {
        read.addInitializedLocalVariable(
            toParameterizedType(field.getType()),
            "f_" + field.getName()
        );
      }

      // protocol.readStructBegin();
      read.loadVariable("protocol").invokeVirtual(
          TProtocolReader.class,
          "readStructBegin",
          void.class
      );

      // while (protocol.nextField())
      read.visitLabel("while-begin");
      read.loadVariable("protocol").invokeVirtual(
          TProtocolReader.class,
          "nextField",
          boolean.class
      );
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
          case BOOL:
            read.loadVariable("protocol").invokeVirtual(
                TProtocolReader.class,
                "readBool",
                boolean.class
            );
            break;
          case BYTE:
            read.loadVariable("protocol").invokeVirtual(
                TProtocolReader.class,
                "readByte",
                byte.class
            );
            break;
          case DOUBLE:
            read.loadVariable("protocol").invokeVirtual(
                TProtocolReader.class,
                "readDouble",
                double.class
            );
            break;
          case I16:
            read.loadVariable("protocol").invokeVirtual(
                TProtocolReader.class,
                "readI16",
                short.class
            );
            break;
          case I32:
            read.loadVariable("protocol").invokeVirtual(
                TProtocolReader.class,
                "readI32",
                int.class
            );
            break;
          case I64:
            read.loadVariable("protocol").invokeVirtual(
                TProtocolReader.class,
                "readI64",
                long.class
            );
            break;
          case STRING:
            read.loadVariable("protocol").invokeVirtual(
                TProtocolReader.class,
                "readString",
                String.class
            );
            break;
          case STRUCT:
            // push struct codec INSTANCE on stack
            ThriftTypeCodec<?> typeCodec = compiledThriftCodec.getTypeCodec(
                field.getType()
                    .getStructMetadata()
                    .getStructClass()
            );
            ParameterizedType fieldType = type(typeCodec.getType());
            ParameterizedType fieldCodecType = type(typeCodec.getClass());
            read.getStaticField(fieldCodecType, "INSTANCE", fieldCodecType);

            // push protocol on stack
            read.loadVariable("protocol");

            // invoke:  CodecClass.INSTANCE.read(protocol);
            read.invokeVirtual(fieldCodecType, "read", fieldType, type(TProtocolReader.class));
            break;
          default:
            throw new IllegalArgumentException(
                "Unsupported field type " + field.getType()
                    .getProtocolType()
            );
        }

        // store protocol value
        read.storeVariable("f_" + field.getName());

        // go back to top of loop
        read.gotoLabel("while-begin");
      }

      // default:
      read.visitLabel("default")
          .loadVariable("protocol").invokeVirtual(
          TProtocolReader.class,
          "skipFieldData",
          void.class
      )
          .gotoLabel("while-begin");

      // end of while loop
      read.visitLabel("while-end");

      // protocol.readStructEnd();
      read.loadVariable("protocol").invokeVirtual(
          TProtocolReader.class,
          "readStructEnd",
          void.class
      );

      // Struct instance = new Struct();
      read.addLocalVariable(structType, "instance");
      read.newObject(structType)
          .dup()
          .invokeConstructor(structType)
          .storeVariable("instance");

      // inject fields
      for (ThriftFieldMetadata field : metadata.getFields()) {
        read.loadVariable("instance")
            .loadVariable("f_" + field.getName())
            .putField(structType, field.getName(), toParameterizedType(field.getType()));
      }

      read.loadVariable("instance")
          .retObject();

      classDefinition.addMethod(read);
    }

    // public void write(Struct struct, TProtocolWriter protocol) throws Exception
    {
      MethodDefinition write = new MethodDefinition(
          a(PUBLIC), "write", null, arg(
          "struct",
          structType
      ), arg("protocol", TProtocolWriter.class)
      );
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

        write.getField(structType, field.getName(), toParameterizedType(field.getType()));
        switch (field.getType().getProtocolType()) {
          case BOOL:
            write.invokeVirtual(
                TProtocolWriter.class,
                "writeBool",
                void.class,
                String.class,
                short.class,
                boolean.class
            );
            break;
          case BYTE:
            write.invokeVirtual(
                TProtocolWriter.class,
                "writeByte",
                void.class,
                String.class,
                short.class,
                byte.class
            );
            break;
          case DOUBLE:
            write.invokeVirtual(
                TProtocolWriter.class,
                "writeDouble",
                void.class,
                String.class,
                short.class,
                double.class
            );
            break;
          case I16:
            write.invokeVirtual(
                TProtocolWriter.class,
                "writeI16",
                void.class,
                String.class,
                short.class,
                short.class
            );
            break;
          case I32:
            write.invokeVirtual(
                TProtocolWriter.class,
                "writeI32",
                void.class,
                String.class,
                short.class,
                int.class
            );
            break;
          case I64:
            write.invokeVirtual(
                TProtocolWriter.class,
                "writeI64",
                void.class,
                String.class,
                short.class,
                long.class
            );
            break;
          case STRING:
            write.invokeVirtual(
                TProtocolWriter.class,
                "writeString",
                void.class,
                String.class,
                short.class,
                String.class
            );
            break;
          case STRUCT:
            // push struct codec INSTANCE on stack
            ParameterizedType fieldCodec = toCodecType(field.getType().getStructMetadata());
            write.getStaticField(fieldCodec, "INSTANCE", fieldCodec);

            // swap the codec and value
            write.swap();

            write.invokeVirtual(
                TProtocolWriter.class,
                "writeStruct",
                void.class,
                String.class,
                short.class,
                ThriftTypeCodec.class,
                Object.class
            );
            break;
          default:
            throw new IllegalArgumentException(
                "Unsupported field type " + field.getType()
                    .getProtocolType()
            );
        }

      }

      write.loadVariable("protocol")
          .invokeVirtual(TProtocolWriter.class, "writeStructEnd", void.class);

      write.ret();
    }

    // public synthetic bridge Object read(TProtocolReader protocol) throws Exception
    {
      classDefinition.addMethod(
          new MethodDefinition(
              a(PUBLIC, BRIDGE, SYNTHETIC), "read", type(Object.class), arg(
              "protocol",
              TProtocolReader.class
          )
          )
              .addException(Exception.class)
              .loadThis()
              .loadVariable("protocol")
              .invokeVirtual(codecType, "read", structType, type(TProtocolReader.class))
              .retObject()
      );
    }

    // public synthetic bridge void write(Object struct, TProtocolWriter protocol) throws Exception
    {
      classDefinition.addMethod(
          new MethodDefinition(
              a(PUBLIC, BRIDGE, SYNTHETIC), "write", null, arg(
              "struct",
              Object.class
          ), arg("protocol", TProtocolWriter.class)
          )
              .addException(Exception.class)
              .loadThis()
              .loadVariable("struct", structType)
              .loadVariable("protocol")
              .invokeVirtual(
                  codecType,
                  "write",
                  type(void.class),
                  structType,
                  type(TProtocolWriter.class)
              )
              .ret()
      );
    }

    ClassNode classNode = classDefinition.getClassNode();

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classNode.accept(cw);
    byte[] byteCode = cw.toByteArray();

    if (debug) {
      ClassReader reader = new ClassReader(byteCode);
      CheckClassAdapter.verify(reader, classLoader, true, new PrintWriter(System.out));
    }
    Class<?> codecClass = classLoader.defineClass(
        codecType.getClassName().replace('/', '.'),
        byteCode
    );

    return codecClass;
  }

  private ParameterizedType toCodecType(ThriftStructMetadata<?> metadata) {
    return type(PACKAGE + "/" + type(metadata.getStructClass()).getClassName() + "Codec");
  }

  public static ParameterizedType toParameterizedType(ThriftType type) {
    switch (type.getProtocolType()) {
      case BOOL:
        return type(boolean.class);
      case BYTE:
        return type(byte.class);
      case DOUBLE:
        return type(double.class);
      case I16:
        return type(short.class);
      case I32:
        return type(int.class);
      case I64:
        return type(long.class);
      case STRING:
        return type(String.class);
      case STRUCT:
        return type(type.getStructMetadata().getStructClass());
      case MAP:
        return type(
            Map.class,
            toParameterizedType(type.getKeyType()),
            toParameterizedType(type.getValueType())
        );
      case SET:
        return type(Set.class, toParameterizedType(type.getValueType()));
      case LIST:
        return type(List.class, toParameterizedType(type.getValueType()));
      case ENUM:
        throw new UnsupportedOperationException("Enums are currently not supported");
      default:
        throw new IllegalArgumentException("Unsupported thrift field type " + type);
    }
  }

}
