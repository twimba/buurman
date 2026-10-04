package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.buurman.util.DocumentLanguages;

/**
 * Synthetic proof, per language, that the number-word lint of authoritative documents flags a
 * spelled-out quantity and spares the articles and pronouns that are spelled like the numeral one.
 *
 * <p>Each language lists {@code flagged} snippets (bare numerals of the tenancy-law quantities and
 * the contexts in which "one" is a quantity) and {@code spared} snippets (articles, pronouns,
 * idioms and ordinary words that must not trip the lint).
 */
@DisplayName("number-word lint per authoritative language")
class LeaseNumberWordsTest {

  record Case(String language, List<String> flagged, List<String> spared) {}

  private static Case c(String language, List<String> flagged, List<String> spared) {
    return new Case(language, flagged, spared);
  }

  static final List<Case> CASES =
      List.of(
          c(
              "en",
              List.of(
                  "two",
                  "three",
                  "four",
                  "five",
                  "six",
                  "seven",
                  "eight",
                  "nine",
                  "ten",
                  "eleven",
                  "twelve",
                  "fourteen",
                  "fifteen",
                  "twenty",
                  "twenty-one",
                  "twenty-four",
                  "thirty",
                  "thirty-six",
                  "sixty",
                  "ninety",
                  "twice the net rent",
                  "three times the net rent",
                  "thrice",
                  "a twofold amount",
                  "within one day",
                  "for one month",
                  "one calendar month",
                  "one year",
                  "one week",
                  "at least one instalment",
                  "more than one tenant",
                  "once a year",
                  "once per month"),
              List.of(
                  "an agreement between the parties",
                  "no one may enter",
                  "the tenants shall assist one another",
                  "the one who signs",
                  "each one of the parties",
                  "any one of the following",
                  "once the lease has ended",
                  "one's own costs",
                  "someone",
                  "a day that falls on a Sunday",
                  "a period of 14 days",
                  "one of the parties",
                  "the two parties agree")),
          c(
              "de",
              List.of(
                  "zwei",
                  "drei",
                  "vier",
                  "fünf",
                  "sechs",
                  "sieben",
                  "acht Wochen",
                  "neun",
                  "zehn",
                  "elf",
                  "zwölf",
                  "vierzehn",
                  "fünfzehn",
                  "zwanzig",
                  "einundzwanzig",
                  "vierundzwanzig",
                  "dreißig",
                  "sechsunddreißig",
                  "sechzig",
                  "neunzig",
                  "zweimal",
                  "dreimal",
                  "zweifach",
                  "innerhalb eines Tages",
                  "binnen einem Tag",
                  "einen Monat",
                  "eine Woche",
                  "ein Jahr",
                  "einmal jährlich",
                  "einmal pro Jahr",
                  "ein halbes Jahr"),
              List.of(
                  "ein Mieter",
                  "eine Vereinbarung",
                  "an einem Tag, der auf einen Sonntag fällt",
                  "noch einmal",
                  "die Sorgfalt in Acht nehmen",
                  "außer Acht lassen",
                  "Acht geben",
                  "einer Frist von 14 Tagen",
                  "eines Tages",
                  "einen Vertrag",
                  "einmalig",
                  "die zwei Parteien")),
          c(
              "fr",
              List.of(
                  "deux",
                  "trois",
                  "quatre",
                  "cinq",
                  "six",
                  "sept",
                  "huit",
                  "neuf jours",
                  "dix",
                  "onze",
                  "douze",
                  "quatorze",
                  "quinze",
                  "vingt",
                  "vingt-et-un",
                  "vingt-quatre",
                  "trente",
                  "trente-six",
                  "soixante",
                  "quatre-vingt-dix",
                  "deux fois le loyer",
                  "un mois",
                  "une semaine",
                  "un an",
                  "une année",
                  "pendant un jour",
                  "une fois par an",
                  "le double du loyer",
                  "en double exemplaire",
                  "une demi-journée"),
              List.of(
                  "un locataire",
                  "une convention",
                  "un jour qui tombe un dimanche",
                  "une fois que le bail a pris fin",
                  "un bâtiment neuf",
                  "4 pour cent",
                  "dans un délai de 14 jours",
                  "dans un délai raisonnable",
                  "chacune des deux parties",
                  "la fin d’une année civile",
                  "résilier pour un jour convenu")),
          c(
              "es",
              List.of(
                  "dos",
                  "tres",
                  "cuatro",
                  "cinco",
                  "seis",
                  "siete",
                  "ocho",
                  "nueve",
                  "diez",
                  "once",
                  "doce",
                  "catorce",
                  "quince",
                  "veinte",
                  "veintiuno",
                  "veinticuatro",
                  "treinta",
                  "treinta y seis",
                  "sesenta",
                  "noventa",
                  "dos veces la renta",
                  "un mes",
                  "una semana",
                  "un año",
                  "dentro de un día",
                  "una vez al año",
                  "el doble de la renta",
                  "por duplicado",
                  "en doble ejemplar",
                  "año y medio"),
              List.of(
                  "un arrendatario",
                  "una cláusula",
                  "uno de los arrendatarios",
                  "una vez finalizado el contrato",
                  "una vez que el contrato termine",
                  "un día que sea festivo",
                  "5 por ciento",
                  "un plazo de 14 días",
                  "las dos partes",
                  "terminar un año natural",
                  "por un día convenido")),
          c(
              "pt",
              List.of(
                  "dois",
                  "duas",
                  "três",
                  "quatro",
                  "cinco",
                  "seis",
                  "sete",
                  "oito",
                  "nove",
                  "dez",
                  "onze",
                  "doze",
                  "catorze",
                  "quinze",
                  "vinte",
                  "trinta",
                  "trinta e seis",
                  "sessenta",
                  "noventa",
                  "duas vezes a renda",
                  "um mês",
                  "uma semana",
                  "um ano",
                  "dentro de um dia",
                  "uma vez por ano",
                  "uma vez ao ano",
                  "o dobro da renda",
                  "em duplicado",
                  "um ano e meio"),
              List.of(
                  "um arrendatário",
                  "uma cláusula",
                  "uma vez que o contrato cessou",
                  "uma vez terminado o contrato",
                  "um dia que seja feriado",
                  "5 por cento",
                  "um dos contraentes",
                  "um prazo de 14 dias",
                  "as duas partes",
                  "o termo de um ano civil",
                  "por um dia convenido")),
          c(
              "it",
              List.of(
                  "due",
                  "tre",
                  "quattro",
                  "cinque",
                  "sei mesi",
                  "sette",
                  "otto",
                  "nove",
                  "dieci",
                  "undici",
                  "dodici",
                  "quattordici",
                  "quindici",
                  "venti",
                  "ventuno",
                  "ventiquattro",
                  "trenta",
                  "trentasei",
                  "sessanta",
                  "novanta",
                  "due volte il canone",
                  "un mese",
                  "una settimana",
                  "un anno",
                  "entro un giorno",
                  "una volta all'anno",
                  "una volta l'anno",
                  "il doppio del canone",
                  "in doppio esemplare",
                  "in duplice copia",
                  "un anno e mezzo"),
              List.of(
                  "un conduttore",
                  "una clausola",
                  "uno dei contraenti",
                  "una volta scaduto il contratto",
                  "una volta che il contratto è cessato",
                  "5 per cento",
                  "un giorno che cada di domenica",
                  "un termine di 14 giorni",
                  "ciascuna delle due parti",
                  "la fine di un anno solare",
                  "per un giorno convenuto")),
          c(
              "sv",
              List.of(
                  "två",
                  "tre",
                  "fyra",
                  "fem",
                  "sex",
                  "sju",
                  "åtta",
                  "nio",
                  "tio",
                  "elva",
                  "tolv",
                  "fjorton",
                  "femton",
                  "tjugo",
                  "tjugoett",
                  "tjugofyra",
                  "trettio",
                  "trettiosex",
                  "sextio",
                  "nittio",
                  "två gånger hyran",
                  "dubbla hyran",
                  "en månad",
                  "ett år",
                  "en vecka",
                  "inom en dag",
                  "en gång per år",
                  "en gång om året",
                  "en gång i veckan",
                  "ett halvt år"),
              List.of(
                  "en hyresgäst",
                  "ett avtal",
                  "en dag som infaller på en söndag",
                  "någon gång",
                  "en gång för alla",
                  "en bestämmelse i avtalet",
                  "en tid av 14 dagar",
                  "de två parterna")),
          c(
              "da",
              List.of(
                  "to",
                  "tre",
                  "fire",
                  "fem",
                  "seks",
                  "syv",
                  "otte",
                  "ni",
                  "ti",
                  "elleve",
                  "tolv",
                  "fjorten",
                  "femten",
                  "tyve",
                  "enogtyve",
                  "fireogtyve",
                  "tredive",
                  "seksogtredive",
                  "tres",
                  "halvfems",
                  "to gange lejen",
                  "dobbelt husleje",
                  "en måned",
                  "et år",
                  "en uge",
                  "inden for en dag",
                  "en gang om året",
                  "en gang årligt",
                  "et halvt år"),
              List.of(
                  "en lejer",
                  "et lejemål",
                  "en dag, der falder på en søndag",
                  "en gang imellem",
                  "en frist på 14 dage",
                  "de to parter")),
          c(
              "nb",
              List.of(
                  "to",
                  "tre",
                  "fire",
                  "fem",
                  "seks",
                  "sju",
                  "syv",
                  "åtte",
                  "ni",
                  "ti",
                  "elleve",
                  "tolv",
                  "fjorten",
                  "femten",
                  "tjue",
                  "tjueen",
                  "tjuefire",
                  "tretti",
                  "trettiseks",
                  "seksti",
                  "nitti",
                  "to ganger husleien",
                  "dobbel husleie",
                  "en måned",
                  "et år",
                  "en uke",
                  "innen en dag",
                  "en gang i året",
                  "en gang per år",
                  "et halvt år"),
              List.of(
                  "en leietaker",
                  "et leieforhold",
                  "en dag som faller på en søndag",
                  "en gang imellom",
                  "en frist på 14 dager",
                  "de to partene")),
          c(
              "fi",
              List.of(
                  "kaksi",
                  "kolme",
                  "neljä",
                  "viisi",
                  "kuusi",
                  "seitsemän",
                  "kahdeksan",
                  "yhdeksän",
                  "kymmenen",
                  "yksitoista",
                  "kaksitoista",
                  "neljätoista",
                  "viisitoista",
                  "kaksikymmentä",
                  "kaksikymmentäyksi",
                  "kaksikymmentäneljä",
                  "kolmekymmentä",
                  "kolmekymmentäkuusi",
                  "kuusikymmentä",
                  "yhdeksänkymmentä",
                  "kahdeksan kuukauden vuokra",
                  "kahdesti",
                  "kaksinkertainen vuokra",
                  "yhden kuukauden",
                  "yhden vuoden",
                  "yksi viikko",
                  "yhden päivän",
                  "kerran vuodessa",
                  "kerran kuukaudessa",
                  "puoli vuotta"),
              List.of(
                  "yhdessä vuokranantajan kanssa",
                  "yksi osapuolista",
                  "kolmannen osapuolen",
                  "kuukauden vuokra",
                  "yhden vuokranantajan",
                  "14 päivän kuluessa",
                  "kerran",
                  "kahden osapuolen välinen",
                  "1 kerran vuodessa")),
          c(
              "el",
              List.of(
                  "δύο",
                  "τρία",
                  "τρεις",
                  "τέσσερα",
                  "πέντε",
                  "έξι",
                  "επτά",
                  "οκτώ",
                  "εννέα",
                  "δέκα",
                  "έντεκα",
                  "δώδεκα",
                  "δεκατέσσερα",
                  "δεκαπέντε",
                  "είκοσι",
                  "είκοσι ένα",
                  "τριάντα",
                  "τριάντα έξι",
                  "εξήντα",
                  "ενενήντα",
                  "δύο φορές",
                  "διπλάσιο μίσθωμα",
                  "τριπλάσιο",
                  "ένα μήνα",
                  "ένα έτος",
                  "μία εβδομάδα",
                  "ενός μηνός",
                  "εντός μίας ημέρας",
                  "μία φορά το έτος",
                  "μία φορά τον μήνα"),
              List.of(
                  "ένας μισθωτής",
                  "μια ρήτρα",
                  "ένα συμβόλαιο",
                  "μια ημέρα που πέφτει Κυριακή",
                  "10 τοις εκατό",
                  "μία φορά και",
                  "των δύο μερών")),
          c(
              "pl",
              List.of(
                  "dwa",
                  "dwie",
                  "dwóch",
                  "trzy",
                  "cztery",
                  "pięć",
                  "sześć",
                  "siedem",
                  "osiem",
                  "dziewięć",
                  "dziesięć",
                  "jedenaście",
                  "dwanaście",
                  "czternaście",
                  "piętnaście",
                  "dwadzieścia",
                  "trzydzieści",
                  "sześćdziesiąt",
                  "dziewięćdziesiąt",
                  "dwa razy czynsz",
                  "dwukrotność czynszu",
                  "podwójny czynsz",
                  "sto złotych",
                  "jeden miesiąc",
                  "jednego roku",
                  "jeden tydzień",
                  "w ciągu jednego dnia",
                  "raz w roku",
                  "raz na rok",
                  "raz w miesiącu",
                  "półtora roku",
                  "pół roku"),
              List.of(
                  "jeden z najemców",
                  "jedna ze stron",
                  "dzień, który przypada w niedzielę",
                  "jeszcze raz",
                  "pewnego dnia",
                  "termin 14 dni",
                  "dwie strony",
                  "1 raz w roku")),
          c(
              "nl",
              List.of(
                  "twee",
                  "drie",
                  "vier",
                  "vijf",
                  "zes",
                  "zeven",
                  "acht weken",
                  "negen",
                  "tien",
                  "elf",
                  "twaalf",
                  "veertien",
                  "vijftien",
                  "twintig",
                  "dertig",
                  "zestig",
                  "negentig",
                  "tweemaal de huur",
                  "driemaal de huur",
                  "een maand",
                  "een week",
                  "een jaar",
                  "binnen een dag"),
              List.of(
                  "een huurder",
                  "een overeenkomst",
                  "een dag die valt op een zondag",
                  "de regels in acht nemen",
                  "een termijn van 14 dagen")));

  /** Fix round 1: compound duration words, English a/an, Finnish translative, period nouns. */
  static final List<Case> MORE_CASES =
      List.of(
          c(
              "en",
              List.of(
                  "a month",
                  "a week",
                  "a year",
                  "an agreement of a year",
                  "within a day",
                  "after an day",
                  "half a year",
                  "half-yearly",
                  "a fortnight",
                  "double the net rent",
                  "triple the rent",
                  "treble damages"),
              List.of("a day that falls on a Sunday", "a calendar year", "a monthly statement")),
          c(
              "de",
              List.of(
                  "eine dreimonatige Frist",
                  "zweijährig",
                  "vierzehntägig",
                  "einwöchig",
                  "zweimonatlich",
                  "die Dreimonatsfrist",
                  "Zweijahresfrist"),
              List.of("jährlich", "monatliche Zahlung", "eine mehrjährige Laufzeit")),
          c(
              "fr",
              List.of(
                  "huitaine",
                  "quinzaine",
                  "triennale",
                  "biennal",
                  "à raison d'un jour par semaine",
                  "à raison d’une journée",
                  "plus d’un jour",
                  "moins d'un jour",
                  "pendant une année civile",
                  "dans un délai de neuf mois"),
              List.of(
                  "après la fin d’une année civile",
                  "au cours d’une année civile",
                  "trimestriel",
                  "à raison de 14 jours")),
          c(
              "es",
              List.of(
                  "quincena",
                  "bienal",
                  "quinquenal",
                  "durante un año natural",
                  "durante un año civil"),
              List.of("después de terminar un año natural", "trimestral", "fin de un año civil")),
          c(
              "pt",
              List.of("quinzena", "bienal", "trienal", "durante um ano civil"),
              List.of("após o termo de um ano civil", "trimestral")),
          c(
              "it",
              List.of(
                  "quindicina",
                  "triennale",
                  "biennale",
                  "bimestre",
                  "per un anno solare",
                  "durante un anno civile"),
              List.of("dopo la fine di un anno solare", "trimestrale")),
          c(
              "sv",
              List.of("tremånaders uppsägningstid", "tvåårig", "femårigt avtal", "tvåveckors"),
              List.of("årlig", "månatlig avgift")),
          c(
              "da",
              List.of("tremåneders", "toårig", "treårig", "tougers frist"),
              List.of("årlig", "månedlig betaling")),
          c(
              "nb",
              List.of("tremåneders", "toårig", "treårig", "toukers frist"),
              List.of("årlig", "månedlig betaling")),
          c(
              "fi",
              List.of(
                  "kaksivuotinen",
                  "kolmikuukautinen",
                  "kahdeksi vuodeksi",
                  "kolmeksi kuukaudeksi",
                  "viideksi päiväksi",
                  "kolmena päivänä",
                  "yhdeksi vuodeksi"),
              List.of("vuosittain", "kuukausittain", "toistaiseksi voimassa")),
          c(
              "el",
              List.of("τρίμηνη", "διετής", "εξάμηνη", "τριετία", "δεκαπενθήμερο"),
              List.of("ετήσια", "μηνιαία")),
          c(
              "pl",
              List.of("trzymiesięczny", "dwuletni", "trzytygodniowy", "czternastodniowy"),
              List.of("roczny", "miesięczna opłata", "wieloletni")),
          c("nl", List.of("twee"), List.of("jaarlijks", "maandelijks")));

  static Stream<Case> cases() {
    return Stream.concat(CASES.stream(), MORE_CASES.stream());
  }

  private static String fragment(String text) {
    return "<div th:fragment=\"clause-a\"><p>Zij betalen " + text + " vooraf.</p></div>";
  }

  @Test
  @DisplayName("every supported document language has a rule set")
  void everyLanguageCovered() {
    assertThat(CASES.stream().map(Case::language).toList())
        .containsExactlyInAnyOrderElementsOf(DocumentLanguages.ORDERED);
  }

  @Test
  @DisplayName("an unsupported language fails loudly instead of passing silently")
  void unknownLanguageRejected() {
    assertThatThrownBy(() -> LeaseNumberWords.violations(fragment("x"), "xx"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("xx");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  @DisplayName("flags a spelled-out quantity in every listed form")
  void flagsSpelledOutNumbers(Case c) {
    for (String text : c.flagged()) {
      assertThat(LeaseNumberWords.violations(fragment(text), c.language()))
          .as("%s should flag: %s", c.language(), text)
          .isNotEmpty();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  @DisplayName("spares articles, pronouns, idioms and digit quantities")
  void sparesArticlesAndIdioms(Case c) {
    for (String text : c.spared()) {
      assertThat(LeaseNumberWords.violations(fragment(text), c.language()))
          .as("%s should spare: %s", c.language(), text)
          .isEmpty();
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  @DisplayName("is whitespace tolerant and ignores markup, comments and attributes")
  void ignoresMarkup(Case c) {
    String word = c.flagged().get(0);
    String html =
        "<!-- "
            + word
            + " -->\n<div th:fragment=\"clause-a\" data-x=\""
            + word
            + "\"><p th:text=\"${x}\">"
            + "</p></div>";
    assertThat(LeaseNumberWords.violations(html, c.language())).isEmpty();
    String spaced =
        "<div th:fragment=\"clause-a\"><p>Zij betalen\n    "
            + word.replace(" ", "\n   ")
            + "\n vooraf.</p></div>";
    assertThat(LeaseNumberWords.violations(spaced, c.language())).isNotEmpty();
  }
}
