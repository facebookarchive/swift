group java;

service(service) ::= <<
package <service.namespace>;

import com.facebook.swift.codec.*;
import com.facebook.swift.service.*;
import java.util.*;

@ThriftService("<service.name>")
public interface <service.javaName> <if(service.parent)>extends <service.javaParent><endif>
{
    <service.methods : { method |<method(method)>}; separator="\n\n">
}
>>

struct(struct) ::= <<
package <struct.namespace>;

import com.facebook.swift.codec.*;
import java.util.*;

@ThriftStruct("<struct.name>")
public class <struct.javaName>
{
    <struct.fields : { field |<field(field)>}; separator="\n\n">
}
>>

exception(exception) ::= <<
package <exception.namespace>;

import com.facebook.swift.codec.*;
import java.util.*;

public class <exception.javaName> extends Exception
{
    <exception.fields : { field |<field(field)>}; separator="\n\n">
}
>>

method(method) ::= <<
@ThriftMethod(value="<method.name>"<if(method.oneway)>,
              oneway=true<endif><if(method.exceptions)>,
              exception={<method.exceptions: {exception |<exception(exception)>}; separator=", ">}<endif>)
<method.javaType> <method.javaName>(<method.parameters: {param |<param(param)>}; separator=",\n">)<if(!method.exceptions)>;
<else><\n>    throws <method.exceptions: {exception |<exception.type>}; separator=", ">;<endif>
>>


param(param) ::= <<
@ThriftField(value=<param.id>, name="<param.name>") <param.javaType> <param.javaName>
>>

exception(exception) ::= <<
@ThriftException(type=<exception.type>.class, id=<exception.id>)
>>

field(field) ::= <<
private <field.javaType> <field.javaName>;

@ThriftField(<field.id>)
public <field.javaType> <field.javaGetterName>() { return <field.javaName>; }

@ThriftField
public void <field.javaSetterName>(final <field.javaType> <field.javaName>) { this.<field.javaName> = <field.javaName>; }
>>

