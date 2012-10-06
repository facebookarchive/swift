thriftservice(package, service) ::= <<
package <package>;

import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;

@ThriftService(value = "<service.name>")

public interface <service.name> {
    <thriftmethodlist(service.methods)>
}
<\n>
>>

thriftmethodlist(methods) ::= "<methods:thriftmethod(); separator=\"\n\n\">"

thriftmethod(method) ::= <<
@ThriftMethod(value = "<method.name>", oneway = <method.oneway>, exception = {<thriftexceptionlist(method.throwsFields)>})
public void <method.name>(
    <thriftmethodarglist(method.arguments)>
) throws TException;
>>

thriftexceptionlist(exceptions) ::= "<exceptions:thriftexception(); separator=\", \">"

thriftexception(exception) ::= "@ThriftException(type = <thrifttype(exception.type)>.class, id = <exception.identifier>)"

thriftmethodarglist(args) ::= "<args:{arg | <thriftmethodarg(arg, i)>}; separator=\",\n\">"

thriftmethodarg(arg, i) ::= "@ThriftField(value = <i>) <thrifttype(arg.type)> <arg.name>"

thrifttype(t) ::= "<t>"

thriftenumeration(package, enum) ::= <<
package <package>;

import com.facebook.swift.codec.ThriftEnumValue;

public enum <enum.name> {
    <thriftenumerationvaluelist(enum.fields)>;

    private final int value;

    private <enum.name>(int value) {
        this.value = value;
    }

    @ThriftEnumValue
    public long getValue() {
        return value;
    }
}
<\n>
>>

thriftenumerationvaluelist(fields) ::= "<fields:thriftenumerationvalue(); separator=\",\n\">"

thriftenumerationvalue(field) ::= "<field.name><if(field.value)>(<field.value>)<endif>"

thriftstructdefinition(package, struct) ::= <<
package <package>;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import java.util.Set;
import java.util.Map;

@ThriftStruct
public class <struct.name> {
    <thriftstructfieldlist(struct.fields)>
}
<\n>
>>

thriftexceptiondefinition(package, exception) ::= <<
package <package>;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import java.util.Set;
import java.util.Map;

@ThriftStruct
public class <exception.name> extends Exception {
    <thriftstructfieldlist(exception.fields)>
}
<\n>
>>

thriftstructfieldlist(fields) ::= "<fields:thriftstructfield(); separator=\"\n\">"

thriftstructfield(field) ::= "@ThriftField(value = <field.identifier>, required = <field.required>) public <thrifttype(field.type)> <field.name>;"
