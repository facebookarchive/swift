group java;

_structbody(context) ::= <<
    <_constructor(context)>

    <context.fields : { field |<_field(field)>}; separator="\n\n">
>>

_field(field) ::= <<
private final <field.javaType> <field.javaName>;

<_annotation(field)>
public <field.javaType> <field.javaGetterName>() { return <field.javaName>; }
>>

_constructor(element) ::= <<
@ThriftConstructor
public <element.javaName>(<element.fields: {field|<_param(field)>}; separator=",\n">) {
  <element.fields: {field|<_ctorAssignment(field)>}; separator="\n">
}
>>

_ctorAssignment(field) ::= <<
this.<field.javaName> = <field.javaName>;
>>
