# Guia da linguagem Nexus

Este documento descreve a sintaxe e a semântica da linguagem Nexus, conforme
implementadas no interpretador. A linguagem é dinamicamente tipada e segue de
perto a linguagem Lox do livro *Crafting Interpreters*.

> Para a gramática formal completa, veja [grammar.md](grammar.md).

## Comentários

Apenas comentários de linha, iniciados por `//`:

```nexus
// Isto é um comentário e é ignorado pelo scanner.
print "oi"; // também pode vir após código
```

Não há comentários de bloco (`/* ... */`).

## Tipos de dados

Nexus tem um pequeno conjunto de tipos primitivos:

| Tipo | Exemplos | Representação interna (Java) |
|------|----------|------------------------------|
| **Número** | `1`, `42`, `3.14` | `Double` (todos os números são de ponto flutuante) |
| **String** | `"texto"` | `String` |
| **Booleano** | `true`, `false` | `Boolean` |
| **Nil** | `nil` | `null` |
| **Lista** | `[1, 2, 3]` | `java.util.List` |

Observações:

- Todos os números são `double`. Ao imprimir, valores inteiros são exibidos sem
  o `.0` final (ex.: `3` em vez de `3.0`), graças à função `stringify`.
- Strings usam aspas duplas e podem abranger múltiplas linhas.
- Não existe tipo caractere separado.

### Sequências de escape em strings

Dentro de uma string, a barra invertida inicia uma sequência de escape:

| Escape | Significado |
|--------|-------------|
| `\n` | Quebra de linha |
| `\t` | Tabulação |
| `\r` | Retorno de carro |
| `\"` | Aspas duplas (sem encerrar a string) |
| `\\` | Barra invertida literal |
| `\0` | Caractere nulo |

```nexus
print "primeira\nsegunda";   // duas linhas
print "ele disse \"oi\"";    // ele disse "oi"
print "caminho: a\\b";       // caminho: a\b
```

Uma sequência de escape desconhecida (ex.: `\q`) é um erro de sintaxe:
*"Invalid escape sequence '\q'."*

> **Codificação:** arquivos de script e a saída do programa são tratados como
> **UTF-8**, independentemente do sistema. Por isso acentos e outros caracteres
> Unicode (ex.: `"maçã"`, `"coração"`) funcionam normalmente. Veja
> `examples/strings.nxs`.

## Variáveis

Declare variáveis com `var`. A inicialização é opcional; sem ela, o valor é `nil`.

```nexus
var a = 10;
var semValor;     // vale nil
print semValor;   // imprime: nil

a = a + 5;        // atribuição (reatribuição)
print a;          // imprime: 15
```

Regras de escopo importantes (verificadas pelo **Resolver**):

- Não é permitido **ler uma variável local dentro de seu próprio inicializador**
  (ex.: `var a = a;` em escopo local gera erro).
- Não é permitido **declarar duas variáveis com o mesmo nome no mesmo escopo
  local**.

## Operadores

### Aritméticos

| Operador | Significado | Observação |
|----------|-------------|------------|
| `+` | Soma / concatenação | Funciona com dois números **ou** duas strings |
| `-` | Subtração / negação unária | |
| `*` | Multiplicação | |
| `/` | Divisão | |

```nexus
print 2 + 3;            // 5
print "foo" + "bar";    // foobar
print -5;               // -5
```

> O operador `+` exige **dois números** ou **duas strings**. Misturar tipos
> (ex.: `"x" + 1`) gera erro de execução: *"Operands must be two numbers or two strings."*

### Comparação e igualdade

| Operador | Significado |
|----------|-------------|
| `==` | Igual |
| `!=` | Diferente |
| `>` | Maior |
| `>=` | Maior ou igual |
| `<` | Menor |
| `<=` | Menor ou igual |

```nexus
print 1 < 2;        // true
print 3 == 3;       // true
print "a" == "a";   // true
print nil == nil;   // true
```

> Os operadores relacionais (`>`, `>=`, `<`, `<=`) exigem **números** em ambos
> os lados. Já `==` e `!=` funcionam entre quaisquer valores.

### Lógicos

| Operador | Significado | Curto-circuito |
|----------|-------------|----------------|
| `and` | E lógico | Retorna o operando esquerdo se ele for "falso", senão o direito |
| `or` | OU lógico | Retorna o operando esquerdo se ele for "verdadeiro", senão o direito |
| `!` | NÃO lógico (unário) | |

```nexus
print true and false;   // false
print nil or "padrão";  // padrão
print !true;            // false
```

### Regra de veracidade (*truthiness*)

Em contextos booleanos (condições de `if`, `while`, operadores lógicos):

- `false` e `nil` são considerados **falsos**.
- **Todo o resto** (incluindo `0` e strings vazias) é considerado **verdadeiro**.

## Controle de fluxo

### `if` / `else`

```nexus
if (condicao) {
  print "verdadeiro";
} else {
  print "falso";
}
```

As chaves são opcionais quando o corpo é um único comando, mas recomendadas.

### `while`

```nexus
var i = 0;
while (i < 3) {
  print i;
  i = i + 1;
}
```

### `for`

```nexus
for (var i = 0; i < 3; i = i + 1) {
  print i;
}
```

Internamente, o `for` é "açúcar sintático": o parser o transforma em um `while`
com inicializador e incremento (ver `forStatement` em `Parser.java`). Todas as
três cláusulas (inicializador, condição, incremento) são opcionais.

### `break` e `continue`

Dentro de laços `while` e `for`:

- **`break`** interrompe o laço imediatamente.
- **`continue`** pula para a próxima iteração.

```nexus
for (var i = 0; i < 10; i = i + 1) {
  if (i == 5) break;      // para quando chega em 5
  if (i == 2) continue;   // pula o 2
  print i;                // 0, 1, 3, 4
}
```

Detalhes importantes:

- Em um `for`, o `continue` **ainda executa o incremento** antes de reavaliar a
  condição — ou seja, não causa laço infinito.
- `break` e `continue` afetam apenas o **laço mais interno** em que estão.
- Usá-los **fora de um laço** é um erro estático: *"Can't use 'break' outside of
  a loop."* O mesmo vale dentro de uma função declarada dentro de um laço — a
  fronteira da função "corta" o laço, então `break`/`continue` ali também são erro.

> Exemplo completo em `examples/break_continue.nxs`.

## Blocos e escopo

Um bloco `{ ... }` cria um novo escopo léxico. Variáveis declaradas dentro dele
não vazam para fora:

```nexus
var x = "global";
{
  var x = "local";
  print x;   // local
}
print x;     // global
```

## `print`

`print` é um **comando embutido da linguagem** (não uma função). Avalia uma
expressão e imprime o resultado seguido de uma quebra de linha:

```nexus
print 1 + 1;     // 2
print "texto";   // texto
```

## Funções

Funções são declaradas com `fun`:

```nexus
fun saudacao(nome) {
  print "Olá, " + nome + "!";
}

saudacao("Mundo");   // Olá, Mundo!
```

### Retorno de valores

```nexus
fun soma(a, b) {
  return a + b;
}

print soma(3, 4);   // 7
```

- Uma função sem `return`, ou com `return;` sem valor, retorna `nil`.
- `return` no nível superior do programa (fora de qualquer função) é um erro,
  detectado pelo Resolver.

### Closures

Funções capturam o ambiente onde foram definidas (escopo léxico), formando
*closures*:

```nexus
fun contador() {
  var i = 0;
  fun incrementa() {
    i = i + 1;
    return i;
  }
  return incrementa;
}

var c = contador();
print c();   // 1
print c();   // 2
print c();   // 3
```

### Limites

- Uma função/método pode ter no máximo **255 parâmetros**, e uma chamada no
  máximo **255 argumentos** (limites verificados pelo Parser).
- O número de argumentos em uma chamada deve bater exatamente com a aridade
  (*arity*) da função, senão ocorre erro de execução.

## Listas

Listas são sequências ordenadas e mutáveis de valores, escritas entre colchetes:

```nexus
var nums = [1, 2, 3];
var vazia = [];
var mista = [1, "dois", true, nil, [9, 8]];   // pode misturar tipos e aninhar
```

### Acesso e atribuição por índice

Os índices são de **base 0**. Use colchetes para ler ou escrever um elemento:

```nexus
var nums = [10, 20, 30];
print nums[0];      // 10
nums[1] = 99;
print nums;         // [10, 99, 30]
```

O índice pode ser qualquer expressão que resulte em um número inteiro:

```nexus
var i = 1;
print [100, 200, 300][i + 1];   // 300
```

Erros de índice são detectados em tempo de execução:

| Situação | Mensagem |
|----------|----------|
| Índice fora do intervalo | *"List index out of range."* |
| Índice não inteiro (ex.: `1.5`) | *"List index must be an integer."* |
| Índice não numérico | *"List index must be a number."* |
| Indexar algo que não é lista | *"Only lists can be indexed."* |

### Funções nativas para listas

| Função | Aridade | Descrição |
|--------|---------|-----------|
| `len(x)` | 1 | Tamanho de uma lista **ou** de uma string. |
| `append(lista, valor)` | 2 | Adiciona `valor` ao fim da lista (muta a lista). Retorna `nil`. |
| `remove(lista, indice)` | 2 | Remove e **retorna** o elemento no índice informado. |

```nexus
var l = [1, 2, 3];
print len(l);        // 3

append(l, 4);
print l;             // [1, 2, 3, 4]

var x = remove(l, 0);
print x;             // 1
print l;             // [2, 3, 4]
```

> As listas são **mutáveis e passadas por referência**: passar uma lista para
> uma função e chamar `append`/`remove` dentro dela altera a lista original.

Um exemplo completo está em `examples/lists.nxs`.

## Função nativa: `clock`

O interpretador registra uma única função nativa no escopo global: `clock()`.
Ela retorna o tempo atual em segundos (como número de ponto flutuante), útil
para medir desempenho:

```nexus
var inicio = clock();
// ... algum trabalho ...
print clock() - inicio;   // tempo decorrido em segundos
```

## Programação orientada a objetos

### Classes e métodos

```nexus
class Pessoa {
  saudar() {
    print "Olá!";
  }
}

var p = Pessoa();   // cria uma instância (chamando a classe como função)
p.saudar();         // Olá!
```

### Campos

Campos (atributos) são criados dinamicamente por atribuição, usando a notação
de ponto:

```nexus
var p = Pessoa();
p.nome = "Ana";     // cria/define o campo
print p.nome;       // Ana
```

> Acessar um campo/propriedade inexistente gera erro:
> *"Undefined property 'x'."*

### `this`

Dentro de um método, `this` refere-se à instância sobre a qual o método foi
chamado:

```nexus
class Pessoa {
  init(nome) {
    this.nome = nome;
  }

  saudar() {
    print "Olá, eu sou " + this.nome;
  }
}

var p = Pessoa("Ana");
p.saudar();   // Olá, eu sou Ana
```

Usar `this` fora de uma classe é um erro detectado pelo Resolver.

### Construtor `init`

O método especial `init` é o **construtor**. Ele é chamado automaticamente
quando a classe é instanciada, e os argumentos passados à classe vão para ele:

```nexus
class Ponto {
  init(x, y) {
    this.x = x;
    this.y = y;
  }
}

var ponto = Ponto(3, 4);
print ponto.x;   // 3
```

Regras do `init`:

- A aridade da classe (número de argumentos esperados ao instanciar) é a aridade
  do `init`.
- `init` sempre retorna a própria instância. Um `return;` sem valor é permitido
  (retorna `this`), mas `return <valor>;` dentro de `init` é um **erro**.

### Herança

Use `<` para herdar de uma superclasse:

```nexus
class Animal {
  falar() {
    print "...";
  }
}

class Cachorro < Animal {
  falar() {
    print "Au au!";
  }
}

Cachorro().falar();   // Au au!
```

Regras de herança:

- A superclasse precisa ser realmente uma classe (senão: *"Superclass must be a class."*).
- Uma classe **não pode herdar de si mesma** (erro detectado pelo Resolver).

### `super`

`super` permite invocar um método da superclasse a partir da subclasse:

```nexus
class Animal {
  falar() {
    print "Som genérico";
  }
}

class Gato < Animal {
  falar() {
    super.falar();        // chama o método da superclasse
    print "Miau";
  }
}

Gato().falar();
// Som genérico
// Miau
```

Regras de `super`:

- Só pode ser usado **dentro de uma classe** que tenha superclasse.
- Usá-lo fora de uma classe, ou em uma classe sem superclasse, é um erro
  detectado pelo Resolver.

## Tratamento de erros

Há dois grandes momentos em que erros podem ocorrer:

1. **Erros estáticos** (sintaxe e análise de escopo): detectados pelo Parser e
   pelo Resolver antes da execução. Exemplos: `;` faltando, `return` no topo,
   `this` fora de classe. Mostram a linha e encerram com código 65.
2. **Erros de execução** (*runtime*): detectados pelo Interpreter. Exemplos:
   operandos de tipo errado, chamar algo que não é função, propriedade
   inexistente. Mostram a mensagem e a linha, e encerram com código 70.

Exemplo de mensagem de erro de runtime:

```
Operands must be numbers.
[line 3]
```
