# Arquitetura interna

Este documento explica como o interpretador Nexus4J está organizado internamente
e qual é o papel de cada classe Java. Todo o código fica no pacote
`org.example` (e o gerador de AST em `org.example.tools`).

## O pipeline de execução

Quando um trecho de código é executado (pela função `run` em `Nexus.java`), ele
passa pelas seguintes etapas, nesta ordem:

```
                  texto do programa
                         │
        ┌────────────────▼─────────────────┐
        │  1. Scanner.scanTokens()          │  análise léxica
        │     texto  →  List<Token>         │
        └────────────────┬─────────────────┘
                         │
        ┌────────────────▼─────────────────┐
        │  2. Parser.parse()                │  análise sintática
        │     List<Token>  →  List<Stmt>    │  (AST)
        └────────────────┬─────────────────┘
                         │   (se houve erro de sintaxe, para aqui)
        ┌────────────────▼─────────────────┐
        │  3. Resolver.resolve()            │  análise semântica
        │     anota distâncias de escopo    │  (resolução de variáveis)
        └────────────────┬─────────────────┘
                         │   (se houve erro de resolução, para aqui)
        ┌────────────────▼─────────────────┐
        │  4. Interpreter.interpret()       │  execução
        │     percorre a AST e produz       │
        │     os efeitos (print, etc.)      │
        └───────────────────────────────────┘
```

As três primeiras etapas são *análise estática* (acontecem antes de qualquer
execução); a quarta é a execução propriamente dita.

## Componentes por etapa

### 1. Análise léxica — `Scanner`, `Token`, `TokenType`

- **`Scanner.java`** percorre o código-fonte caractere a caractere e o quebra em
  *tokens*. Lida com palavras-chave, identificadores, números, strings,
  operadores, espaços em branco e comentários. Mantém um contador de linha para
  mensagens de erro.
- **`Token.java`** representa um único token: seu tipo (`TokenType`), o lexema
  (texto original), o valor literal (para números e strings) e a linha.
- **`TokenType.java`** é o enum com todos os tipos de token possíveis.

> O scanner reporta erros léxicos (caractere inesperado e string não terminada)
> por meio de `Nexus.error(int line, String message)`.

### 2. Análise sintática — `Parser`, `Expr`, `Stmt`

- **`Parser.java`** é um parser de *descida recursiva* (recursive descent). Ele
  consome a lista de tokens e constrói a **AST** (árvore sintática abstrata),
  representada como uma lista de comandos (`Stmt`). Cada método do parser
  corresponde a uma regra da gramática e codifica a precedência dos operadores.
  Também faz recuperação de erros via `synchronize()`.
- **`Expr.java`** define os nós de **expressão** da AST. Subclasses:
  `Assign`, `Binary`, `Call`, `Get`, `Set`, `Array`, `Index`, `IndexSet`,
  `Super`, `This`, `Grouping`, `Literal`, `Logical`, `Unary`, `Variable`.
  (`Array` é o literal de lista `[...]`, `Index` é a leitura `l[i]` e
  `IndexSet` é a atribuição `l[i] = v`.)
- **`Stmt.java`** define os nós de **comando/declaração** da AST. Subclasses:
  `Block`, `Class`, `Expression`, `If`, `Function`, `Print`, `Return`, `Var`,
  `While`, `Break`, `Continue`. (O nó `While` carrega um campo `increment`
  opcional, usado pelo `for` para que `continue` ainda execute o incremento.)

Tanto `Expr` quanto `Stmt` usam o **padrão Visitor**: cada nó tem um método
`accept(Visitor)`, e os "consumidores" da AST (Resolver, Interpreter, AstPrinter)
implementam a interface `Visitor` correspondente. Isso permite adicionar novas
operações sobre a AST sem alterar as classes dos nós.

### 3. Análise semântica — `Resolver`

- **`Resolver.java`** faz uma passagem sobre a AST **antes** da execução para
  resolver o escopo léxico de cada variável. Para cada referência a variável, ele
  calcula a quantos níveis de escopo de distância está a declaração, e informa
  isso ao interpretador (`interpreter.resolve(expr, depth)`). Isso garante
  *binding* léxico correto e estável, mesmo na presença de closures.
- O Resolver também faz **verificações estáticas** que produzem erros antes da
  execução, entre elas:
  - Ler uma variável local em seu próprio inicializador.
  - Declarar duas variáveis com o mesmo nome no mesmo escopo local.
  - `return` no nível superior (fora de função).
  - `return` com valor dentro de `init`.
  - `this` fora de uma classe.
  - `super` fora de uma classe ou em classe sem superclasse.
  - Uma classe herdar de si mesma.

Ele usa os enums internos `FunctionType` (`NONE`, `FUNCTION`, `INITIALIZER`,
`METHOD`) e `ClassType` (`NONE`, `CLASS`, `SUBCLASS`) para saber o contexto atual
durante a travessia.

### 4. Execução — `Interpreter` e o runtime

- **`Interpreter.java`** é o coração do projeto. Implementa
  `Expr.Visitor<Object>` e `Stmt.Visitor<Void>`: percorre a AST e avalia cada nó,
  produzindo valores e efeitos colaterais (como `print`). Concentra as regras
  semânticas em tempo de execução: veracidade (*truthiness*), igualdade,
  conversão para texto (`stringify`), verificação de tipos dos operandos e
  despacho de chamadas. Também registra a função nativa `clock`.

O runtime é apoiado por várias classes:

- **`Environment.java`** é a tabela de símbolos. Cada `Environment` mapeia nomes
  para valores e aponta para um ambiente *enclosing* (externo), formando a cadeia
  de escopos aninhados. Oferece acesso por distância (`getAt`/`assignAt`),
  usado em conjunto com as resoluções feitas pelo Resolver.
- **`NexusCallable.java`** é a interface implementada por tudo que pode ser
  "chamado" com `(...)`: define `arity()` e `call(...)`. As implementações são as
  funções nativas, `NexusFunction` e `NexusClass`.
- **`NexusFunction.java`** representa funções e métodos definidos pelo usuário.
  Guarda a declaração (`Stmt.Function`), o *closure* (o ambiente de definição) e
  se é um inicializador. O método `bind` cria uma cópia com `this` ligado a uma
  instância (usado para métodos).
- **`NexusClass.java`** representa uma classe em runtime. É `NexusCallable`:
  chamá-la cria uma `NexusInstance` e executa o `init`, se existir. Guarda o
  nome, a superclasse e o mapa de métodos; `findMethod` faz a busca subindo a
  cadeia de herança.
- **`NexusInstance.java`** representa um objeto (instância de uma classe). Mantém
  um mapa de campos e delega a busca de métodos à sua classe.
- **`Return.java`** é uma exceção (sem stack trace) usada como mecanismo de
  controle de fluxo para implementar o comando `return`: ela "desenrola" a pilha
  de chamadas até a função que está executando.
- **`BreakException.java`** e **`ContinueException.java`** seguem a mesma ideia
  para `break` e `continue`: são lançadas pelos respectivos comandos e capturadas
  pelo laço (`visitWhileStmt`) mais próximo.
- **`RuntimeError.java`** é a exceção de erros em tempo de execução; carrega o
  `Token` onde o erro ocorreu, para informar a linha.

### Orquestração — `Nexus`

- **`Nexus.java`** é o ponto de entrada (`main`). Decide entre o REPL
  (`runPrompt`) e a execução de arquivo (`runFile`), encadeia as quatro etapas do
  pipeline em `run`, e centraliza o relato de erros (`error`, `runtimeError`,
  `report`) com as flags `hadError` e `hadRuntimeError`.

### Utilitário — `AstPrinter`

- **`AstPrinter.java`** implementa `Expr.Visitor<String>` e serve para
  **depuração**: produz uma representação textual (em notação de parênteses) de
  uma expressão. Não participa do fluxo normal de execução.

## Geração de código da AST

As classes `Expr.java` e `Stmt.java` são **geradas automaticamente** pela
ferramenta **`tools/GenerateAst.java`**. Em vez de escrever à mão as dezenas de
pequenas subclasses de nós (cada uma com seus campos, construtor e `accept`),
descreve-se cada tipo de nó em uma única linha, por exemplo:

```
"Binary   : Expr left, Token operator, Expr right"
```

e o `GenerateAst` emite o arquivo Java completo, incluindo a interface `Visitor`.

O diretório de saída é passado como argumento de linha de comando, por exemplo:

```bash
java -cp target/classes org.example.tools.GenerateAst src/main/java/org/example
```

> É uma ferramenta de *build-time*; não é chamada durante a execução do
> interpretador.

## O padrão Visitor em uma frase

Cada classe de nó (`Expr.*`, `Stmt.*`) sabe apenas "me visite" (`accept`); a
lógica de *o que fazer* com cada nó vive nas implementações de `Visitor`
(`Interpreter`, `Resolver`, `AstPrinter`). Assim, dá para adicionar novas
operações sobre a árvore sem tocar nas classes dos nós.

## Observações e pontos de atenção

O que falta para a linguagem evoluir está organizado em uma lista priorizada no
[roadmap.md](roadmap.md). Em resumo, como implementação no estilo *jlox* a
linguagem está completa; o que falta para ser uma linguagem de uso real são
recursos além do livro. Já implementado:

- **Listas** com literais `[...]`, indexação para leitura/escrita e as funções
  nativas `len`, `append` e `remove`.
- **`break` e `continue`** em laços, com verificação estática de uso fora de laço.
- **Sequências de escape em strings** (`\n`, `\t`, `\r`, `\"`, `\\`, `\0`) e
  leitura/saída em **UTF-8**.

Ainda pendente:

- **Mapas / dicionários** e demais coleções.
- **Biblioteca padrão** mais ampla (além de `clock` e das funções de lista):
  entrada do usuário, funções de string, matemática, conversões de tipo.
- **Operador de módulo `%`** e comentários de bloco.
- **Testes automatizados** e empacotamento em um JAR executável.

Veja o [roadmap.md](roadmap.md) para a lista completa e a ordem sugerida.
