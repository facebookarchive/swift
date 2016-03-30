thriftfile(context) ::= <<
<! ================================================================================== !>
<! =                                                                                = !>
<! = top-level thrift file definition                                               = !>
<! =                                                                                = !>
<! = called from the generator to render a new thrift source file.                  = !>
<! =                                                                                = !>
<! ================================================================================== !>
namespace java.swift <context.namespace>
<context.customNamespaces : {key | namespace <key> <context.customNamespaces.(key)>}; separator="\n">

<context.includes : {inc | include "<inc>"}; separator="\n">

<context.enums : {e | <_enum(e)>}; separator="\n\n">

<context.structs : {s | <_struct(s)>}; separator="\n\n">

<context.services : {s | <_service(s)>}; separator="\n\n">

>>

_enum(enum) ::= <<
<_doc(enum.documentation)><\\\>
enum <enum.enumName> {
  <enum.elementsDocumentation : {enumElem | <_doc(enum.elementsDocumentation.(enumElem))><\\\>
  <enumElem><_maybeEnumValue(enum, enumElem)>}; separator=", ">
}
>>

_maybeEnumValue(enum, enumElem) ::= <<
<if(enum.byEnumConstant)>=<enum.byEnumConstant.(enumElem)><endif>
>>

_struct(struct) ::= <<
<_doc(struct.documentation)><\\\>
<_structkind(struct)> <struct.structName><_structAnnotations(struct.idlAnnotations)> {
  <struct.fields : {f | <if(!f.internal)><_field(f)>;<endif>}; separator="\n">
}
>>

_structAnnotations(annotations) ::= <<
<if(annotations)> ( <annotations.keys : {key | <_structAnnotation(key, annotations.(key))>}; separator=", "> )<endif>
>>

_structAnnotation(annotationKey, annotationValue) ::= <<
<annotationKey> = "<annotationValue>"
>>

_structkind(struct) ::= <<
<if(struct.struct)>struct<endif><if(struct.exception)>exception<endif><if(struct.union)>union<endif>
>>

_field(field) ::= <<
<_doc(field.documentation)><\\\>
<field.id>: <_fieldRequiredness(field)><field.thriftType> <field.name><_fieldAnnotations(field.idlAnnotations)>
>>

_fieldAnnotations(annotations) ::= <<
<if(annotations)> ( <annotations.keys : {key | <_fieldAnnotation(key, annotations.(key))>}; separator=", "> )<endif>
>>

_fieldAnnotation(annotationKey, annotationValue) ::= <<
<annotationKey> = "<annotationValue>"
>>

_fieldRequiredness(field) ::= <<
<if(field.requiredness)><field.requiredness> <endif>
>>

_service(service) ::= <<
<_doc(service.documentation)><\\\>
service <service> <if(service.parentService)>extends <service.parentService> <endif>{
  <service.declaredMethods : {method | <_method(service.declaredMethods.(method))>}; separator="\n">
}
>>

_method(method) ::= <<
<_doc(method.documentation)><\\\>
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

_doc(documentation) ::= <<
<if(documentation)>
/**
<documentation : {line |  * <line>}; separator="\n">
 */
<endif>
>>
