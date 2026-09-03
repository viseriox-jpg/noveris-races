# Design System Noveris

Guia reutilizável para interfaces de mods do universo Noveris. Define a identidade visual compartilhada entre mods, mantendo a aparência do Noveris Staff Call, do painel do Destino e do Noveris Races.

## Princípios

1. A interface deve parecer um painel diegético do universo Noveris, não uma tela vanilla.
2. A informação deve ser legível antes de ser decorativa.
3. Cada tela tem hierarquia clara: título, estado, conteúdo e ação.
4. Nenhum texto, botão ou ícone pode se sobrepor.
5. O mesmo componente deve ter o mesmo comportamento em todos os mods.

## Identidade visual

### Cores oficiais

| Função | Cor | Hex |
|---|---|---|
| Fundo principal | Preto quente | `#0D0C09` |
| Fundo secundário | Preto amarronzado | `#17140E` |
| Moldura e linha | Amarelo vivo | `#FFD84D` |
| Botão ativo | Amarelo profundo | `#D6A800` |
| Hover | Amarelo claro | `#F2C94C` |
| Texto principal | Branco quente | `#FFFBEB` |
| Texto secundário | Bege claro | `#C9BE9B` |
| Alerta | Amarelo forte | `#FFC928` |
| Erro/perigo | Vermelho-alaranjado | `#FF6B5E` |

O amarelo deve permanecer luminoso e saturado. Não usar marrom escuro como destaque. Texto escuro sobre amarelo é proibido; textos de botões permanecem brancos.

### Moldura

- Painel centralizado com fundo `#0D0C09` e opacidade aproximada de 92%.
- Borda de 3–4 px em `#FFD84D`.
- Símbolo `+` nos quatro cantos internos.
- Linha horizontal amarela separando cabeçalho e conteúdo.
- O mundo do Minecraft pode permanecer visível atrás, sem competir com o texto.

## Tipografia e espaçamento

- Usar uma fonte pixelada única em todos os mods Noveris.
- Títulos: 16–20 px; seções: 12–14 px; corpo: 10–12 px.
- Texto principal em `#FFFBEB`; descrições em `#C9BE9B`.
- Altura mínima entre linhas: 18 px.
- Separação mínima entre blocos: 24 px.
- Quebrar texto pela largura da coluna; nunca deixar uma frase atravessar outro componente.

## Estrutura padrão de tela

### Cabeçalho

- Esquerda: nome da tela ou sistema, por exemplo `TRIBUNAL DAS ALMAS`.
- Direita: estado atual, jogador, reino ou contador.
- Divisória horizontal abaixo do cabeçalho.

### Conteúdo

Usar uma ou duas colunas com larguras calculadas pela área disponível. Cada bloco tem título curto, conteúdo limitado e espaço reservado para seu crescimento.

### Rodapé

Reservar o rodapé antes de desenhar o conteúdo. Ação principal fica centralizada; ações secundárias ficam nas laterais. Nenhuma descrição pode ocupar essa faixa.

## Componentes reutilizáveis

### Abas

Formato: `[ ABA 1 ] [ ABA 2 ] [ ABA 3 ]`.

- Todas têm a mesma largura e altura.
- Ativa: fundo `#D6A800`, texto branco.
- Inativa: fundo `#17140E`, texto branco/bege.
- Hover: `#F2C94C`.
- Trocar de aba não move o painel principal.

### Cartões

- Largura e altura uniformes.
- Símbolo no topo e nome centralizado.
- Selecionado com preenchimento amarelo.
- Espaçamento constante.
- Em telas estreitas, usar menos cartões por linha ou paginação.

### Botões

- Altura mínima: 36 px.
- Texto centralizado, branco e em caixa alta.
- Primário: `#D6A800`; secundário: `#17140E`; hover: `#F2C94C`.
- Rótulos de confirmação e perigo devem ser explícitos.
- Nunca colocar botão sobre texto, divisória ou outro botão.

### Listas e estados

- `•` para listas e `◆` para habilidades.
- Estados explícitos: `ATIVO`, `SUSPENSO`, `EM TESTE`, `BLOQUEADO`, `RECARREGANDO`.
- Mensagens rápidas usam a barra de ação do Minecraft, não linhas permanentes no painel.

## Segunda página e paginação

Quando houver muitos detalhes, manter a primeira página simples e adicionar `VER DETALHES` ou `PODERES E FRAQUEZAS`. A segunda página preserva cabeçalho, moldura, dimensões e rodapé, com `VOLTAR`. Nunca diminuir a fonte até ficar ilegível para evitar paginação.

## Estados visuais

Todo componente interativo deve diferenciar normal, hover/foco, selecionado/ativo, desativado/bloqueado, recarga e erro/confirmação pendente. O estado deve aparecer por cor e também por texto ou ícone; partículas e sons são complementares.

## Layout responsivo e anti-sobreposição

- Calcular posições a partir de `left`, `top`, `contentWidth` e `contentHeight`.
- Não usar coordenadas fixas de uma única resolução.
- Reservar cabeçalho e rodapé antes de distribuir conteúdo.
- Definir `maxLines` e largura máxima para cada bloco.
- Usar espaçamento vertical previsível entre título, descrição, lista e controles.
- Em resoluções menores, usar paginação, rolagem controlada ou grade menor.
- Testar em 854×480, 1280×720 e 1920×1080.
- Moldura e botão principal devem permanecer totalmente visíveis.

## Feedback, partículas e sons

- Partículas reforçam a função da habilidade ou estado e usam a cor do tema.
- Uma ação tem feedback visual imediato no jogador, no painel e, quando apropriado, no mundo.
- Sons recorrentes são discretos; confirmação, erro e conclusão podem ser mais marcantes.
- Partículas nunca são a única indicação de uma alteração importante.

## Checklist para novos mods

- [ ] Fundo preto quente e amarelo `#FFD84D`.
- [ ] Moldura, símbolos de canto, divisória e fonte pixelada do padrão Noveris.
- [ ] Texto claro e contraste alto.
- [ ] Estados normal, hover, ativo, bloqueado e recarga.
- [ ] Rodapé reservado antes do conteúdo.
- [ ] Textos quebrados sem sobreposição.
- [ ] Segunda página ou paginação quando necessário.
- [ ] Teste nas três resoluções.
- [ ] Partículas e sons complementam o texto.
