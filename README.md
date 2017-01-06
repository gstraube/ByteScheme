# ByteScheme

A Scheme compiler written in Java 8 using ANTLR v4. It compiles Scheme to Java which is then compiled to byte code with the help of Javassist.

Currently supports the definition of variable and procedures as well as the evaluation of expressions. Expressions can be either constants, quotations, procedure applications or variable references. Constants consist of integers, booleans, characters and strings. It is possible to quote constants, identifiers, lists and vectors.


![Build status](https://travis-ci.org/gstraube/ByteScheme.svg?branch=master)
