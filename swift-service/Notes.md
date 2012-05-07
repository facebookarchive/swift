# Overview

Swift is an easy to use, annotation-based Java library for creating Thrift serializable types and services.

# Structs 

## Field

    @ThriftStruct
    public class Bonk {
      @ThriftField(1)
      public String message;
    
      @ThriftField(2)
      public int type;
    
      public BonkField() {
      }
    } 

## Beans

    @ThriftStruct
    public class Bonk {
      private String message;
      private int type;
    
      @ThriftField(1)
      public String getMessage() {
        return message;
      }
    
      @ThriftField
      public void setMessage(String message) {
        this.message = message;
      }
    
      @ThriftField(2)
      public int getType() {
        return type;
      }
    
      @ThriftField
      public void setType(int type) {
        this.type = type;
      }
    }
 
## Constructor

    @Immutable
    @ThriftStruct
    public class Bonk {
      private final String message;
      private final int type;
    
      @ThriftConstructor
      public Bonk(
        @ThriftField(name = "message") String message,
        @ThriftField(name = "type") int type
      ) {
        this.message = message;
        this.type = type;
      }
    
      @ThriftField(1)
      public String getMessage() {
        return message;
      }
    
      @ThriftField(2)
      public int getType() {
        return type;
      }
    }

## Builder

    @Immutable
    @ThriftStruct(builder = Builder.class)
    public class Bonk {
      private final String message;
      private final int type;
    
      public Bonk(
        String message,
        int type
      ) {
        this.message = message;
        this.type = type;
      }
    
      @ThriftField(1)
      public String getMessage() {
        return message;
      }
    
      @ThriftField(2)
      public int getType() {
        return type;
      }
      
      public static class Builder {
        private String message;
        private int type;
    
        @ThriftField
        public Builder setMessage(String message) {
          this.message = message;
          return this;
        }
    
        @ThriftField
        public Builder setType(int type) {
          this.type = type;
          return this;
        }
    
        @ThriftConstructor
        public Bonk create() {
          return new Bonk(message, type);
        }
      }
    }

# Enumerations

## Implicit Value

    public enum Fruit {
      APPLE, BANANA, CHERRY
    }

## Explicit Value

    public enum Letter {
      A(65), B(66), C(67), D(68);
    
      private final int asciiValue;
    
      Letter(int asciiValue) {
    
        this.asciiValue = asciiValue;
      }
    
      @ThriftEnumValue
      public int getAsciiValue() {
        return asciiValue;
      }
    }

# Todo
* Implement required fields
* Tests using invalid struct classes
* Better error messages with tracing for nested structures

# Future
* Generate IDL
  * Maven Plugin
  * Ant Plugin 
* Verify metadata matches IDL


