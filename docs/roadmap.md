# Roadmap — o que falta para a linguagem evoluir

Este documento organiza, em níveis de prioridade, o que falta para o Nexus
deixar de ser "o interpretador do livro" e virar uma linguagem de uso real.

Como implementação no estilo *jlox* (o interpretador *tree-walking* de
*Crafting Interpreters*), a linguagem já está **funcionalmente completa**:
scanner, parser, resolver, interpreter, funções, closures, classes, herança e
`super`. Os itens abaixo são correções e recursos **além** do livro.

---

## Nível 1 — Correções e limpeza ✅ (concluído)

Pequenos defeitos e resquícios já resolvidos:

- [x] **Erros léxicos reativados.** O scanner voltou a reportar "caractere
  inesperado" e "string não terminada", via a nova sobrecarga
  `Nexus.error(int line, String message)`. Antes, entradas inválidas passavam
  silenciosamente.
- [x] **Import inválido removido** (`import com.sun.tools.javac.Main;` em
  `Scanner.java`).
- [x] **`Expr.java` legado da raiz removido** (pertencia ao pacote
  `org.example.Nexus` e estava incompleto).
- [x] **`GenerateAst` sem caminho fixo de Windows** — agora recebe o diretório
  de saída como argumento de linha de comando.
- [x] **Mensagem de uso corrigida** de `jlox` para `nexus`.

---

## Nível 2 — Recursos para ser "linguagem de verdade"

Em ordem sugerida de implementação (do mais impactante ao complementar):

- [x] **Listas**: literais `[...]`, indexação para leitura e escrita `l[i]`,
  com verificação de limites. Veja `examples/lists.nxs` e o
  [language-guide.md](language-guide.md#listas).
- [ ] **Mapas / dicionários**: coleção associativa chave→valor.
- [ ] **Biblioteca padrão** além de `clock()`:
  - [x] `len(x)` — tamanho de lista ou string.
  - [x] `append(lista, valor)` e `remove(lista, indice)`.
  - [ ] Entrada do usuário (ex.: `input()` / `read()`).
  - [ ] Funções de string (substring, busca).
  - [ ] Matemática (`sqrt`, `abs`, `floor`, `ceil`, ...).
  - [ ] Conversões de tipo (`str`, `num`).
- [ ] **Operador de módulo `%`** (e, opcionalmente, divisão inteira).
- [x] **`break` e `continue`** em laços `while`/`for` (com verificação estática
  de uso fora de laço). Veja `examples/break_continue.nxs`.
- [ ] **Escapes em strings** (`\n`, `\t`, `\"`, `\\`).
- [ ] **Comparação de strings** com `<`, `>`, `<=`, `>=` (hoje só números).
- [ ] **Comentários de bloco** `/* ... */`.
- [ ] **Funções anônimas / lambdas.**
- [ ] **Tratamento de erros** (`try` / `catch` ou equivalente).
- [ ] **Módulos / import** e **I/O de arquivo.**

---

## Nível 3 — Qualidade e ferramentas

- [ ] **Testes automatizados** (unitários para scanner/parser e de ponta a ponta
  com scripts `.nxs` de referência).
- [ ] **Empacotamento em JAR executável**: configurar `mainClass` no `pom.xml`
  (ex.: `spring-boot-maven-plugin` ou `maven-jar-plugin`) para rodar com
  `java -jar`.
- [ ] **Script runner** conveniente para executar arquivos `.nxs`.
- [ ] **Integração contínua (CI)** para compilar e rodar os testes a cada push.

---

## Resumo

| Nível | Objetivo | Estado |
|-------|----------|--------|
| 1 | Igualar e consertar a base do livro | ✅ Concluído |
| 2 | Virar linguagem usável de verdade | 🟡 Em andamento (listas, `len`/`append`/`remove` e `break`/`continue` prontos; faltam mapas, mais stdlib, `%`, escapes) |
| 3 | Qualidade, build e automação | ⬜ Pendente |
