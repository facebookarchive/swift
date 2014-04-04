_structbody(context) ::= <<
<_constructor(context)>

<_builder(context)>

<context.fields : { field |<_field(field)>}; separator="\n\n">
>>

_field(field) ::= <<
private final <field.javaType> <field.javaName>;

<_fieldAnnotation(field)>
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

_builder(element) ::= <<
public static class Builder {
    <element.fields: {field|<_builder_field(field)>}; separator="\n">

    public Builder() { }
    public Builder(<element.javaName> other) {
        <element.fields: {field|<_builderInitAssignment(field)>}; separator="\n">
    }

    public <element.javaName> build() {
        return new <element.javaName> (
            <element.fields: {field|this.<field.javaName>}; separator=",\n">
        );
    }
}
>>

_builderInitAssignment(field) ::= <<
this.<field.javaName> = other.<field.javaName>;
>>

_builder_field(field) ::= <<
private <field.javaType> <field.javaName>;

public Builder <field.javaSetterName>(<field.javaType> <field.javaName>) {
    this.<field.javaName> = <field.javaName>;
    return this;
}
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
