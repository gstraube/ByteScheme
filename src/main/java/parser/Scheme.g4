grammar Scheme;

program: form*;

form: definition
    | expression
    ;

definition: variable_definition;

variable_definition: '(' 'define' IDENTIFIER expression ')';

expression: constant
          | IDENTIFIER
          ;

constant: NUMBER;

IDENTIFIER: INITIAL SUBSEQUENT*
          | '+'
          | '-'
          | '...'
          ;

INITIAL: LETTER
       | SPECIAL_CHARACTER;

SUBSEQUENT: INITIAL
          | DIGIT
          | OTHER_SUBSEQUENT_CHAR
          ;

NUMBER: DIGIT+;

LETTER: [a-z];

DIGIT: [0-9];

SPECIAL_CHARACTER: '!' | '$' | '%' | '&' | '*' | '/' | ':' | '<' | '=' | '>' | '?' | '~' | '_' | '^';

OTHER_SUBSEQUENT_CHAR: '.' | '+' | '-';

WHITESPACE: [ \n\t\r]+ -> skip;