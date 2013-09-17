_structbody(context) ::= <<
<_constructor(context)>

<context.fields : { field |<_field(field)>}; separator="\n\n">
>>

_field(field) ::= <<
private <field.javaType> <field.javaName>;

<_annotation(field)>
public <field.javaType> <field.javaGetterName>() { return <field.javaName>; }

public void <field.javaSetterName>(final <field.javaType> <field.javaName>) { this.<field.javaName> = <field.javaName>; }
>>

_constructor(element) ::= <<
@ThriftConstructor
public <element.javaName><_params(element.fields)> {
    <element.fields: {field|<_ctorAssignment(field)>}; separator="\n">
}

public <element.javaName>() {
}
>>

_ctorAssignment(field) ::= <<
this.<field.javaName> = <field.javaName>;
>>
