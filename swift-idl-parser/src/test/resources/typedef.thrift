namespace java com.facebook.test

typedef i32 SomeTypeDef
typedef SomeTypeDef SomeOtherTypeDef

typedef SomeStruct SomeStructTypeDef

struct SomeStruct {
  1: i32 intField,
  3: SomeTypeDef tdField,
  4: bool boolField,
  5: SomeStructTypeDef structField,
}


exception StrangeException {
  1: string message;
}

service TestService
{
  SomeStruct getStruct(
    1: list<string> listStrings,
    2: i64 i64Value,
    3: SomeTypeDef type,
    4: i32 i32Value)
    throws (1:StrangeException se);
}
