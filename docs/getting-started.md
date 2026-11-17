# Primeiros passos

Este guia mostra como compilar e executar o interpretador Nexus4J.

## Pré-requisitos

- **JDK 21** ou superior (o projeto define `maven.compiler.source/target = 21`).
- **Apache Maven** (o projeto usa o `spring-boot-starter-parent` como parent POM).

Verifique sua instalação:

```bash
java -version    # deve indicar 21 ou superior
mvn -version
```

## Compilando o projeto

A partir da raiz do repositório:

```bash
mvn compile
```

Isso compila todas as classes Java para `target/classes`.

## Executando

A classe principal é `org.example.Nexus`. Ela tem dois modos de uso, decididos
pela quantidade de argumentos passados:

| Argumentos | Comportamento |
|------------|---------------|
| Nenhum | Inicia o **REPL** (prompt interativo). |
| Um (caminho de arquivo) | Executa o **arquivo de script** informado. |
| Mais de um | Imprime `Usage: jlox [script]` e encerra com código 64. |

### Modo REPL (interativo)

```bash
mvn exec:java -Dexec.mainClass=org.example.Nexus
```

Ou, após compilar, diretamente com `java`:

```bash
java -cp target/classes org.example.Nexus
```

Você verá o prompt `>`. Digite expressões/comandos linha a linha:

```
> print "Olá, Nexus!";
Olá, Nexus!
> var a = 1 + 2; print a;
3
```

Para sair do REPL, envie EOF (`Ctrl+D` no Linux/macOS, `Ctrl+Z` no Windows).

### Executando um arquivo de script

Os scripts Nexus usam, por convenção, a extensão `.nxs`. O repositório inclui
um exemplo em `src/main/java/org/example/main.nxs`:

```bash
java -cp target/classes org.example.Nexus src/main/java/org/example/main.nxs
```

Saída esperada:

```
Fry until golden brown.
Pipe full of custard and coat with chocolate.
```

## Códigos de saída

O interpretador segue a convenção de códigos de saída do *Crafting Interpreters*:

| Código | Significado |
|--------|-------------|
| `0` | Execução bem-sucedida. |
| `64` | Uso incorreto da linha de comando (argumentos demais). |
| `65` | Erro de compilação/sintaxe no script (`hadError`). |
| `70` | Erro de execução em tempo de runtime (`hadRuntimeError`). |

> No modo REPL, erros não encerram o programa: a flag de erro é reiniciada a
> cada linha, permitindo continuar digitando.

## Ferramenta auxiliar: GenerateAst

A classe `org.example.tools.GenerateAst` é um **gerador de código** que produz
os arquivos `Expr.java` e `Stmt.java` (as classes de nós da AST). Ela **não faz
parte da execução do interpretador** — é usada apenas durante o desenvolvimento
para regenerar essas classes a partir de uma descrição compacta.

Ela recebe o diretório de saída como argumento de linha de comando:

```bash
java -cp target/classes org.example.tools.GenerateAst src/main/java/org/example
```

Veja [architecture.md](architecture.md#geração-de-código-da-ast) para detalhes.
