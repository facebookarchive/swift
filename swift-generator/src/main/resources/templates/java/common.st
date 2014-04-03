service(context, tweaks) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = service definition                                                             = !>
<! =                                                                                = !>
<! = called from the generator to render a new service java source file.            = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;
import com.facebook.swift.codec.ThriftField.Requiredness;
import com.facebook.swift.service.*;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.util.*;

@ThriftService("<context.name>")
public interface <context.javaName><if(context.javaParents)> extends <context.javaParents; separator=", "><endif>
{
    @ThriftService("<context.name>")
    public interface Async<if(context.javaAsyncParents)> extends <context.javaAsyncParents; separator=", "><endif>
    {
        <context.methods : { method |<_asyncMethod(method)>}; separator="\n">
    }
    <context.methods : { method |<_method(method)>}; separator="\n">
}
>>

struct(context, tweaks) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = struct definition                                                              = !>
<! =                                                                                = !>
<! = called from the generator to render a new struct java source file.             = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;
import com.facebook.swift.codec.ThriftField.Requiredness;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("<context.name>")
public final class <context.javaName>
{
    <_structbody(context)>

    <_toString(context)>

    <_equalsImpl(context)>

    <_hashCodeImpl(context)>
}<\n>
>>

union(context, tweaks) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = union definition                                                               = !>
<! =                                                                                = !>
<! = called from the generator to render a new union java source file.              = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;
import com.facebook.swift.codec.ThriftField.Requiredness;
import java.util.*;

import static com.google.common.base.Objects.toStringHelper;

@ThriftUnion("<context.name>")
public final class <context.javaName>
{
    <_union_body(context)>

    <context.fields : { field |<_union_field(field)>}; separator="\n\n">

    @ThriftUnionId
    public int getThriftId()
    {
        return this.id;
    }

    public String getThriftName()
    {
        return this.name;
    }

    <_union_toString(context)>
}<\n>
>>

exception(context, tweaks) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = exception definition                                                           = !>
<! =                                                                                = !>
<! = called from the generator to render a new exception java source file.          = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;
import com.facebook.swift.codec.ThriftField.Requiredness;
import java.util.*;

@ThriftStruct("<context.name>")
public final class <context.javaName> extends <if(tweaks.EXTEND_RUNTIME_EXCEPTION)>RuntimeException<else>Exception<endif>
{
    private static final long serialVersionUID = 1L;

    <_structbody(context)>
}<\n>
>>

constants(context, tweaks) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = constants definition                                                           = !>
<! =                                                                                = !>
<! = called from the generator to render a new constants java source file.          = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;
import com.google.common.collect.*;
import java.util.*;

public final class <context.javaName>
{
    private <context.javaName>() {
    }

    <context.constants : { constant|<_constantElement(constant)>}; separator="\n\n">
}<\n>
>>



stringEnum(context, tweaks) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = string based enum definition                                                   = !>
<! =                                                                                = !>
<! = called from the generator to render a new enum java source file.               = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;

public enum <context.javaName>
{
    <context.fields : { field|<_enumElement(field)>}; separator=", ">;
}<\n>
>>

intEnum(context, tweaks) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = int based enum definition                                                      = !>
<! =                                                                                = !>
<! = called from the generator to render a new enum java source file.               = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;

public enum <context.javaName>
{
    <context.fields : { field|<_enumElement(field)>}; separator=", ">;

    private final int value;

    <context.javaName>(int value)
    {
        this.value = value;
    }

    @ThriftEnumValue
    public int getValue()
    {
        return value;
    }
}<\n>
>>

_enumElement(field) ::= <<
<field.javaName><if(field.value)>(<field.value>)<endif>
>>

_method(method) ::= <<
<if(method.name)>
@ThriftMethod(value = "<method.name>"<if(method.oneway)>,
              oneway = true<endif><if(method.annotatedExceptions)>,
              exception = {
                  <_annotatedExceptions(method)>
              }<endif>)
<endif>
<method.javaType> <method.javaName><_params(method.parameters)><if(!method.exceptions)>;
<else> throws <method.exceptions: {exception |<exception.type>}; separator=", ">;<endif>
>>

_asyncMethod(method) ::= <<
<if(method.name)>
@ThriftMethod(value = "<method.name>"<if(method.oneway)>,
              oneway = true<endif><if(method.annotatedExceptions)>,
              exception = {
                  <_annotatedExceptions(method)>
              }<endif>)
<endif>
<if(method.allowAsync)>ListenableFuture\<<method.boxedJavaType>\><else><method.javaType><endif> <method.javaName><_params(method.parameters)>;
>>

_annotatedExceptions(method) ::= <<
<method.annotatedExceptions: {exception |<_exceptionElement(exception)>}; separator=",\n">
>>

_fieldAnnotation(field) ::= <<
@ThriftField(value=<field.id>, name="<field.name>"<if(field.requiredness)>, requiredness=Requiredness.<field.requiredness><endif>)
>>

_params(parameters) ::= <<
<if(parameters)>(
    <parameters: {param |<_param(param)>}; separator=",\n">
)<else>()<endif>
>>

_param(param) ::= <<
<_fieldAnnotation(param)> final <param.javaType> <param.javaName>
>>

_exceptionElement(exception) ::= <<
@ThriftException(type=<exception.type>.class, id=<exception.id>)
>>

_constantElement(constant) ::= <<
public static final <constant.javaType> <constant.javaName> = <constant.javaValue>;
>>

_toString(context) ::= <<
@Override
public String toString()
{
    return toStringHelper(this)
        <context.fields : { field |<_toStringField(field)>}; separator="\n">
        .toString();
}
>>

_toStringField(field) ::= <<
.add("<field.javaName>", <field.javaName>)
>>

_hashCodeImpl(context) ::= <<
@Override
public int hashCode() {
    return Arrays.deepHashCode(new Object[] {
        <context.fields : { field |<field.javaName>}; separator = ",\n">
    });
}
>>

_equalsImpl(context) ::= <<
@Override
public boolean equals(Object o) {
    if (this == o) {
        return true;
    }
    if (o == null || getClass() != o.getClass()) {
        return false;
    }

    <context.javaName> other = (<context.javaName>)o;

    return
        <context.fields : { field |<_checkFieldEquality(field)>}; separator=" &&\n">;
}
>>

_checkFieldEquality(field) ::= <<
<if(field.arrayType)><_checkArrayFieldEquality(field)><else><_checkObjectFieldEquality(field)><endif>
>>

_checkArrayFieldEquality(field) ::= <<
Arrays.equals(<field.javaName>, other.<field.javaName>)
>>

_checkObjectFieldEquality(field) ::= <<
Objects.equals(<field.javaName>, other.<field.javaName>)
>>

_union_field(field) ::= <<
<_fieldAnnotation(field)>
public <field.javaType> <field.javaGetterName>() {
    if (this.id != <field.id>) {
        throw new IllegalStateException("Not a <field.name> element!");
    }
    return (<field.javaType>) value;
}

public boolean <field.javaTestPresenceName>() {
    return this.id == <field.id>;
}
>>

_union_toString(context) ::= <<
@Override
public String toString()
{
    return toStringHelper(this)
        .add("value", value)
        .add("id", id)
        .add("name", name)
        .add("type", value == null ? "\<null\>" : value.getClass().getSimpleName())
        .toString();
}
>>
