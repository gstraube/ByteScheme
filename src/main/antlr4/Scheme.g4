grammar Scheme;

program: form* EOF;

form: definition
    | expression
    ;

definition: variable_definition
          | procedure_definition;

variable_definition: '(' 'define' IDENTIFIER expression ')';

procedure_definition: '(' 'define' '(' proc_name param* ')' definition* expression+ ')';

expression: constant
          | application
          | IDENTIFIER
          ;

application: '(' IDENTIFIER expression* ')';

datum: constant
     | list
     | IDENTIFIER;

list: '(' datum* ')';

constant: NUMBER
        | BOOLEAN
        | CHARACTER
        | STRING;

proc_name: IDENTIFIER;

param: IDENTIFIER;

IDENTIFIER: INITIAL SUBSEQUENT*
          | '+'
          | '-'
          | '...'
          ;

fragment
INITIAL: LETTER
       | SPECIAL_CHARACTER;

fragment
SUBSEQUENT: INITIAL
          | DIGIT
          | OTHER_SUBSEQUENT_CHAR
          ;

BOOLEAN: '#t' | '#f';

CHARACTER: '#\\' .
         | '#\\newline'
         | '#\\space';

STRING: '"' ~[\\|"]* '"';

NUMBER: '-'? DIGIT+;

fragment
LETTER: [a-z];

fragment
DIGIT: [0-9];

fragment
SPECIAL_CHARACTER: '!' | '$' | '%' | '&' | '*' | '/' | ':' | '<' | '=' | '>' | '?' | '~' | '_' | '^';

fragment
OTHER_SUBSEQUENT_CHAR: '.' | '+' | '-';

WHITESPACE: [ \n\t\r]+ -> skip;