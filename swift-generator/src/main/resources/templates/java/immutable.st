group java;

service(context) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = service definition                                                             = !>
<! =                                                                                = !>
<! = called from the generator to render a new service java source file.            = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;
import com.facebook.swift.service.*;
import java.io.*;
import java.util.*;

@ThriftService("<context.name>")
public interface <context.javaName> extends <if(context.javaParent)><context.javaParent>, <endif>Closeable
{
    // Swift clients do not throw any exceptions during close.
    void close();

    <context.methods : { method |<_method(method)>}; separator="\n\n">
}
>>

struct(context) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = struct definition                                                              = !>
<! =                                                                                = !>
<! = called from the generator to render a new struct java source file.             = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;
import java.util.*;

@ThriftStruct("<context.name>")
public class <context.javaName>
{
    <_constructor(context)>

    <context.fields : { field |<_field(field)>}; separator="\n\n">
}
>>

exception(context) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = exception definition                                                           = !>
<! =                                                                                = !>
<! = called from the generator to render a new exception java source file.          = !>
<! =                                                                                = !>
<! ================================================================================== !>
package <context.javaPackage>;

import com.facebook.swift.codec.*;
import java.util.*;

public class <context.javaName> extends Exception
{
    private static final long serialVersionUID = 1L;

    <_constructor(context)>

    <context.fields : { field |<_field(field)>}; separator="\n\n">
}
>>

stringEnum(context) ::= <<
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
}
>>

intEnum(context) ::= <<
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
}
>>

_enumElement(field) ::= <<
  <field.javaName><if(field.value)>(<field.value>)<endif>
>>

_method(method) ::= <<
@ThriftMethod(value="<method.name>"<if(method.oneway)>,
              oneway=true<endif><if(method.annotatedExceptions)>,
              exception={<method.annotatedExceptions: {exception |<_exceptionElement(exception)>}; separator=", ">}<endif>)
<method.javaType> <method.javaName>(<method.parameters: {param |<_param(param)>}; separator=",\n">)<if(!method.exceptions)>;
<else><\n>    throws <method.exceptions: {exception |<exception.type>}; separator=", ">;<endif>
>>

_annotation(field) ::= <<
@ThriftField(value=<field.id>, name="<field.name>")
>>

_param(param) ::= <<
<_annotation(param)> final <param.javaType> <param.javaName>
>>

_exceptionElement(exception) ::= <<
@ThriftException(type=<exception.type>.class, id=<exception.id>)
>>

_field(field) ::= <<
private final <field.javaType> <field.javaName>;

<_annotation(field)>
public <field.javaType> <field.javaGetterName>() { return <field.javaName>; }
>>

_constructor(context) ::= <<
@ThriftConstructor
public <context.javaName>(<context.fields: {field|<_param(field)>}; separator=",\n">) {
  <context.fields: {field|<_ctorAssignment(field)>}; separator="\n">
}
>>

_ctorAssignment(field) ::= <<
this.<field.javaName> = <field.javaName>;
>>



