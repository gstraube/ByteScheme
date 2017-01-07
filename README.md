# ByteScheme

A Scheme[1] compiler written in Java 8 using ANTLR v4[2]. It compiles Scheme to Java which is then compiled to byte code with the help of Javassist[3].

Currently supports the definition of variable and procedures as well as the evaluation of expressions. Expressions can be either constants, quotations, procedure applications or variable references. Constants consist of integers, booleans, characters and strings. It is possible to quote constants, identifiers, lists and vectors.

# Links

[1] http://www.scheme-reports.org

[2] http://www.antlr.org/

[3] https://jboss-javassist.github.io/javassist/

![Build status](https://travis-ci.org/gstraube/ByteScheme.svg?branch=master)
