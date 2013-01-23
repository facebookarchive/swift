group java;

_structbody(context) ::= <<
<context.fields : { field |<_field(field)>}; separator="\n\n">
>>

_field(field) ::= <<
private <field.javaType> <field.javaName>;

<_annotation(field)>
public <field.javaType> <field.javaGetterName>() { return <field.javaName>; }

@ThriftField
public void <field.javaSetterName>(final <field.javaType> <field.javaName>) { this.<field.javaName> = <field.javaName>; }
>>
