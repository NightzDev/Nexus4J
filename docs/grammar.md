# Gramática e tokens da linguagem Nexus

Este documento descreve formalmente a sintaxe da linguagem Nexus: a lista de
tokens reconhecidos pelo scanner e a gramática usada pelo parser.

## Tokens

Os tipos de token estão definidos no enum `TokenType` (`TokenType.java`).

### Tokens de caractere único

```
( )   LEFT_PAREN    RIGHT_PAREN
{ }   LEFT_BRACE    RIGHT_BRACE
[ ]   LEFT_BRACKET  RIGHT_BRACKET
,     COMMA
.     DOT
-     MINUS
+     PLUS
;     SEMICOLON
/     SLASH
*     STAR
```

### Tokens de um ou dois caracteres

```
!   !=    BANG         BANG_EQUAL
=   ==    EQUAL        EQUAL_EQUAL
>   >=    GREATER      GREATER_EQUAL
<   <=    LESS         LESS_EQUAL
```

### Literais

```
IDENTIFIER   nomes de variáveis, funções, classes, parâmetros
STRING       "delimitada por aspas duplas"
NUMBER       inteiros ou decimais (sempre armazenados como double)
```

### Palavras-chave

```
and    break   class    continue  else    false
fun    for     if       nil       or      print
return super   this     true      var     while
```

A correspondência entre texto e palavra-chave é feita pela tabela `keywords`
no `Scanner`. Qualquer identificador que não esteja nessa tabela vira um token
`IDENTIFIER`.

### Token especial

```
EOF    marca o fim da entrada
```

## Regras léxicas

- **Espaços em branco** (espaço, `\r`, `\t`) são ignorados.
- **Quebras de linha** (`\n`) incrementam o contador de linhas usado nas
  mensagens de erro.
- **Comentários** começam com `//` e vão até o fim da linha.
- **Números**: uma sequência de dígitos, opcionalmente com uma parte
  fracionária (`.` seguido de mais dígitos). Não há sinal nem notação
  científica no léxico — o sinal de menos é tratado como operador unário.
- **Strings**: tudo entre aspas duplas; podem conter quebras de linha.

## Gramática (notação EBNF)

A gramática abaixo reflete a implementação do `Parser`. A precedência dos
operadores aumenta de cima para baixo (regras mais abaixo "agarram" mais forte).

### Programa e declarações

```ebnf
program        → declaration* EOF ;

declaration    → classDecl
               | funDecl
               | varDecl
               | statement ;

classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )?
                 "{" function* "}" ;

funDecl        → "fun" function ;

function       → IDENTIFIER "(" parameters? ")" block ;

parameters     → IDENTIFIER ( "," IDENTIFIER )* ;

varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
```

### Comandos (statements)

```ebnf
statement      → exprStmt
               | printStmt
               | returnStmt
               | block
               | ifStmt
               | whileStmt
               | forStmt
               | breakStmt
               | continueStmt ;

exprStmt       → expression ";" ;

printStmt      → "print" expression ";" ;

returnStmt     → "return" expression? ";" ;

block          → "{" declaration* "}" ;

ifStmt         → "if" "(" expression ")" statement
                 ( "else" statement )? ;

whileStmt      → "while" "(" expression ")" statement ;

forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                           expression? ";"
                           expression? ")" statement ;

breakStmt      → "break" ";" ;

continueStmt   → "continue" ";" ;
```

> O `forStmt` é convertido pelo parser em um `whileStmt` equivalente
> (dessugaring); não há um nó de AST específico para `for`.

### Expressões (em ordem de precedência)

```ebnf
expression     → assignment ;

assignment     → ( call "." IDENTIFIER
                 | call "[" expression "]"
                 | IDENTIFIER ) "=" assignment
               | logic_or ;

logic_or       → logic_and ( "or" logic_and )* ;

logic_and      → equality ( "and" equality )* ;

equality       → comparison ( ( "!=" | "==" ) comparison )* ;

comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;

term           → factor ( ( "-" | "+" ) factor )* ;

factor         → unary ( ( "/" | "*" ) unary )* ;

unary          → ( "!" | "-" ) unary
               | call ;

call           → primary ( "(" arguments? ")"
                          | "." IDENTIFIER
                          | "[" expression "]" )* ;

arguments      → expression ( "," expression )* ;

array          → "[" ( expression ( "," expression )* )? "]" ;

primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | array
               | IDENTIFIER
               | "this"
               | "super" "." IDENTIFIER ;
```

## Tabela de precedência (do menor para o maior)

| Nível | Operadores | Associatividade |
|-------|------------|-----------------|
| 1 (mais fraco) | `=` (atribuição) | à direita |
| 2 | `or` | à esquerda |
| 3 | `and` | à esquerda |
| 4 | `==` `!=` | à esquerda |
| 5 | `>` `>=` `<` `<=` | à esquerda |
| 6 | `+` `-` | à esquerda |
| 7 | `*` `/` | à esquerda |
| 8 | `!` `-` (unários) | à direita |
| 9 (mais forte) | `()` chamada, `.` acesso, `[]` indexação | à esquerda |

## Recuperação de erros

O parser implementa **recuperação por sincronização**: ao encontrar um erro de
sintaxe, ele descarta tokens até chegar a um ponto seguro — após um `;` ou no
início de uma nova declaração (`class`, `fun`, `var`, `for`, `if`, `while`,
`print`, `return`). Isso permite relatar vários erros de uma vez, em vez de
parar no primeiro. Veja o método `synchronize` em `Parser.java`.
