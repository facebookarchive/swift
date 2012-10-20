group java;

interface(namespace, name, parent) ::= <<
package <namespace>;

import com.facebook.swift.service.*;

@ThriftService
public interface <name> <if(parent)>extends <parent><endif>
{
}
>>
