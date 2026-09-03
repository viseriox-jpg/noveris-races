# Noveris Races — Design da Interface

Documento de referência visual e funcional para as telas do mod **Noveris Races**. A interface segue o padrão do Noveris Staff Call: painel escuro, moldura luminosa, tipografia pixelada e hierarquia simples, legível e responsiva.

## Direção visual

- **Fundo:** quase preto, `#0D0C09`, com transparência aproximada de 92%.
- **Cor de destaque:** amarelo vivo, nunca marrom: `#FFD84D`.
- **Destaque ativo/hover:** `#D6A800` e `#F2C94C`.
- **Texto principal:** branco quente `#FFFBEB`.
- **Texto secundário:** bege claro `#C9BE9B`.
- **Texto de alerta:** amarelo intenso `#FFC928`.
- **Cores de afinidade:** fogo `#FF8A4B`, gelo `#80D9FF`, veneno `#86D48A`, natureza `#8ED081`, água `#75D6F5`, ar `#E8F3F5`.

A moldura usa uma linha de 3–4 px, símbolos `+` nos quatro cantos e uma divisória horizontal abaixo do cabeçalho. Botões ativos usam preenchimento amarelo e texto branco; botões inativos usam preenchimento quase preto e texto branco/secundário. Texto escuro sobre amarelo não deve ser usado.

## Tipografia e espaçamento

- Usar a fonte pixelada do mod em títulos, rótulos e botões.
- Títulos: 16–20 px; subtítulos: 12–14 px; corpo: 10–12 px.
- Altura mínima entre linhas: 18 px; separação entre blocos: 24 px.
- Textos longos devem ser quebrados antes de desenhar; nunca podem atravessar outro bloco.

## Tela de seleção de raça

### Cabeçalho e abas

- Esquerda: `LINHAGENS DE NOVERIS`.
- Direita: reino atual, por exemplo `REINO DE ORVANNIS`.
- Abas alinhadas e com largura igual: `[ ORVANNIS ] [ NEUTROS ] [ AVARION ]`.
- Somente a aba ativa usa destaque; a grade é atualizada sem mover o painel de detalhes.

### Grade de raças

- Até quatro cartões por linha em telas largas.
- Cartões com largura igual, espaçamento constante e altura fixa.
- Cada cartão contém símbolo, nome e estado selecionado.
- O cartão selecionado recebe moldura/preenchimento amarelo.
- Meio-Sangue usa uma área própria para ascendências abaixo dos detalhes, nunca sobre o botão principal.

### Painel de detalhes

O painel tem duas colunas:

- **Esquerda:** nome, reino, corações, limite de altura, descrição, habilidades ativas e mobilidade.
- **Direita:** passivas, fraquezas/debuffs e condições especiais.

Quando o conteúdo exceder a altura disponível, mostrar `VER PODERES E FRAQUEZAS` e abrir uma segunda página. Não reduzir a fonte para caber tudo.

### Altura e ação principal

Mostrar uma única linha com `[ MENOR ] [ MÉDIO ] [ MAIOR ]`. O valor atual aparece acima: `PORTE — 95%`. Os botões alteram imediatamente a prévia e respeitam o limite máximo da raça, nunca acima de 115%.

`SELECIONAR RAÇA` fica centralizado no rodapé, separado dos controles de altura por pelo menos 20 px. Nunca deve compartilhar a linha de textos de fraqueza ou linhagem.

## Tela de teste e painel da raça

### Cabeçalho

- Esquerda: `SANGUE DE [RAÇA]`.
- Direita: `EM TESTE 4:59` ou o estado definitivo.
- Divisória horizontal abaixo do cabeçalho.

### Página 1 — resumo

- **Esquerda:** resumo, vida, porte e condições relevantes.
- **Direita:** condições atuais e efeitos ativos.
- Rodapé: lista curta de passivas e `ABRIR PODERES E FRAQUEZAS`.

### Página 2 — poderes e fraquezas

- Coluna esquerda: habilidades com ícone, nome, descrição, tecla e recarga.
- Coluna direita: passivas detalhadas, fraquezas, condições de ativação e penalidades.
- Rodapé: `VOLTAR` e, durante o teste, `TESTAR OUTRA RAÇA` ou `CONFIRMAR RAÇA`.
- Cada habilidade tem no máximo três linhas visuais; descrições maiores continuam abaixo do próprio título.

### Visão racial

O painel deve mostrar sempre um estado inequívoco: `VISÃO RACIAL — ATIVA` ou `VISÃO RACIAL — SUSPENSA`. A tecla separada alterna apenas essa visão; o ícone e o texto mudam imediatamente. O efeito só é aplicado em baixa luz quando estiver ativa.

## Confirmação e teste temporário

Ao clicar em `SELECIONAR RAÇA`, abrir: `DESEJA REALMENTE ESCOLHER ESTA RAÇA?` com `[ CONFIRMAR ] [ VOLTAR ]`.

O teste dura cinco minutos, é cancelado ao desconectar, continua após morte e permite trocar de raça fora de combate. Ao terminar, o painel pede confirmação definitiva.

## Regras de interação

- Mostrar as teclas primária, secundária, mobilidade e visão racial.
- Mobilidades usam até três cargas, intervalo mínimo de 3 segundos entre cargas e recarga de 45 segundos após esgotar.
- Exibir combate, hidratação, afinidade elemental e penalidade de armadura em `CONDIÇÕES ATUAIS`.
- Mensagens temporárias usam a barra de ação, sem criar linhas permanentes sobre o painel.
- Clique, foco do teclado e estado ativo têm feedback amarelo.

## Responsividade e prevenção de bugs

- Calcular posições a partir de `left`, `top`, `contentWidth` e `contentHeight`; não depender de coordenadas fixas.
- Reservar o rodapé antes de desenhar habilidades e fraquezas.
- Aplicar quebra de texto e `maxLines` por coluna.
- Nunca desenhar controles depois de iniciar o rodapé.
- Em resoluções menores, reduzir cartões por linha ou usar paginação, nunca sobreposição.
- O botão principal deve permanecer dentro da moldura em 854×480, 1280×720 e 1920×1080.

## Símbolos, partículas e fluxo

- Usar ícones consistentes para vida, passiva, habilidade, mobilidade, fraqueza e recarga.
- Partículas são feedback complementar, nunca a única indicação de uma habilidade.
- Cores de elemento acompanham texto e partícula, especialmente veneno verde e fogo laranja/vermelho.

Fluxo: seleção obrigatória ao entrar → reino → raça → afinidade/linhagem → porte → confirmação → teste → confirmação definitiva. A tecla do painel abre o resumo da raça escolhida a qualquer momento.
