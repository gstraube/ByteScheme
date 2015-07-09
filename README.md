# ByteScheme

A Scheme interpreter written in Java 8 using ANTLR v4. Currently supports the definition of variable and
(non-recursive) procedures as well as the evaluation of expressions. Expressions can be either constants, quotations, 
procedure applications or variable references. Constants consist of integers, booleans, characters and strings. It is
possible to quote constants, identifiers, lists and vectors.