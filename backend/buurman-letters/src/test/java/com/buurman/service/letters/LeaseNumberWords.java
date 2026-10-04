package com.buurman.service.letters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Number-word lint for authoritative lease documents: every quantity (deadline, period, multiple,
 * count) is written as DIGITS, never spelled out. One rule set per document language.
 *
 * <p>Each rule set has two kinds of patterns, joined into one regex:
 *
 * <ul>
 *   <li><b>Unambiguous numerals</b> (two..twelve, fourteen, fifteen, twenty, twenty-one,
 *       twenty-four, thirty, thirty-six, sixty, ninety and the other tens, hundred, thousand, and
 *       the multiplier words: twice, three times, double/triple, ...) are flagged wherever they
 *       occur as a whole word. For inflected languages (pl, fi, el, de) the common inflected forms
 *       are listed.
 *   <li><b>The numeral "one"</b> is spelled like an indefinite article or pronoun in most languages
 *       (a/an, un/une, ein/eine, een, uno/una, um/uma, en/ett/et/ei, yksi, ένα/μία, jeden). It is
 *       flagged only where it is a quantity: directly before a time-unit noun (month, week, year;
 *       always a quantity in a contract), before "day" after a quantity cue (within, after, per, at
 *       least, ...), in the "once a year" idiom, and for the multipliers ("the double of").
 *       Everywhere else the article or pronoun is spared. "Day" and "term/period of" are not
 *       flagged without a cue, because "a day that falls on a Sunday" and "a period of 14 days" use
 *       the article.
 * </ul>
 *
 * <p>Words that are also ordinary words are exempt by rule: de "Acht" (care) in "in Acht nehmen",
 * "außer Acht lassen", "Acht geben"; fr "pour cent" and "neuf" (new) unless it counts something;
 * es/pt/it/el "por ciento"/"por cento"/"per cento"/"τοις εκατό" (percent); en/es/it/pt "once"/"una
 * vez"/"uma vez"/"una volta" (conjunction "once the lease ended") unless followed by a frequency
 * ("once a year", "una vez al año"); fi "yhdessä" (together) and ordinals like "kolmannen" (third)
 * are not in the lists.
 *
 * <p>Also exempt, because they are not quantities of the kind the digits convention covers: "the
 * two parties" (fr "des deux parties", es "las dos partes", ...: the count of contracting parties),
 * "calendar year" nouns (fr "une année civile", es "un año natural", pt "um ano civil", it "un anno
 * solare"), and a frequency word that follows a digit ("1 kerran vuodessa", "1 raz w roku": the
 * digit is the quantity). "For a day agreed for the payment" is an article use: the prepositions fr
 * "pour", es/pt "por", it "per" are not quantity cues.
 *
 * <p>Known limits: ordinals are not linted (the citation convention covers paragraph numbers
 * separately), nor are digits-in-words written as compounds not listed here. Dutch keeps the
 * original rule of {@code LeaseDocumentFidelityTest}: "een" before maand/week/jaar, or after a
 * quantity cue before dag/termijn, is a quantity; "in acht" only as the idiom "in acht nemen".
 */
final class LeaseNumberWords {

  private LeaseNumberWords() {}

  /** Idioms are blanked out before matching; {@code normalize} collapses whitespace first. */
  private record Rule(Pattern idioms, Pattern words, boolean normalize) {}

  private static final String L = "(?<![\\p{L}\\p{N}])";
  private static final String R = "(?![\\p{L}\\p{N}])";

  private static Pattern lint(String... alternatives) {
    return Pattern.compile(
        L + "(?:" + String.join("|", alternatives) + ")" + R,
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }

  private static Pattern idiom(String regex) {
    return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }

  private static final Rule EN =
      new Rule(
          idiom("(?:the|these|those) two (?:contracting )?parties"),
          lint(
              "two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen"
                  + "|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty"
                  + "|seventy|eighty|ninety|hundred|thousand",
              "twice|thrice|twofold|threefold|two-fold|three-fold",
              "(?:within|after|before|per|than|least|most|exactly|only|until) one(?! another|['’])",
              "one (?:(?:calendar|working|business|full|whole|additional|further|extra|single) )*"
                  + "(?:day|week|month|year|hour|instalment|installment|period)s?",
              "once (?:a|per|every|each) (?:calendar )?(?:day|week|month|quarter|year)"),
          true);

  private static final Rule DE =
      new Rule(
          idiom(
              "acht(?= (?:zu )?(?:nehmen|nimmt|nahm|genommen|lassen|lässt|ließ|gelassen|geben"
                  + "|gibt|gab|gegeben)(?![\\p{L}]))"
                  + "|(?:den|der|die|diese|beide) zwei (?:vertrags)?(?:parteien|partner)"),
          lint(
              "zwei|drei|vier|f(?:ü|ue)nf|sechs|sieben|acht|neun|zehn|elf|zw(?:ö|oe)lf|dreizehn"
                  + "|vierzehn|f(?:ü|ue)nfzehn|sechzehn|siebzehn|achtzehn|neunzehn|zwanzig"
                  + "|(?:ein|zwei|drei|vier|f(?:ü|ue)nf|sechs|sieben|acht|neun)und"
                  + "(?:zwanzig|drei(?:ß|ss)ig|vierzig|f(?:ü|ue)nfzig|sechzig|siebzig|achtzig"
                  + "|neunzig)|drei(?:ß|ss)ig|vierzig|f(?:ü|ue)nfzig|sechzig|siebzig|achtzig"
                  + "|neunzig|hundert|tausend",
              "zweimal|dreimal|viermal|f(?:ü|ue)nfmal|sechsmal|zweifach|dreifach|vierfach"
                  + "|doppelt(?:e|en|er|es|em)?|zweier|dreier|anderthalb|eineinhalb|zweieinhalb"
                  + "|dreieinhalb",
              "(?:ein|eine|einen|einem|einer|eines) (?:monat(?:e|en|s)?|woche(?:n)?"
                  + "|jahr(?:e|es|en)?|quartal(?:e|en|s)?|halbjahr(?:e|es|en)?)",
              "(?:innerhalb|binnen|nach|vor|pro|je|mindestens|höchstens|längstens|spätestens"
                  + "|innert|mehr als|weniger als|länger als|kürzer als|jeden) "
                  + "(?:ein|eine|einen|einem|einer|eines) (?:tag|tages|werktag|werktages"
                  + "|kalendertag|kalendertages|arbeitstag|arbeitstages)",
              "halb(?:e|es|en|er|em)? (?:jahr|jahres|monat|monats)",
              "einmal (?:jährlich|monatlich|wöchentlich|täglich|pro|je|im|innerhalb|alle)"),
          true);

  private static final Rule FR =
      new Rule(
          idiom(
              "(?:des|les|aux|ces) deux (?:parties|contractants|époux)"
                  + "|une année (?:civile|calendaire)"),
          lint(
              "deux|trois|quatre|cinq|six|sept|huit|dix|onze|douze|treize|quatorze|quinze|seize"
                  + "|vingt|trente|quarante|cinquante|soixante|septante|huitante|octante|nonante"
                  + "|mille|(?<!pour )cent",
              "neuf (?:jours?|mois|semaines?|ans?|années?|fois|euros?|heures?|mensualités?)",
              "(?:un|une) (?:mois|an|année|semaine|trimestre|semestre)",
              "(?:dans|sous|après|apres|avant|pendant|durant|par|chaque|au plus|au moins"
                  + "|à raison d['’]|tous les) (?:un|une) jour",
              "(?:moins|plus) d['’](?:un|une) jour",
              "une fois (?:par|chaque|tous les|l['’]an|l['’]année)",
              "(?:le|au) (?:double|triple)|(?:double|triple) exemplaire",
              "et demie?|demi-(?:mois|an|année|journée|heure|semaine)"),
          true);

  private static final Rule ES =
      new Rule(
          idiom("(?:las|estas) dos partes|un año (?:natural|civil|calendario)"),
          lint(
              "dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce|trece|catorce|quince"
                  + "|dieci(?:s[eé]is|siete|ocho|nueve)|veinte|veinti\\p{L}+|treinta|cuarenta"
                  + "|cincuenta|sesenta|setenta|ochenta|noventa|(?<!por )cien(?:to)?|mil",
              "(?:un|una|uno) (?:mes|meses|semana|año|trimestre|semestre)",
              "(?:en|dentro de|cada|al menos|como mínimo|como máximo|más de|menos de|durante"
                  + "|tras|después de|antes de|pasado) (?:un|una) d[ií]a",
              "una vez (?:al|por|cada|a la|en cada)",
              "(?:el|al) (?:doble|triple|cu[aá]druple)|(?:doble|triple) ejemplar",
              "(?:en|por) (?:duplicado|triplicado)",
              "(?:año|mes|años|meses) y medio|medio (?:año|mes)"),
          true);

  private static final Rule PT =
      new Rule(
          idiom("(?:as|das|às|estas) duas partes|um ano (?:civil|calendário)"),
          lint(
              "dois|duas|tr[eê]s|quatro|cinco|seis|sete|oito|nove|dez|onze|doze|treze|catorze"
                  + "|quatorze|quinze|dezasseis|dezesseis|dezassete|dezessete|dezoito|dezanove"
                  + "|dezenove|vinte|trinta|quarenta|cinquenta|sessenta|setenta|oitenta|noventa"
                  + "|cem|(?<!por )cento|mil",
              "(?:um|uma) (?:m[eê]s|ano|semana|trimestre|semestre)",
              "(?:no|dentro de|em|cada|durante|ap[oó]s|antes de|at[eé]|mais de|menos de"
                  + "|pelo menos|no m[aá]ximo|a cada) (?:um|uma) dia",
              "uma vez (?:por|ao|a cada|em cada|cada)",
              "(?:o|ao) (?:dobro|triplo)",
              "em (?:duplicado|triplicado)",
              "(?:ano|mês|anos|meses) e meio|meio (?:ano|m[eê]s)"),
          true);

  private static final Rule IT =
      new Rule(
          idiom("(?:le|delle|alle|queste) due parti|un anno (?:solare|civile)"),
          lint(
              "due|tre|quattro|cinque|sei|sette|otto|nove|dieci|undici|dodici|tredici|quattordici"
                  + "|quindici|sedici|diciassette|diciotto|diciannove"
                  + "|vent(?:i(?:due|tre|tré|quattro|cinque|sei|sette|otto|nove)?|uno|un)"
                  + "|(?:trenta|quaranta|cinquanta|sessanta|settanta|ottanta|novanta)"
                  + "(?:uno|due|tre|tré|quattro|cinque|sei|sette|otto|nove)?"
                  + "|(?:trent|quarant|cinquant|sessant|settant|ottant|novant)un[oa]?"
                  + "|(?<!per )cento|mille|mila",
              "(?:un|uno|una) (?:mese|anno|settimana|trimestre|semestre)",
              "(?:entro|dopo|prima di|ogni|almeno|al massimo|più di|meno di|durante|oltre"
                  + "|trascorso) (?:un|uno) giorno",
              "una volta (?:all['’]anno|l['’]anno|al mese|ogni|per anno|alla settimana|il mese)",
              "(?:il|al) (?:doppio|triplo|quadruplo)",
              "doppio esemplare|doppia copia|duplice copia|triplice copia|triplice esemplare",
              "(?:anno|mese|anni|mesi) e mezzo|mezzo (?:anno|mese)"),
          true);

  private static final Rule SV =
      new Rule(
          idiom("de två parter(?:na)?"),
          lint(
              "två|tre|fyra|fem|sex|sju|åtta|nio|tio|elva|tolv|tretton|fjorton|femton|sexton"
                  + "|sjutton|arton|nitton|tjugo\\p{L}*|trettio\\p{L}*|fyrtio\\p{L}*|femtio\\p{L}*"
                  + "|sextio\\p{L}*|sjuttio\\p{L}*|åttio\\p{L}*|nittio\\p{L}*|hundra|tusen"
                  + "|dubb(?:el|elt|la|le)",
              "(?:en|ett) (?:månad(?:en|s)?|vecka|veckas?|år(?:et|s)?|kvartal|halvår)",
              "(?:inom|efter|före|under|per|varje|minst|högst|längst|senast|mer än|mindre än"
                  + "|längre än|kortare än) en dag",
              "en gång (?:per|om|varje|i|årligen|under)",
              "(?:ett )?halv(?:t|a)? (?:år|månad)|halvår"),
          true);

  private static final Rule DA =
      new Rule(
          idiom("de to parter"),
          lint(
              "to|tre|fire|fem|seks|syv|otte|ni|ti|elleve|tolv|tretten|fjorten|femten|seksten"
                  + "|sytten|atten|nitten|tyve|(?:en|to|tre|fire|fem|seks|syv|otte|ni)og"
                  + "(?:tyve|tredive|fyrre|halvtreds|treds|tres|halvfjerds|firs|halvfems)"
                  + "|tredive|fyrre|halvtreds|tres|halvfjerds|firs|halvfems|hundrede|tusind"
                  + "|dobbelt(?:e)?",
              "(?:en|et) (?:måned(?:s)?|uge|år(?:s)?|kvartal|halvår)",
              "(?:inden for|inden|efter|før|pr\\.|per|hver|mindst|højst|længst|senest|mere end"
                  + "|mindre end|længere end|kortere end|i) en dag",
              "en gang (?:om|pr|per|hver|årligt|årlig|i)",
              "(?:et )?halvt år|halvår"),
          true);

  private static final Rule NB =
      new Rule(
          idiom("de to partene"),
          lint(
              "to|tre|fire|fem|seks|sju|syv|åtte|ni|ti|elleve|tolv|tretten|fjorten|femten|seksten"
                  + "|sytten|atten|nitten|tjue\\p{L}*|tyve|tretti\\p{L}*|tredve|førti\\p{L}*"
                  + "|femti\\p{L}*|seksti\\p{L}*|sytti\\p{L}*|åtti\\p{L}*|nitti\\p{L}*|hundre"
                  + "|tusen|dobbelt(?:e)?|dobbel|det doble",
              "(?:en|ei|et) (?:måned(?:s|en)?|uke|uka|år(?:s)?|kvartal|halvår)",
              "(?:innen|etter|før|per|pr\\.|hver|minst|høyst|maksimalt|senest|mer enn|mindre enn"
                  + "|lenger enn|kortere enn) en dag",
              "en gang (?:i|per|pr|hver|om|årlig)",
              "(?:et )?halvt år|halvår"),
          true);

  private static final String FI_STEMS =
      "yksi|yhden|kaksi|kahden|kolme|kolmen|neljä|neljän|viisi|viiden|kuusi|kuuden|seitsemän"
          + "|kahdeksan|yhdeksän";

  private static final Rule FI =
      new Rule(
          idiom("kahden (?:sopimus)?osapuolen"),
          lint(
              "kaksi|kahden|kahta|kahdessa|kahdelle|kahdella|kahteen|kahdesta",
              "kolme|kolmen|kolmea|kolmessa|kolmelle|kolmella|kolmeen|kolmesta",
              "neljä|neljän|neljää|neljässä|neljälle|neljällä|neljään|neljästä",
              "viisi|viiden|viittä|viidessä|viidelle|viidellä|viiteen|viidestä",
              "kuusi|kuuden|kuutta|kuudessa|kuudelle|kuudella|kuuteen|kuudesta",
              "seitsemän|seitsemää|seitsemässä|seitsemälle|seitsemällä|seitsemään|seitsemästä",
              "kahdeksan|kahdeksaa|kahdeksassa|kahdeksalle|kahdeksalla|kahdeksaan|kahdeksasta",
              "yhdeksän|yhdeksää|yhdeksässä|yhdeksälle|yhdeksällä|yhdeksään|yhdeksästä",
              "kymmenen|kymmentä|kymmenessä|kymmenelle|kymmenellä|kymmeneen|kymmenestä",
              "(?:" + FI_STEMS + ")(?:toista|kymmen|kymmentä)\\p{L}*",
              "sata|sadan|sataa|tuhat|tuhannen|tuhatta",
              "(?:kaksin|kolmin|nelin|viisin)kertai\\p{L}*",
              "kahdesti|kolmesti|neljästi",
              "(?:yksi|yhden|yhtä|yhdelle|yhdellä|yhdestä|yhteen) (?:kuukau\\p{L}*|kuukaut\\p{L}*"
                  + "|vuoden|vuotta|vuosi|viikko\\p{L}*|viikon|päivä|päivän|päivää|päivänä"
                  + "|kalenteripäivän|arkipäivän)",
              "(?<!\\d )kerran (?:vuodessa|kuukaudessa|viikossa|vuosittain|kuukausittain|päivässä)",
              "puoli (?:vuotta|vuosi|kuukautta|vuoden)|puolivuotis\\p{L}*|puolen vuoden"),
          true);

  private static final Rule EL =
      new Rule(
          idiom(
              "(?:των|τα|τους) δύο (?:μερών|μέρη|συμβαλλομένων|συμβαλλόμενων|συμβαλλομένους"
                  + "|συμβαλλόμενους)"),
          lint(
              "δύο|δυο|τρία|τρια|τρεις|τριών|τέσσερα|τέσσερις|τεσσάρων|πέντε|έξι|εξι|επτά|εφτά"
                  + "|οκτώ|οχτώ|εννέα|εννιά|δέκα|έντεκα|δώδεκα"
                  + "|δεκα(?:τρ|τέσσερ|τεσσάρ|πέντε|έξι|εξ|επτά|εφτά|οκτώ|οχτώ|εννέα|εννιά)\\p{L}*"
                  + "|είκοσι|τριάντα|σαράντα|πενήντα|εξήντα|εβδομήντα|ογδόντα|ενενήντα"
                  + "|(?<!τοις )εκατό|εκατόν|χίλια|χιλίων|χιλιάδ\\p{L}*"
                  + "|διπλάσι\\p{L}*|τριπλάσι\\p{L}*",
              "(?:ένα|έναν|ένας|ενός|μία|μια|μιας|μίας|μίαν) (?:μήνα|μήνας|μηνός|μήνες|έτος|έτους"
                  + "|χρόνο|χρόνου|εβδομάδα|εβδομάδας|τρίμηνο|εξάμηνο)",
              "(?:εντός|μετά|πριν|ανά|κάθε|τουλάχιστον|έως|μέχρι|για) "
                  + "(?:μία|μια|μιας|μίας|μίαν|ένα|ενός) (?:ημέρα|ημέρας|ημέρες|ημέραν|μέρα|μέρας)",
              "(?:μία|μια) φορά (?:το|τον|ανά|κάθε|στο|στον|την|τη)"),
          true);

  private static final Rule PL =
      new Rule(
          idiom("dwie strony|dwóch stron|dwu stron|dwóm stronom|dwiema stronami|obu stron"),
          lint(
              "dwa|dwie|dwóch|dwu|dwóm|dwoma|dwiema|dwojga|trzy|trzech|trzem|trzema|cztery"
                  + "|czterech|czterem|czterema|pięć|pięciu|pięcioma|sześć|sześciu|sześcioma"
                  + "|siedem|siedmiu|siedmioma|osiem|ośmiu|ośmioma|dziewięć|dziewięciu"
                  + "|dziewięcioma|dziesięć|dziesięciu|dziesięcioma",
              "jedenaście|jedenastu|dwanaście|dwunastu|trzynaście|trzynastu|czternaście"
                  + "|czternastu|piętnaście|piętnastu|szesnaście|szesnastu|siedemnaście"
                  + "|siedemnastu|osiemnaście|osiemnastu|dziewiętnaście|dziewiętnastu",
              "dwadzieścia|dwudziestu|trzydzieści|trzydziestu|czterdzieści|czterdziestu"
                  + "|pięćdziesiąt|pięćdziesięciu|sześćdziesiąt|sześćdziesięciu|siedemdziesiąt"
                  + "|siedemdziesięciu|osiemdziesiąt|osiemdziesięciu|dziewięćdziesiąt"
                  + "|dziewięćdziesięciu",
              "sto|stu|tysiąc|tysiące|tysięcy",
              "(?:dwu|trzy|cztero|pięcio|sześcio)krotn\\p{L}*|podwójn\\p{L}*|potrójn\\p{L}*"
                  + "|półtora|półtorej|pół (?:roku|miesiąca|godziny)",
              "(?:jeden|jedna|jedno|jednego|jednej|jednemu|jednym|jedną) (?:miesiąc|miesiąca"
                  + "|miesiące|tydzień|tygodnia|tygodniu|rok|roku|roczn\\p{L}*|kwartał|kwartału"
                  + "|godzinę|godziny)",
              "(?:w ciągu|po|przed|co|na|przez|nie później niż|nie wcześniej niż|co najmniej"
                  + "|najwyżej|więcej niż|mniej niż|do|nie dłużej niż|nie krócej niż) "
                  + "(?:jeden|jednego|jednym) (?:dzień|dnia|dniu|dniem)",
              "(?<!\\d )raz (?:w|na|do) (?:roku|rok|miesiącu|miesiąc|kwartale|tygodniu|tydzień)"),
          true);

  private static final Rule NL =
      new Rule(
          Pattern.compile(
              "\\bin\\s+acht(?=\\s*[,.;:)!?]|\\s*$|\\s+(?:nemen|neemt|nam|genomen|neem|die|dat|en|of|te)\\b)",
              Pattern.CASE_INSENSITIVE),
          Pattern.compile(
              "\\b(twee|drie|vier|vijf|zes|zeven|acht|negen|tien|elf|twaalf|dertien|veertien|vijftien"
                  + "|twintig|dertig|veertig|vijftig|zestig|zeventig|tachtig|negentig|honderd|duizend"
                  + "|eenmaal|tweemaal|driemaal|viermaal|anderhalf|half|halve|dubbel\\w*"
                  + "|een\\s+(?:maand|maanden|week|weken|jaar|jaren)"
                  + "|(?:binnen|na|per|gedurende|elke|uiterlijk|ten\\s+minste|ten\\s+hoogste"
                  + "|langer\\s+dan|korter\\s+dan|meer\\s+dan|minder\\s+dan)\\s+een\\s+(?:dag|termijn))\\b",
              Pattern.CASE_INSENSITIVE),
          false);

  private static final Map<String, Rule> RULES =
      Map.ofEntries(
          Map.entry("en", EN),
          Map.entry("de", DE),
          Map.entry("fr", FR),
          Map.entry("es", ES),
          Map.entry("pt", PT),
          Map.entry("it", IT),
          Map.entry("sv", SV),
          Map.entry("da", DA),
          Map.entry("nb", NB),
          Map.entry("fi", FI),
          Map.entry("el", EL),
          Map.entry("pl", PL),
          Map.entry("nl", NL));

  /**
   * Spelled-out numbers left in the prose of the fragments of a document written in {@code
   * language}.
   *
   * @throws IllegalArgumentException for a language without a rule set: a new authoritative
   *     language must get its word list before it can be registered
   */
  static List<String> violations(String html, String language) {
    Rule rule =
        Optional.ofNullable(RULES.get(language))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "no number-word rules for language '" + language + "'"));
    List<String> problems = new ArrayList<>();
    LeaseDocumentFidelityTest.fragments(html)
        .forEach(
            (name, body) -> {
              String prose = LeaseDocumentFidelityTest.prose(body);
              if (rule.normalize()) {
                prose = prose.replaceAll("[\\s\\u00a0]+", " ");
              }
              if (rule.idioms() != null) {
                prose = rule.idioms().matcher(prose).replaceAll(" ");
              }
              Matcher m = rule.words().matcher(prose);
              while (m.find()) {
                problems.add("number word in " + name + ": " + m.group());
              }
            });
    return problems;
  }
}
