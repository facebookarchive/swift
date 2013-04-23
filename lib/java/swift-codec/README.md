# Swift Codec

Swift Codec is a simple library specifying how Java objects are converted to and
from Thrift.  This library is similar to JaxB (XML) and Jackson (JSON), but
for Thrift.  Swift codec supports field, method, constructor, and builder
injection.

# Structs 

To make a Java class a Thrift struct simply add the `@ThriftStruct` annotation.
Swift will assume the Java class and the Thrift struct have the same name, so
if the Thrift struct has a different name, you will need to add a value to
annotation like this: `@ThriftStruct("MyStructName")`.

## Field

The simplest way to add a Thrift field is to annotate a public Java field with
`@ThriftField(42)`.  As with structs, Swift will assume the Java field and
Thrift field have the same name, so if they don't just add a name to the
annotation like this: `@ThriftField(value = 1, name="myFieldName")`.

    @ThriftStruct
    public class Bonk
    {
        @ThriftField(1)
        public String message;

        @ThriftField(2)
        public int type;

        public BonkField()
        {
        }
    } 

## Beans

Traditional Java beans can easily be converted to Thrift structs by annotating
the getters and setters.  Swift will link the getter and setter by name, so you
only need to specify the Thrift field id on one of them.  You can override the
Thrift field name in the annotation if necessary.

    @ThriftStruct
    public class Bonk
    {
        private String message;
        private int type;

        @ThriftField(1)
        public String getMessage()
        {
            return message;
        }

        @ThriftField
        public void setMessage(String message)
        {
            this.message = message;
        }

        @ThriftField(2)
        public int getType()
        {
            return type;
        }

        @ThriftField
        public void setType(int type)
        {
            this.type = type;
        }
    }
 
## Constructor

Swift support immutable Java objects using constructor injection.  Simply,
annotate the constructor you want Swift to use with `@ThriftConstructor`, and
Swift will automatically supply the constructor with the specified fields.
Assuming you have compiled with debug symbols on, the parameters are
automatically matched to a Thrift field (getter or Java field) by name.
Otherwise, you will need to annotate the parameters with
`@ThriftField(name = "myName")`.

    @Immutable
    @ThriftStruct
    public class Bonk
    {
        private final String message;
        private final int type;

        @ThriftConstructor
        public Bonk(String message, int type)
        {
            this.message = message;
            this.type = type;
        }

        @ThriftField(1)
        public String getMessage()
        {
            return message;
        }

        @ThriftField(2)
        public int getType()
        {
            return type;
        }
    }

## Builder

For larger immutable objects, Swift supports the builder pattern.  The Thrift
struct is linked to the builder class using the `builder` property on the
`@ThriftStruct` annotation.  Swift will look for a factory method annotated
with `@ThriftConstructor` on the builder class.  The builder can use field,
method and/or constructor injection in addition to injection into the factory
method itself.

    @Immutable
    @ThriftStruct(builder = Builder.class)
    public class Bonk
    {
        private final String message;
        private final int type;

        public Bonk(String message, int type)
        {
            this.message = message;
            this.type = type;
        }

        @ThriftField(1)
        public String getMessage()
        {
            return message;
        }

        @ThriftField(2)
        public int getType()
        {
            return type;
        }

        public static class Builder
        {
            private String message;
            private int type;

            @ThriftField
            public Builder setMessage(String message)
            {
                this.message = message;
                return this;
            }

            @ThriftField
            public Builder setType(int type)
            {
                this.type = type;
                return this;
            }

            @ThriftConstructor
            public Bonk create()
            {
                return new Bonk(message, type);
            }
        }
    }

# Enumerations

Swift automatically maps Java enumerations to a Thrift int.

## Implicit Value

Swift supports standard Java enumerations directly as a Thrift enumeration
using the Java ordinal value as the Thrift enum value.

    public enum Fruit
    {
        APPLE, BANANA, CHERRY
    }

## Explicit Value

For custom enumerations, you can annotate a method on the enumeration to
supply an int value.

    public enum Letter
    {
        A(65), B(66), C(67), D(68);

        private final int asciiValue;

        Letter(int asciiValue)
        {
            this.asciiValue = asciiValue;
        }

        @ThriftEnumValue
        public int getAsciiValue()
        {
            return asciiValue;
        }
    }

# Guice Support

A `ThriftCodec` can be bound into Guice adding the `ThriftCodecModule` to the injector and bind the codec with the fluent `ThriftCodecBinder` as follows:

    Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new ThriftCodecModule(),
            new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    thriftServerBinder(binder).bindThriftCodec(Bonk.class);
                }
            });
      
Then, simply add the `ThriftCodec` type to any `@Inject` annotated field or method.  Like this:

    @Inject
    private ThriftCodec<Bonk> bonkCodec;
    
    public void write(Bonk bonk, TProtocol protocol) throws Exception
    {
        bonkCodec.write(nwq TProtocolWriter(protocol));
    }
