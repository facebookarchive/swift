thriftfile(context) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = top-level thrift file definition                                               = !>
<! =                                                                                = !>
<! = called from the generator to render a new thrift source file.                  = !>
<! =                                                                                = !>
<! ================================================================================== !>
namespace java.swift <context.namespace>

<context.includes : {inc | include "<inc>"}; separator="\n">

<context.enums : {e | <_enum(e)>}; separator="\n">

<context.structs : {s | <_struct(s)>}; separator="\n">

<context.services : {s | <_service(s)>}; separator="\n">
>>

_enum(enum) ::= <<
enum <enum.enumName> {
<if(enum.explicitThriftValue)><_enumWithValues(enum)><else><_enumWithoutValues(enum)><endif>
}
>>

_enumWithValues(enum) ::= <<
  <enum.byEnumConstant : {enumElem | <enumElem>=<enum.byEnumConstant.(enumElem)>}; separator=", ">
>>

_enumWithoutValues(enum) ::= <<
  <enum.enumClass.enumConstants; separator=", ">
>>

_struct(struct) ::= <<
<if(struct.exception)>exception<else>struct<endif> <struct.structName> {
  <struct.unsortedFields : {f | <_field(f)>;}; separator="\n">
}
>>

_field(field) ::= <<
<field.id>: <field.type> <field.name>
>>

_service(service) ::= <<
service <service> <if(service.parentService)>extends <service.parentService> <endif>{
  <service.declaredMethods : {method | <_method(service.declaredMethods.(method))>}; separator="\n">
}
>>

_method(method) ::= <<
<if(method.oneway)>oneway <endif><\\\>
<method.returnType> <method.name><\\\>
(<_methodParameters(method.parameters)>)<\\\>
<_methodExceptions(method.exceptions)><\\\>
;
>>

_methodParameters(params) ::= <<
<params : {p | <_field(p)>}; separator=", ">
>>

_methodExceptions(exceptions) ::= <<
<if(exceptions)> throws (<exceptions : {e | <e>: <exceptions.(e)> ex<e>}; separator=", ">)<endif>
>>
