# Documentação do Nexus4J

**Nexus4J** é um interpretador *tree-walking* (que percorre a árvore sintática)
para a linguagem de script **Nexus**, escrito em **Java 21**. O projeto é uma
implementação no estilo do livro [*Crafting Interpreters*](https://craftinginterpreters.com/)
(a linguagem Lox), com a nomenclatura adaptada para "Nexus".

Nexus é uma linguagem:

- **Dinamicamente tipada** — os tipos são verificados em tempo de execução.
- **Interpretada** — o código-fonte é executado diretamente, sem compilação para bytecode.
- **Orientada a objetos** — com suporte a classes, herança, métodos e o construtor `init`.
- **De escopo léxico** — com closures e resolução de variáveis em uma etapa dedicada.

---

## Índice da documentação

| Documento | Conteúdo |
|-----------|----------|
| [getting-started.md](getting-started.md) | Pré-requisitos, como compilar e executar (REPL e arquivos). |
| [language-guide.md](language-guide.md) | Guia completo da linguagem Nexus: tipos, variáveis, controle de fluxo, funções, classes. |
| [grammar.md](grammar.md) | Gramática formal da linguagem e lista de tokens. |
| [architecture.md](architecture.md) | Arquitetura interna: pipeline do interpretador e papel de cada classe Java. |
| [roadmap.md](roadmap.md) | O que falta para a linguagem evoluir, em níveis de prioridade. |

---

## Visão geral rápida

Um programa Nexus se parece com isto:

```nexus
class Doughnut {
  cook() {
    print "Fry until golden brown.";
  }
}

class BostonCream < Doughnut {
  cook() {
    print "Pipe full of custard and coat with chocolate.";
  }
}

Doughnut().cook();
BostonCream().cook();
```

Saída:

```
Fry until golden brown.
Pipe full of custard and coat with chocolate.
```

> Este exemplo está disponível no repositório em
> `src/main/java/org/example/main.nxs`.

---

## Como o código-fonte vira execução

O interpretador processa o código em quatro etapas principais:

```
Código-fonte (texto)
        │
        ▼
   [ Scanner ]   → produz uma lista de Tokens
        │
        ▼
   [ Parser ]    → produz a AST (lista de Stmt / Expr)
        │
        ▼
   [ Resolver ]  → resolve escopos das variáveis (análise semântica)
        │
        ▼
   [ Interpreter ] → executa a AST e produz os efeitos (print, etc.)
```

Cada uma dessas etapas é detalhada em [architecture.md](architecture.md).

---

## Estrutura do projeto

```
Nexus4J/
├── pom.xml                       # Configuração Maven (Java 21)
├── docs/                         # Esta documentação
└── src/main/java/org/example/
    ├── Nexus.java                # Ponto de entrada (main): REPL e execução de arquivos
    ├── Scanner.java              # Análise léxica (texto → tokens)
    ├── Token.java                # Representação de um token
    ├── TokenType.java            # Enum com todos os tipos de token
    ├── Parser.java               # Análise sintática (tokens → AST)
    ├── Expr.java                 # Nós de expressão da AST
    ├── Stmt.java                 # Nós de declaração/comando da AST
    ├── Resolver.java             # Resolução léxica de escopo
    ├── Interpreter.java          # Execução da AST
    ├── Environment.java          # Tabela de símbolos / escopos aninhados
    ├── NexusCallable.java        # Interface para qualquer coisa "chamável"
    ├── NexusFunction.java        # Funções e métodos definidos pelo usuário
    ├── NexusClass.java           # Representação de uma classe em runtime
    ├── NexusInstance.java        # Instância de uma classe em runtime
    ├── Return.java               # Exceção usada para implementar `return`
    ├── RuntimeError.java         # Erro de execução com token associado
    ├── AstPrinter.java           # Utilitário para imprimir a AST (debug)
    ├── main.nxs                  # Programa de exemplo em Nexus
    └── tools/
        └── GenerateAst.java      # Gerador de código das classes Expr e Stmt
```

> Para o que ainda falta na linguagem (coleções, biblioteca padrão,
> `break`/`continue` etc.), veja o [roadmap.md](roadmap.md).
