grammar Scheme;

program: form*;

form: definition
    | expression
    ;

definition: variable_definition;

variable_definition: '(' 'define' variable expression ')';

variable: identifier;

expression: constant
          | variable
          ;

constant: number;

identifier: initial subsequent*
          | '+'
          | '-'
          | '...'
          ;

initial: letter | '!' | '$' | '%' | '&' | '*' | '/' | ':' | '<' | '=' | '>' | '?' | '~' | '_' | '^';

subsequent: initial
          | digit
          | '.'
          | '+'
          | '-'
          ;

number: digit+;

letter: LETTER;

digit: DIGIT;

LETTER: [a-z];

DIGIT: [0-9];