package com.facebook.swift.swiftify;

import com.facebook.swift.parser.model.BaseType;
import com.facebook.swift.parser.model.Const;
import com.facebook.swift.parser.model.ConstDouble;
import com.facebook.swift.parser.model.ConstIdentifier;
import com.facebook.swift.parser.model.ConstInteger;
import com.facebook.swift.parser.model.ConstList;
import com.facebook.swift.parser.model.ConstMap;
import com.facebook.swift.parser.model.ConstString;
import com.facebook.swift.parser.model.Document;
import com.facebook.swift.parser.model.Header;
import com.facebook.swift.parser.model.IdentifierType;
import com.facebook.swift.parser.model.IntegerEnum;
import com.facebook.swift.parser.model.IntegerEnumField;
import com.facebook.swift.parser.model.ListType;
import com.facebook.swift.parser.model.MapType;
import com.facebook.swift.parser.model.Namespace;
import com.facebook.swift.parser.model.Service;
import com.facebook.swift.parser.model.SetType;
import com.facebook.swift.parser.model.StringEnum;
import com.facebook.swift.parser.model.Struct;
import com.facebook.swift.parser.model.ThriftException;
import com.facebook.swift.parser.model.ThriftField;
import com.facebook.swift.parser.model.ThriftMethod;
import com.facebook.swift.parser.model.TypeAnnotation;
import com.facebook.swift.parser.model.Typedef;
import com.facebook.swift.parser.model.Union;
import com.facebook.swift.parser.model.VoidType;

import java.io.IOException;

/**
 * Abstract node visitor
 */
public abstract class AbstractIdlNodeVisitor
{
    public AbstractIdlNodeVisitor accept(Object node)
            throws IOException
    {
        if (node instanceof BaseType) {
            visitBaseType((BaseType)node);
        } else if (node instanceof Const) {
            visitConst((Const)node);
        } else if (node instanceof ConstDouble) {
            visitConstDouble((ConstDouble)node);
        } else if (node instanceof ConstIdentifier) {
            visitConstIdentifier((ConstIdentifier)node);
        } else if (node instanceof ConstInteger) {
            visitConstInteger((ConstInteger)node);
        } else if (node instanceof ConstList) {
            visitConstList((ConstList)node);
        } else if (node instanceof ConstMap) {
            visitConstMap((ConstMap)node);
        } else if (node instanceof ConstString) {
            visitConstString((ConstString)node);
        } else if (node instanceof Document) {
            visitDocument((Document)node);
        } else if (node instanceof Header) {
            visitHeader((Header)node);
        } else if (node instanceof IdentifierType) {
            visitIdentifierType((IdentifierType)node);
        } else if (node instanceof IntegerEnum) {
            visitIntegerEnum((IntegerEnum) node);
        } else if (node instanceof IntegerEnumField) {
            visitIntegerEnumField((IntegerEnumField) node);
        } else if (node instanceof ListType) {
            visitListType((ListType) node);
        } else if (node instanceof MapType) {
            visitMapType((MapType) node);
        } else if (node instanceof Namespace) {
            visitNamespace((Namespace) node);
        } else if (node instanceof Service) {
            visitService((Service) node);
        } else if (node instanceof SetType) {
            visitSetType((SetType) node);
        } else if (node instanceof StringEnum) {
            visitStringEnum((StringEnum) node);
        } else if (node instanceof Struct) {
            visitStruct((Struct) node);
        } else if (node instanceof ThriftException) {
            visitThriftException((ThriftException) node);
        } else if (node instanceof ThriftField) {
            visitThriftField((ThriftField) node);
        } else if (node instanceof ThriftMethod) {
            visitThriftMethod((ThriftMethod) node);
        } else if (node instanceof TypeAnnotation) {
            visitTypeAnnotation((TypeAnnotation) node);
        } else if (node instanceof Typedef) {
            visitTypedef((Typedef) node);
        } else if (node instanceof Union) {
            visitUnion((Union) node);
        } else if (node instanceof VoidType) {
            visitVoidType((VoidType) node);
        } else {
            throw new IllegalArgumentException("Unknown thrift IDL node type");
        }

        return this;
    }

    protected void visitBaseType(BaseType node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitConst(Const node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitConstDouble(ConstDouble node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitConstIdentifier(ConstIdentifier node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitConstInteger(ConstInteger node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitConstList(ConstList node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitConstMap(ConstMap node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitConstString(ConstString node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitDocument(Document node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitHeader(Header node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitIdentifierType(IdentifierType node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitIntegerEnum(IntegerEnum node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitIntegerEnumField(IntegerEnumField node)
            throws IOException
    {

    }

    protected void visitListType(ListType node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitMapType(MapType node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitNamespace(Namespace node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitService(Service node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitSetType(SetType node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitStringEnum(StringEnum node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitStruct(Struct node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitThriftException(ThriftException node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitThriftField(ThriftField node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitThriftMethod(ThriftMethod node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitTypeAnnotation(TypeAnnotation node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitTypedef(Typedef node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitUnion(Union node)
    {
        throw new IllegalStateException("Unimplemented node type");
    }

    protected void visitVoidType(VoidType node)
            throws IOException
    {
        throw new IllegalStateException("Unimplemented node type");
    }
}
