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
public <element.javaName><_params(element.fields)> {
    <element.fields: {field|<_ctorAssignment(field)>}; separator="\n">
}
>>

_ctorAssignment(field) ::= <<
this.<field.javaName> = <field.javaName>;
>>

_union_ctor(context, field) ::= <<
@ThriftConstructor
public <context.javaName>(final <field.javaType> <field.javaName>) {
    this.value = <field.javaName>;
    this.id = <field.id>;
    this.name = "<field.name>";
}
>>

_union_body(context) ::= <<
private final Object value;
private final int id;
private final String name;

<context.fields : { field |<_union_ctor(context, field)>}; separator="\n\n">
>>
