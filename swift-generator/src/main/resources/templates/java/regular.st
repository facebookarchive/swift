group java;

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
import com.facebook.swift.service.*;
import java.io.*;
import java.util.*;

@ThriftService("<context.name>")
public interface <context.javaName><if(context.javaParents)> extends <context.javaParents; separator=", "><endif>
{
    <context.methods : { method |<_method(method)>}; separator="\n\n">
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
import java.util.*;

@ThriftStruct("<context.name>")
public class <context.javaName>
{
    <context.fields : { field |<_field(field)>}; separator="\n\n">
}
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
import java.util.*;

public class <context.javaName> extends <if(tweaks.EXTEND_RUNTIME_EXCEPTION)>RuntimeException<else>Exception<endif>
{
    private static final long serialVersionUID = 1L;

    <context.fields : { field |<_field(field)>}; separator="\n\n">
}
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
}
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
}
>>

_enumElement(field) ::= <<
  <field.javaName><if(field.value)>(<field.value>)<endif>
>>

_method(method) ::= <<
<if(method.name)>
@ThriftMethod(value="<method.name>"<if(method.oneway)>,
              oneway=true<endif><if(method.annotatedExceptions)>,
              exception={<method.annotatedExceptions: {exception |<_exceptionElement(exception)>}; separator=", ">}<endif>)
<endif>
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
private <field.javaType> <field.javaName>;

<_annotation(field)>
public <field.javaType> <field.javaGetterName>() { return <field.javaName>; }

@ThriftField
public void <field.javaSetterName>(final <field.javaType> <field.javaName>) { this.<field.javaName> = <field.javaName>; }
>>

