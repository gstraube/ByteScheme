grammar Scheme;

program: form* EOF;

form: definition
    | expression
    ;

definition: variable_definition
          | '(' 'begin' definition* ')';

variable_definition: '(' 'define' IDENTIFIER expression ')';

expression: constant
          | quotation
          | IDENTIFIER
          ;

quotation: '(' 'quote' (quotation | datum) ')'
         | '\'' (quotation | datum);

datum: constant
     | list
     | vector
     | IDENTIFIER;

list: '(' datum* ')';

vector: '#(' datum* ')';

constant: NUMBER
        | BOOLEAN
        | CHARACTER
        | STRING;

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