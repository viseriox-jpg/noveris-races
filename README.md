# Noveris Races — NeoForge 1.21.1

Mod de raças jogáveis do Noveris SMP, independente do Noveris Staff Call e do Origins.

## Raças

- **Tiefling** — 10 corações, afinidade infernal, Pulso Infernal e Avanço em Fogo.
- **Licantropo** — 12 corações, fortalecimento noturno, Uivo de Caçada e Bote Predatório.
- **Draconato** — 13 corações, linhagens de Fogo/Gelo/Veneno, Sopro Elemental e Investida.
- **Harpia** — 9 corações, salto/planagem sem voo, Rajada de Vento e Impulso Alado.

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
