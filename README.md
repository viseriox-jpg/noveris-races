# Noveris Races — NeoForge 1.21.1

Mod de raças jogáveis do Noveris SMP, independente do Noveris Staff Call e do Origins.

## Reinos e raças

- **Orvannis:** Elfo, Feérico, Sátiro e Thalassiano.
- **Neutros:** Humano, Nephilin, Vampiro e Meio-Sangue.
- **Avarion:** Tiefling, Licantropo, Draconato e Harpia.

Meio-Sangue exige duas ascendências diferentes e recebe versões reduzidas de características e fraquezas. Nenhuma raça possui voo livre.

## Portes

Cada raça oferece `Porte menor` e `Porte padrão`. O porte padrão é o máximo racial e nunca ultrapassa 115%. Porte menor reduz em 5% o alcance corpo a corpo e de interação. Licantropos crescem 10 pontos percentuais à noite, chegando no máximo a 110%; a transformação híbrida cresce 5 pontos.

O mod não concede qualquer benefício relacionado a encantamentos.

## Seleção

Jogadores sem raça recebem a tela de seleção obrigatória ao entrar. A raça é testada por cinco minutos. Trocas são permitidas fora de combate; desconectar cancela o teste. Ao fim, o jogador confirma definitivamente.

## Teclas padrão

- `R` — painel da raça.
- `G` — habilidade ativa.
- `V` — habilidade de mobilidade.

Todas podem ser alteradas nos controles do Minecraft.

## Administração

Comandos exigem OP nível 2 e são registrados no log:

```text
/noverisraces reset <jogador>
/noverisraces consultar <jogador>
/noverisraces definir <jogador> <raça> [linhagem]
/noverisraces listar <raça>
```

IDs aceitos: `tiefling`, `lycanthrope`, `dragonborn`, `harpy`. Linhagens: `fire`, `frost`, `venom`.

## Build

Requer Java 21:

```bash
gradle build
```

O JAR é criado em `build/libs/`.

O workflow `Build` instala automaticamente o Gradle 8.10.2 e compila o projeto com Java 21 em pushes e pull requests.

## Integração com armas de prata

Os IDs informados futuramente podem ser inseridos em:

```text
src/main/resources/data/noveris_races/tags/item/silver_weapons.json
```

Isso permite adicionar armas do servidor sem acoplar o mod ao projeto que fornece cada item.
