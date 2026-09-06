package com.noveris.races;

import java.util.Locale;

/** Fonte única dos textos exibidos nas telas de seleção e do painel da raça. */
public final class RaceInfo {
    private RaceInfo() {}

    public static String summary(Race r) {
        return switch (r) {
            case ELF -> "Explorador arcano e arqueiro.";
            case FAIRY -> "Suporte natural.";
            case SATYR -> "Mobilidade terrestre silvestre.";
            case THALASSIAN -> "Especialista em ambientes aquáticos.";
            case HUMAN -> "Pessoa comum com eficiência básica.";
            case NEPHILIM -> "Resistência sobrenatural.";
            case VAMPIRE -> "Caçador noturno com sustentação.";
            case TIEFLING -> "Resistente a fogo e ambientes hostis.";
            case LYCANTHROPE -> "Predador fortalecido pela noite.";
            case DRAGONBORN -> "Tanque ofensivo elemental.";
            case HARPY -> "Exploradora extremamente ágil.";
            case GOD -> "Entidade administrativa de poder absoluto.";
            case NPC -> "Personagem secundário para dar vida ao servidor.";
            default -> "Nenhuma raça escolhida.";
        };
    }

    public static String description(Race r) {
        return switch (r) {
            case ELF -> "Exploração, conhecimento, magia e combate à distância.";
            case FAIRY -> "Magia, suporte e conexão com a natureza.";
            case SATYR -> "Mobilidade terrestre e sobrevivência natural.";
            case THALASSIAN -> "Exploração oceânica e combate aquático.";
            case HUMAN -> "Versatilidade, ferramentas e progressão.";
            case NEPHILIM -> "Resistência sobrenatural equilibrada.";
            case VAMPIRE -> "Sustentação e combate noturno.";
            case TIEFLING -> "Sobrevivência e retaliação.";
            case LYCANTHROPE -> "Predador fortalecido pela noite.";
            case DRAGONBORN -> "Tanque ofensivo de linhagem elemental.";
            case HARPY -> "Exploração e agilidade.";
            default -> "";
        };
    }

    public static String active(Race r, FairyAffinity affinity) {
        return switch (r) {
            case ELF -> "Disparo Perfurante";
            case FAIRY -> switch (affinity) {
                case WATER -> "Véu de Maré";
                case AIR -> "Rajada Feérica";
                default -> "Raízes Prensoras";
            };
            case SATYR -> "Vigor Silvestre";
            case THALASSIAN -> "Guarda das Marés";
            case HUMAN -> "Nenhuma habilidade ativa";
            case NEPHILIM -> "Rajada de Luz";
            case VAMPIRE -> "Drenagem de Sangue";
            case TIEFLING -> "Pulso Ígneo";
            case LYCANTHROPE -> "Uivo de Caçada";
            case DRAGONBORN -> "Sopro Elemental";
            case HARPY -> "Rajada de Vento";
            case GOD -> "Julgamento Divino";
            case NPC -> "Investida de Guarda";
            default -> "—";
        };
    }

    public static String mobility(Race r, FairyAffinity affinity) {
        return switch (r) {
            case ELF -> "Recuo Acrobático";
            case FAIRY -> switch (affinity) {
                case WATER -> "Salto de Maré";
                case AIR -> "Passo do Vento";
                default -> "Salto de Cipó";
            };
            case SATYR -> "Investida Caprina";
            case THALASSIAN -> "Impulso Aquático";
            case HUMAN -> "Nenhuma habilidade de mobilidade";
            case NEPHILIM -> "Impulso Radiante";
            case VAMPIRE -> "Passo Sombrio";
            case TIEFLING -> "Avanço em Fogo";
            case LYCANTHROPE -> "Bote Predatório";
            case DRAGONBORN -> "Investida Dracônica";
            case HARPY -> "Impulso de Mobilidade";
            case GOD -> "Teleporte Celestial";
            default -> "—";
        };
    }

    public static String activeInfo(Race r, FairyAffinity affinity) {
        return switch (r) {
            case ELF -> "Projétil mágico perfurante: 2,5 corações em até 18 blocos.";
            case FAIRY -> switch (affinity) {
                case WATER -> "Empurra, apaga fogo, cura 1 coração e concede Resistência.";
                case AIR -> "Rajada em linha causa 1,5 coração, empurra e concede Velocidade.";
                default -> "Prende inimigos por 3s, causa 1 coração e purifica um efeito negativo.";
            };
            case SATYR -> "Concede velocidade e regeneração temporárias.";
            case THALASSIAN -> "Concede resistência por 6s e apaga fogo, em qualquer ambiente.";
            case NEPHILIM -> "Feixe frontal que causa 2 corações e Lentidão por 5 segundos.";
            case VAMPIRE -> "Drenagem rouba até 3 corações; mordida cura 0,5 à noite a cada 10s.";
            case TIEFLING -> "Queima e fere criaturas ao redor.";
            case LYCANTHROPE -> "Revela presas; recebe velocidade extra à noite.";
            case DRAGONBORN -> "Dispara o elemento da linhagem em uma área frontal.";
            case HARPY -> "Empurra criaturas com uma rajada frontal.";
            case GOD -> "Raio direcionado: 7 corações a até 20 blocos.";
            case NPC -> "Investida com empurrão e 1,5 coração de dano.";
            default -> "";
        };
    }

    public static String mobilityInfo(Race r, FairyAffinity affinity) {
        return switch (r) {
            case ELF -> "Recuo de ~3 blocos; remove Lentidão e protege 0,5s. 3 cargas, recarga 45s.";
            case FAIRY -> switch (affinity) {
                case WATER -> "Impulso curto, mais forte dentro ou ao lado da água. 3 cargas, recarga 45s.";
                case AIR -> "Passo rápido no chão, com salto curto. 3 cargas, recarga 45s.";
                default -> "Salto curto que funciona melhor em terreno natural. 3 cargas, recarga 45s.";
            };
            case SATYR -> "Investida terrestre veloz com salto curto. 3 cargas, recarga 45s.";
            case THALASSIAN -> "Arrancada livre na água; em terra exige chão. 3 cargas, recarga 45s.";
            case NEPHILIM -> "Impulso radiante que exige contato com o chão. 3 cargas, recarga 45s.";
            case VAMPIRE -> "Avanço sombrio terrestre na direção do olhar. 3 cargas, recarga 45s.";
            case TIEFLING -> "Avanço terrestre envolto em fogo. 3 cargas, recarga 45s.";
            case LYCANTHROPE -> "Bote terrestre longo para alcançar uma presa. 3 cargas, recarga 45s.";
            case DRAGONBORN -> "Investida terrestre pesada e horizontal. 3 cargas, recarga 45s.";
            case HARPY -> "Impulso de mobilidade único. 3 cargas, recarga 45s.";
            case GOD -> "Teleporta até 28 blocos, se houver espaço seguro.";
            default -> "";
        };
    }

    public static String[] passives(Race r) {
        return switch (r) {
            case ELF -> new String[]{"Visão controlável em baixa luz", "Velocidade adicional dentro de florestas"};
            case FAIRY -> new String[]{"Natureza: purifica veneno em terreno natural", "Água: resistência quando molhado; Ar: mobilidade aprimorada"};
            case SATYR -> new String[]{"Mais rápido e ágil sobre terreno natural", "Resistência aprimorada; cura em florestas"};
            case THALASSIAN -> new String[]{"Respira e minera normalmente na água", "Nado acelerado e visão aquática"};
            case HUMAN -> new String[]{"Pressa I permanente ao trabalhar", "Sem habilidades ativas ou poderes sobrenaturais"};
            case NEPHILIM -> new String[]{"50% menos dano de fogo", "Força temporária quando está com pouca vida"};
            case VAMPIRE -> new String[]{"Visão controlável; mordida cura 0,5 à noite a cada 10s", "Força e velocidade adicionais durante a noite"};
            case TIEFLING -> new String[]{"Imune a dano de fogo e lava; visão controlável", "Ao receber fogo, ganha Força temporária"};
            case LYCANTHROPE -> new String[]{"Força, velocidade e regeneração à noite", "Detecta criaturas em um raio curto de 6 blocos"};
            case DRAGONBORN -> new String[]{"12% de resistência física e a empurrão", "Resiste ao elemento de sua linhagem"};
            case HARPY -> new String[]{"Mobilidade aprimorada em espaços abertos", "Desempenho melhor em áreas abertas"};
            default -> new String[0];
        };
    }

    public static String[] weaknesses(Race r, DragonLineage lineage) {
        if (r == Race.DRAGONBORN && lineage == DragonLineage.FIRE)
            return new String[]{"Água e chuva causam dano", "Elemento oposto aplica penalidade temporária"};
        return switch (r) {
            case ELF -> new String[]{"Golpes corpo a corpo fortes causam Lentidão"};
            case FAIRY -> new String[]{"A regeneração é limitada em condições adversas", "Lava bloqueia habilidades"};
            case SATYR -> new String[]{"Armadura pesada reduz bônus por etapas", "Ambientes subterrâneos reduzem velocidade"};
            case THALASSIAN -> new String[]{"Desidrata ao permanecer longe da água", "Fogo aplica Fraqueza temporária"};
            case HUMAN -> new String[]{"Sem resistências naturais especiais", "Sem habilidades ativas ou de mobilidade"};
            case NEPHILIM -> new String[]{"Toda cura recebida é 20% menor", "Rajada exige mira e não oferece proteção"};
            case VAMPIRE -> new String[]{"Sol suspende poderes noturnos e roubo de vida", "Fogo aplica Fraqueza temporária; comida rende menos"};
            case TIEFLING -> new String[]{"Água e chuva causam dano", "Curas pequenas são 25% menores com fome alta"};
            case LYCANTHROPE -> new String[]{"Velocidade e regeneração exigem 6 de fome", "Durante o dia, os bônus noturnos ficam suspensos"};
            case DRAGONBORN -> new String[]{"Movimento lento e fome aumentada", "Elemento oposto aplica penalidade temporária"};
            case HARPY -> new String[]{"Golpes físicos causam Lentidão breve", "Armadura pesada limita a mobilidade"};
            default -> new String[0];
        };
    }
}
