package com.buurman.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.metadata.AlContractMetadata;
import com.buurman.domain.metadata.ArContractMetadata;
import com.buurman.domain.metadata.AtContractMetadata;
import com.buurman.domain.metadata.BaContractMetadata;
import com.buurman.domain.metadata.BeContractMetadata;
import com.buurman.domain.metadata.BgContractMetadata;
import com.buurman.domain.metadata.BrContractMetadata;
import com.buurman.domain.metadata.CaContractMetadata;
import com.buurman.domain.metadata.ChContractMetadata;
import com.buurman.domain.metadata.ClContractMetadata;
import com.buurman.domain.metadata.CoContractMetadata;
import com.buurman.domain.metadata.ContractCountryMetadata;
import com.buurman.domain.metadata.CyContractMetadata;
import com.buurman.domain.metadata.CzContractMetadata;
import com.buurman.domain.metadata.DeContractMetadata;
import com.buurman.domain.metadata.DkContractMetadata;
import com.buurman.domain.metadata.EeContractMetadata;
import com.buurman.domain.metadata.EsContractMetadata;
import com.buurman.domain.metadata.FiContractMetadata;
import com.buurman.domain.metadata.FrContractMetadata;
import com.buurman.domain.metadata.GenericContractMetadata;
import com.buurman.domain.metadata.GrContractMetadata;
import com.buurman.domain.metadata.HrContractMetadata;
import com.buurman.domain.metadata.HuContractMetadata;
import com.buurman.domain.metadata.IeContractMetadata;
import com.buurman.domain.metadata.ItContractMetadata;
import com.buurman.domain.metadata.LtContractMetadata;
import com.buurman.domain.metadata.LuContractMetadata;
import com.buurman.domain.metadata.LvContractMetadata;
import com.buurman.domain.metadata.MeContractMetadata;
import com.buurman.domain.metadata.MkContractMetadata;
import com.buurman.domain.metadata.MtContractMetadata;
import com.buurman.domain.metadata.MxContractMetadata;
import com.buurman.domain.metadata.NlContractMetadata;
import com.buurman.domain.metadata.NoContractMetadata;
import com.buurman.domain.metadata.PeContractMetadata;
import com.buurman.domain.metadata.PlContractMetadata;
import com.buurman.domain.metadata.PtContractMetadata;
import com.buurman.domain.metadata.RoContractMetadata;
import com.buurman.domain.metadata.RsContractMetadata;
import com.buurman.domain.metadata.SeContractMetadata;
import com.buurman.domain.metadata.SiContractMetadata;
import com.buurman.domain.metadata.SkContractMetadata;
import com.buurman.domain.metadata.UkContractMetadata;
import com.buurman.domain.metadata.UsContractMetadata;
import com.buurman.domain.metadata.UyContractMetadata;
import com.buurman.domain.metadata.XkContractMetadata;
import com.buurman.exception.BadRequestException;
import com.buurman.util.MoneyAmount;

@Component
public class CountryMetadataValidator {

  private static final Set<String> ENERGY_RATINGS_A_G = Set.of("A", "B", "C", "D", "E", "F", "G");

  private static final Set<String> NL_ENERGY_LABELS =
      Set.of("A++++", "A+++", "A++", "A+", "A", "B", "C", "D", "E", "F", "G");

  private static final Set<String> DE_ENERGY_RATINGS =
      Set.of("A+", "A", "B", "C", "D", "E", "F", "G", "H");

  private static final Set<String> IT_APE_RATINGS =
      Set.of("A4", "A3", "A2", "A1", "B", "C", "D", "E", "F", "G");

  private static final Set<String> PT_ENERGY_RATINGS =
      Set.of("A+", "A", "B", "B-", "C", "D", "E", "F");

  private static final Set<String> BE_ENERGY_RATINGS =
      Set.of("A+", "A", "B", "C", "D", "E", "F", "G");

  private static final Set<String> AT_ENERGY_RATINGS =
      Set.of("A++", "A+", "A", "B", "C", "D", "E", "F", "G");

  private static final Set<String> DK_ENERGY_LABELS =
      Set.of("A2015", "A2010", "A", "B", "C", "D", "E", "F", "G");

  private static final Set<String> HR_ENERGY_RATINGS =
      Set.of("A+", "A", "B", "C", "D", "E", "F", "G");

  private static final Set<String> LT_ENERGY_RATINGS =
      Set.of("A++", "A+", "A", "B", "C", "D", "E", "F", "G");

  private static final Set<String> EE_ENERGY_RATINGS =
      Set.of("A", "B", "C", "D", "E", "F", "G", "H");

  private static final Set<String> GR_ENERGY_RATINGS =
      Set.of("A+", "A", "B+", "B", "C", "D", "E", "F", "G", "H");

  private static final Set<String> SK_ENERGY_RATINGS =
      Set.of("A0", "A1", "B", "C", "D", "E", "F", "G");

  private static final Set<String> SI_ENERGY_RATINGS =
      Set.of("A1", "A2", "B1", "B2", "C", "D", "E", "F", "G");

  private static final Set<String> HU_ENERGY_RATINGS =
      Set.of("AA++", "AA+", "AA", "BB", "CC", "DD", "EE", "FF", "GG", "HH", "II", "JJ");

  private static final Set<String> LU_ENERGY_RATINGS =
      Set.of("A", "B", "C", "D", "E", "F", "G", "H", "I");

  private static final Set<String> IE_BER_RATINGS =
      Set.of(
          "A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3", "D1", "D2", "E1", "E2", "F", "G");

  private static final BigDecimal HUNDRED = new BigDecimal("100");

  public void validate(String countryCode, ContractCountryMetadata metadata) {
    List<String> errors = new ArrayList<>();

    switch (metadata) {
      case NlContractMetadata nl -> validateNl(nl, errors);
      case DeContractMetadata de -> validateDe(de, errors);
      case FrContractMetadata fr -> validateFr(fr, errors);
      case ItContractMetadata it -> validateIt(it, errors);
      case UkContractMetadata uk -> validateUk(uk, errors);
      case UsContractMetadata us -> validateUs(us, errors);
      case BeContractMetadata be -> validateBe(be, errors);
      case EsContractMetadata es -> validateEs(es, errors);
      case PtContractMetadata pt -> validatePt(pt, errors);
      case AtContractMetadata at -> validateAt(at, errors);
      case ChContractMetadata ch -> validateCh(ch, errors);
      case DkContractMetadata dk -> validateDk(dk, errors);
      case SeContractMetadata se -> validateSe(se, errors);
      case FiContractMetadata fi -> validateFi(fi, errors);
      case NoContractMetadata no -> validateNo(no, errors);
      case IeContractMetadata ie -> validateIe(ie, errors);
      case PlContractMetadata pl -> validatePl(pl, errors);
      case CzContractMetadata cz -> validateCz(cz, errors);
      case HuContractMetadata hu -> validateHu(hu, errors);
      case RoContractMetadata ro -> validateRo(ro, errors);
      case BgContractMetadata bg -> validateBg(bg, errors);
      case SkContractMetadata sk -> validateSk(sk, errors);
      case SiContractMetadata si -> validateSi(si, errors);
      case HrContractMetadata hr -> validateHr(hr, errors);
      case LtContractMetadata lt -> validateLt(lt, errors);
      case LvContractMetadata lv -> validateLv(lv, errors);
      case EeContractMetadata ee -> validateEe(ee, errors);
      case GrContractMetadata gr -> validateGr(gr, errors);
      case MtContractMetadata mt -> validateMt(mt, errors);
      case CyContractMetadata cy -> validateCy(cy, errors);
      case LuContractMetadata lu -> validateLu(lu, errors);
      case RsContractMetadata rs -> validateRs(rs, errors);
      case BaContractMetadata ba -> validateBa(ba, errors);
      case AlContractMetadata al -> validateAl(al, errors);
      case MeContractMetadata me -> validateMe(me, errors);
      case MkContractMetadata mk -> validateMk(mk, errors);
      case XkContractMetadata xk -> validateXk(xk, errors);
      case CaContractMetadata ca -> validateCa(ca, errors);
      case MxContractMetadata mx -> validateMx(mx, errors);
      case BrContractMetadata br -> validateBr(br, errors);
      case ArContractMetadata ar -> validateAr(ar, errors);
      case ClContractMetadata cl -> validateCl(cl, errors);
      case CoContractMetadata co -> validateCo(co, errors);
      case PeContractMetadata pe -> validatePe(pe, errors);
      case UyContractMetadata uy -> validateUy(uy, errors);
      case GenericContractMetadata gen -> validateGeneric(gen, errors);
    }

    if (!errors.isEmpty()) {
      throw new BadRequestException(
          "Country metadata validation failed for "
              + countryCode
              + ": "
              + String.join("; ", errors));
    }
  }

  private void validateNl(NlContractMetadata nl, List<String> errors) {
    validateEnum(
        nl.sectorClassification(),
        Set.of("VRIJE_SECTOR", "GEREGULEERD", "MIDDENHUUR"),
        "Sector Classification",
        errors);
    validateEnum(nl.energyLabel(), NL_ENERGY_LABELS, "Energy Label", errors);
    requireRange(nl.wwsPoints(), 0, 500, "WWS Points", errors);
    requireNonNegative(nl.liberalizationThreshold(), "Liberalization Threshold", errors);
    requireNonNegative(nl.totalServiceCostsAmount(), "Total Service Costs Amount", errors);
  }

  private void validateDe(DeContractMetadata de, List<String> errors) {
    validateEnum(
        de.rentType(), Set.of("STAFFELMIETE", "INDEXMIETE", "STANDARD"), "Rent Type", errors);
    validateEnum(
        de.energyCertificateType(),
        Set.of("VERBRAUCH", "BEDARF"),
        "Energy Certificate Type",
        errors);
    validateEnum(
        de.energyCertificateRating(), DE_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireRange(de.kautionMonths(), 0, 3, "Kaution Months", errors);
    requireNonNegative(de.nebenkostenAmount(), "Nebenkosten Amount", errors);
    requireNonNegative(de.kautionAmount(), "Kaution Amount", errors);
    requireNonNegativeDecimal(de.energyCertificateValue(), "Energy Certificate Value", errors);
    requireMaxDecimal(
        de.energyCertificateValue(), new BigDecimal("500"), "Energy Certificate Value", errors);
  }

  private void validateFr(FrContractMetadata fr, List<String> errors) {
    validateEnum(fr.diagnosticDpe(), ENERGY_RATINGS_A_G, "DPE Rating", errors);
    requireNonNegative(fr.referenceRentPrice(), "Reference Rent Price", errors);
    requireNonNegative(fr.maxRentPrice(), "Maximum Rent Price", errors);
    requireNonNegative(fr.cautionAmount(), "Caution Amount", errors);
    requireRange(fr.cautionMonths(), 0, 2, "Caution Months", errors);
  }

  private void validateIt(ItContractMetadata it, List<String> errors) {
    validateEnum(
        it.contractCategory(),
        Set.of("LIBERO", "CONCORDATO", "TRANSITORIO", "STUDENTI"),
        "Contract Category",
        errors);
    validateEnum(it.apeRating(), IT_APE_RATINGS, "APE Rating", errors);
    if (it.cedolareRate() != null) {
      if (it.cedolareRate().compareTo(BigDecimal.ZERO) < 0
          || it.cedolareRate().compareTo(HUNDRED) > 0) {
        errors.add("Cedolare Rate must be between 0 and 100");
      }
    }
    requireNonNegative(it.depositoAmount(), "Deposito Amount", errors);
    requireRange(it.depositoMonths(), 0, 3, "Deposito Months", errors);
  }

  private void validateUk(UkContractMetadata uk, List<String> errors) {
    validateEnum(
        uk.tenancyType(),
        Set.of("AST", "PERIODIC", "REGULATED", "COMPANY_LET"),
        "Tenancy Type",
        errors);
    validateEnum(uk.depositScheme(), Set.of("DPS", "MYDEPOSITS", "TDS"), "Deposit Scheme", errors);
    validateEnum(uk.epcRating(), ENERGY_RATINGS_A_G, "EPC Rating", errors);
    requireNonNegative(uk.depositAmount(), "Deposit Amount", errors);
  }

  private void validateUs(UsContractMetadata us, List<String> errors) {
    if (us.state() != null) {
      Set<String> validStates =
          Set.of(
              "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID", "IL",
              "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE",
              "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD",
              "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY");
      if (!validStates.contains(us.state())) {
        errors.add("State must be a valid US state or DC code");
      }
    }
    requireRange(us.securityDepositMonths(), 0, 12, "Security Deposit Months", errors);
    requireNonNegative(us.securityDepositLimit(), "Security Deposit Limit", errors);
  }

  private void validateBe(BeContractMetadata be, List<String> errors) {
    validateEnum(be.region(), Set.of("BRUSSELS", "WALLONIA", "FLANDERS"), "Region", errors);
    validateEnum(
        be.energyCertificateRating(), BE_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    validateEnum(
        be.depositType(),
        Set.of("BLOCKED_ACCOUNT", "BANK_GUARANTEE", "OCMW_GUARANTEE"),
        "Deposit Type",
        errors);
    requireNonNegativeDecimal(be.indexationBase(), "Indexation Base", errors);
    requireRange(be.depositMonths(), 0, 3, "Deposit Months", errors);
  }

  private void validateEs(EsContractMetadata es, List<String> errors) {
    validateEnum(
        es.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireRange(es.fianzaMonths(), 0, 2, "Fianza Months", errors);
    requireNonNegative(es.fianzaAmount(), "Fianza Amount", errors);
    requireNonNegativeDecimal(es.referencePriceIndex(), "Reference Price Index", errors);
    requireNonNegative(es.garantiaAdicionalAmount(), "Garantia Adicional Amount", errors);
    requireRange(es.garantiaAdicionalMonths(), 0, 2, "Garantia Adicional Months", errors);
  }

  private void validatePt(PtContractMetadata pt, List<String> errors) {
    validateEnum(pt.nrauRegime(), Set.of("NRAU", "VINCULISTICO", "RAU"), "NRAU Regime", errors);
    validateEnum(
        pt.energyCertificateRating(), PT_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegativeDecimal(pt.updateCoefficient(), "Update Coefficient", errors);
  }

  private void validateGeneric(GenericContractMetadata gen, List<String> errors) {
    validateEnum(
        gen.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegativeInt(gen.maxDepositMonths(), "Max Deposit Months", errors);
  }

  // --- DACH + Nordics + Ireland ---

  private void validateAt(AtContractMetadata at, List<String> errors) {
    validateEnum(
        at.mietrechtsgesetzCategory(),
        Set.of("MRG", "WGG", "ABGB"),
        "Mietrechtsgesetz Category",
        errors);
    validateEnum(
        at.energyCertificateRating(), AT_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(at.betriebskostenAmount(), "Betriebskosten Amount", errors);
    requireRange(at.kautionMonths(), 0, 6, "Kaution Months", errors);
    requireNonNegative(at.kautionAmount(), "Kaution Amount", errors);
    requireNonNegative(at.richtwertmiete(), "Richtwertmiete", errors);
  }

  private void validateCh(ChContractMetadata ch, List<String> errors) {
    requireNonNegative(ch.nebenkostenAmount(), "Nebenkosten Amount", errors);
    requireRange(ch.kautionMonths(), 0, 3, "Kaution Months", errors);
    requireNonNegative(ch.kautionAmount(), "Kaution Amount", errors);
    requireNonNegativeDecimal(ch.referenzzinssatz(), "Referenzzinssatz", errors);
  }

  private void validateDk(DkContractMetadata dk, List<String> errors) {
    validateEnum(dk.lejelovType(), Set.of("PRIVATE", "ALMEN"), "Lejelov Type", errors);
    validateEnum(dk.energyLabel(), DK_ENERGY_LABELS, "Energy Label", errors);
    requireNonNegative(dk.depositumAmount(), "Depositum Amount", errors);
    requireRange(dk.depositumMonths(), 0, 3, "Depositum Months", errors);
    requireNonNegative(dk.forudbetalingAmount(), "Forudbetaling Amount", errors);
    requireRange(dk.forudbetalingMonths(), 0, 3, "Forudbetaling Months", errors);
  }

  private void validateSe(SeContractMetadata se, List<String> errors) {
    validateEnum(se.hyrestyp(), Set.of("PRIVATE", "KOMMUNAL"), "Hyrestyp", errors);
    validateEnum(
        se.energyDeclarationRating(), ENERGY_RATINGS_A_G, "Energy Declaration Rating", errors);
    requireNonNegative(se.depositAmount(), "Deposit Amount", errors);
    requireRange(se.depositMonths(), 0, 6, "Deposit Months", errors);
  }

  private void validateFi(FiContractMetadata fi, List<String> errors) {
    validateEnum(
        fi.vuokrasopimustyyppi(), Set.of("FIXED", "INDEFINITE"), "Vuokrasopimustyyppi", errors);
    validateEnum(
        fi.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(fi.vakuusAmount(), "Vakuus Amount", errors);
    requireRange(fi.vakuusMonths(), 0, 3, "Vakuus Months", errors);
  }

  private void validateNo(NoContractMetadata no, List<String> errors) {
    validateEnum(
        no.husleielovType(), Set.of("RESIDENTIAL", "COMMERCIAL"), "Husleielov Type", errors);
    validateEnum(no.energyLabel(), ENERGY_RATINGS_A_G, "Energy Label", errors);
    requireNonNegative(no.depositumskontoAmount(), "Depositumskonto Amount", errors);
    requireRange(no.depositumskontoMonths(), 0, 6, "Depositumskonto Months", errors);
  }

  private void validateIe(IeContractMetadata ie, List<String> errors) {
    validateEnum(ie.tenancyType(), Set.of("PART4", "FIXED", "PERIODIC"), "Tenancy Type", errors);
    validateEnum(ie.berRating(), IE_BER_RATINGS, "BER Rating", errors);
    requireNonNegative(ie.depositAmount(), "Deposit Amount", errors);
    requireRange(ie.depositMonths(), 0, 2, "Deposit Months", errors);
    requireNonNegative(ie.marketRentAmount(), "Market Rent Amount", errors);
  }

  // --- Central & Eastern Europe ---

  private void validatePl(PlContractMetadata pl, List<String> errors) {
    validateEnum(
        pl.rodzajNajmu(),
        Set.of("OKAZJONALNY", "INSTYTUCJONALNY", "ZWYKLY"),
        "Rodzaj Najmu",
        errors);
    validateEnum(
        pl.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(pl.kaucjaAmount(), "Kaucja Amount", errors);
    requireRange(pl.kaucjaMonths(), 0, 12, "Kaucja Months", errors);
  }

  private void validateCz(CzContractMetadata cz, List<String> errors) {
    validateEnum(
        cz.najemniSmlouvaType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        cz.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(cz.kauceAmount(), "Kauce Amount", errors);
    requireRange(cz.kauceMonths(), 0, 6, "Kauce Months", errors);
    requireNonNegative(cz.sluzbyAmount(), "Sluzby Amount", errors);
  }

  private void validateHu(HuContractMetadata hu, List<String> errors) {
    validateEnum(
        hu.berletiszerzodesType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        hu.energyCertificateRating(), HU_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(hu.kaucioAmount(), "Kaucio Amount", errors);
    requireRange(hu.kaucioMonths(), 0, 3, "Kaucio Months", errors);
    requireNonNegative(hu.kozosKoltsegAmount(), "Kozos Koltseg Amount", errors);
  }

  private void validateRo(RoContractMetadata ro, List<String> errors) {
    validateEnum(ro.contractType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        ro.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(ro.garantieAmount(), "Garantie Amount", errors);
    requireRange(ro.garantieMonths(), 0, 3, "Garantie Months", errors);
    requireNonNegative(ro.intretinereAmount(), "Intretinere Amount", errors);
  }

  private void validateBg(BgContractMetadata bg, List<String> errors) {
    validateEnum(bg.contractType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        bg.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(bg.depozitAmount(), "Depozit Amount", errors);
    requireRange(bg.depozitMonths(), 0, 3, "Depozit Months", errors);
    requireNonNegative(bg.obshtiRazhodiAmount(), "Obshti Razhodi Amount", errors);
  }

  private void validateSk(SkContractMetadata sk, List<String> errors) {
    validateEnum(sk.najomnaZmluvaType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        sk.energyCertificateRating(), SK_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(sk.kauciaAmount(), "Kaucia Amount", errors);
    requireRange(sk.kauciaMonths(), 0, 3, "Kaucia Months", errors);
    requireNonNegative(sk.poplatkyAmount(), "Poplatky Amount", errors);
  }

  private void validateSi(SiContractMetadata si, List<String> errors) {
    validateEnum(
        si.najemnaPogodbaTip(),
        Set.of("TRZNO", "NEPROFITNO", "SLUZBENO"),
        "Najemna Pogodba Tip",
        errors);
    validateEnum(
        si.energyCertificateRating(), SI_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(si.varscinsAmount(), "Varscins Amount", errors);
    requireRange(si.varscinsMonths(), 0, 3, "Varscins Months", errors);
    requireNonNegative(si.rezervniFondAmount(), "Rezervni Fond Amount", errors);
  }

  private void validateHr(HrContractMetadata hr, List<String> errors) {
    validateEnum(hr.ugovorType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        hr.energyCertificateRating(), HR_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(hr.jamcevinaAmount(), "Jamcevina Amount", errors);
    requireRange(hr.jamcevinaMonths(), 0, 3, "Jamcevina Months", errors);
    requireNonNegative(hr.pricuvaAmount(), "Pricuva Amount", errors);
  }

  private void validateLt(LtContractMetadata lt, List<String> errors) {
    validateEnum(
        lt.nuomosSutartisType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        lt.energyCertificateRating(), LT_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(lt.uzstatasAmount(), "Uzstatas Amount", errors);
    requireRange(lt.uzstatasMonths(), 0, 3, "Uzstatas Months", errors);
    requireNonNegative(lt.komunaliniaiAmount(), "Komunaliniai Amount", errors);
  }

  private void validateLv(LvContractMetadata lv, List<String> errors) {
    validateEnum(lv.iresLigumsType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        lv.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(lv.drosibaNaudaAmount(), "Drosiba Nauda Amount", errors);
    requireRange(lv.drosibaNaudaMonths(), 0, 3, "Drosiba Nauda Months", errors);
    requireNonNegative(lv.komunalieAmount(), "Komunalie Amount", errors);
  }

  private void validateEe(EeContractMetadata ee, List<String> errors) {
    validateEnum(ee.uuerilepinguType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        ee.energyCertificateRating(), EE_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(ee.tagatisrahaAmount(), "Tagatisraha Amount", errors);
    requireRange(ee.tagatisrahaMonths(), 0, 3, "Tagatisraha Months", errors);
    requireNonNegative(ee.kommunaalkuludAmount(), "Kommunaalkulud Amount", errors);
  }

  // --- Mediterranean & Benelux ---

  private void validateGr(GrContractMetadata gr, List<String> errors) {
    validateEnum(
        gr.misthosisType(),
        Set.of("RESIDENTIAL", "COMMERCIAL", "PROFESSIONAL"),
        "Misthosis Type",
        errors);
    validateEnum(
        gr.energyCertificateRating(), GR_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(gr.eggysisAmount(), "Eggysis Amount", errors);
    requireRange(gr.eggysisMonths(), 0, 2, "Eggysis Months", errors);
    requireNonNegative(gr.koinochristaAmount(), "Koinochrista Amount", errors);
  }

  private void validateMt(MtContractMetadata mt, List<String> errors) {
    validateEnum(
        mt.tenancyType(), Set.of("PRIVATE", "CONTROLLED", "SHORT_LET"), "Tenancy Type", errors);
    validateEnum(mt.epcRating(), ENERGY_RATINGS_A_G, "EPC Rating", errors);
    requireNonNegative(mt.depositAmount(), "Deposit Amount", errors);
    requireRange(mt.depositMonths(), 0, 2, "Deposit Months", errors);
    requireNonNegative(mt.groundRent(), "Ground Rent", errors);
  }

  private void validateCy(CyContractMetadata cy, List<String> errors) {
    validateEnum(cy.tenancyType(), Set.of("STATUTORY", "CONTRACTUAL"), "Tenancy Type", errors);
    validateEnum(cy.epcRating(), ENERGY_RATINGS_A_G, "EPC Rating", errors);
    requireNonNegative(cy.depositAmount(), "Deposit Amount", errors);
    requireRange(cy.depositMonths(), 0, 3, "Deposit Months", errors);
    requireNonNegative(cy.commonExpensesAmount(), "Common Expenses Amount", errors);
  }

  private void validateLu(LuContractMetadata lu, List<String> errors) {
    validateEnum(lu.bailType(), Set.of("HABITATION", "COMMERCIAL", "MEUBLE"), "Bail Type", errors);
    validateEnum(
        lu.energyCertificateRating(), LU_ENERGY_RATINGS, "Energy Certificate Rating", errors);
    requireNonNegative(lu.cautionAmount(), "Caution Amount", errors);
    requireRange(lu.cautionMonths(), 0, 3, "Caution Months", errors);
    requireNonNegative(lu.chargesAmount(), "Charges Amount", errors);
  }

  // --- Balkans ---

  private void validateRs(RsContractMetadata rs, List<String> errors) {
    validateEnum(rs.ugovorType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        rs.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(rs.depozitAmount(), "Depozit Amount", errors);
    requireRange(rs.depozitMonths(), 0, 3, "Depozit Months", errors);
    requireNonNegative(rs.komunalniTroskoviAmount(), "Komunalni Troskovi Amount", errors);
  }

  private void validateBa(BaContractMetadata ba, List<String> errors) {
    validateEnum(ba.ugovorType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(ba.entityRegion(), Set.of("FBH", "RS", "BD"), "Entity/Region", errors);
    requireNonNegative(ba.depozitAmount(), "Depozit Amount", errors);
    requireRange(ba.depozitMonths(), 0, 3, "Depozit Months", errors);
    requireNonNegative(ba.rezijeAmount(), "Rezije Amount", errors);
  }

  private void validateAl(AlContractMetadata al, List<String> errors) {
    validateEnum(al.kontrataTip(), Set.of("DEFINITE", "INDEFINITE"), "Kontrata Tip", errors);
    validateEnum(
        al.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(al.garanciaAmount(), "Garancia Amount", errors);
    requireRange(al.garanciaMonths(), 0, 3, "Garancia Months", errors);
    requireNonNegative(al.shpenzimet(), "Shpenzimet", errors);
  }

  private void validateMe(MeContractMetadata me, List<String> errors) {
    validateEnum(me.ugovorType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        me.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(me.depozitAmount(), "Depozit Amount", errors);
    requireRange(me.depozitMonths(), 0, 3, "Depozit Months", errors);
    requireNonNegative(me.komunalijeAmount(), "Komunalije Amount", errors);
  }

  private void validateMk(MkContractMetadata mk, List<String> errors) {
    validateEnum(mk.dogovorType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    validateEnum(
        mk.energyCertificateRating(), ENERGY_RATINGS_A_G, "Energy Certificate Rating", errors);
    requireNonNegative(mk.depozitAmount(), "Depozit Amount", errors);
    requireRange(mk.depozitMonths(), 0, 3, "Depozit Months", errors);
    requireNonNegative(mk.rezhiskiTroskoviAmount(), "Rezhiski Troskovi Amount", errors);
  }

  private void validateXk(XkContractMetadata xk, List<String> errors) {
    validateEnum(xk.kontrataTip(), Set.of("DEFINITE", "INDEFINITE"), "Kontrata Tip", errors);
    requireNonNegative(xk.depozitAmount(), "Depozit Amount", errors);
    requireRange(xk.depozitMonths(), 0, 3, "Depozit Months", errors);
    requireNonNegative(xk.shpenzimetKomunale(), "Shpenzimet Komunale", errors);
  }

  // --- Americas ---

  private void validateCa(CaContractMetadata ca, List<String> errors) {
    validateEnum(
        ca.province(),
        Set.of("AB", "BC", "MB", "NB", "NL", "NS", "NT", "NU", "ON", "PE", "QC", "SK", "YT"),
        "Province",
        errors);
    requireNonNegative(ca.securityDepositAmount(), "Security Deposit Amount", errors);
    requireRange(ca.securityDepositMonths(), 0, 12, "Security Deposit Months", errors);
  }

  private void validateMx(MxContractMetadata mx, List<String> errors) {
    validateEnum(mx.contratoType(), Set.of("DEFINITE", "INDEFINITE"), "Contract Type", errors);
    requireNonNegative(mx.depositoAmount(), "Deposito Amount", errors);
    requireRange(mx.depositoMonths(), 0, 3, "Deposito Months", errors);
    requireNonNegative(mx.mantenimientoAmount(), "Mantenimiento Amount", errors);
  }

  private void validateBr(BrContractMetadata br, List<String> errors) {
    validateEnum(
        br.tipoLocacao(), Set.of("RESIDENCIAL", "COMERCIAL", "TEMPORADA"), "Tipo Locacao", errors);
    requireNonNegative(br.caucaoAmount(), "Caucao Amount", errors);
    requireRange(br.caucaoMonths(), 0, 3, "Caucao Months", errors);
    requireNonNegative(br.condominioAmount(), "Condominio Amount", errors);
  }

  private void validateAr(ArContractMetadata ar, List<String> errors) {
    validateEnum(ar.tipoContrato(), Set.of("HABITUAL", "TEMPORARIO"), "Tipo Contrato", errors);
    requireNonNegative(ar.depositoAmount(), "Deposito Amount", errors);
    requireRange(ar.depositoMonths(), 0, 3, "Deposito Months", errors);
    requireNonNegative(ar.expensasAmount(), "Expensas Amount", errors);
  }

  private void validateCl(ClContractMetadata cl, List<String> errors) {
    validateEnum(cl.tipoArriendo(), Set.of("HABITUAL", "TEMPORAL"), "Tipo Arriendo", errors);
    requireNonNegative(cl.garantiaAmount(), "Garantia Amount", errors);
    requireRange(cl.garantiaMonths(), 0, 3, "Garantia Months", errors);
    requireNonNegative(cl.gastosComunes(), "Gastos Comunes", errors);
  }

  private void validateCo(CoContractMetadata co, List<String> errors) {
    validateEnum(
        co.tipoContrato(), Set.of("VIVIENDA_URBANA", "COMERCIAL"), "Tipo Contrato", errors);
    requireNonNegative(co.depositoAmount(), "Deposito Amount", errors);
    requireRange(co.depositoMonths(), 0, 3, "Deposito Months", errors);
    requireNonNegative(co.administracionAmount(), "Administracion Amount", errors);
  }

  private void validatePe(PeContractMetadata pe, List<String> errors) {
    validateEnum(pe.tipoContrato(), Set.of("DEFINIDO", "INDEFINIDO"), "Tipo Contrato", errors);
    requireNonNegative(pe.garantiaAmount(), "Garantia Amount", errors);
    requireRange(pe.garantiaMonths(), 0, 3, "Garantia Months", errors);
    requireNonNegative(pe.mantenimientoAmount(), "Mantenimiento Amount", errors);
  }

  private void validateUy(UyContractMetadata uy, List<String> errors) {
    validateEnum(
        uy.garantiaType(),
        Set.of("DEPOSITO", "BHU", "PORTO", "ANDA", "CONTADURIA"),
        "Garantia Type",
        errors);
    validateEnum(uy.tipoContrato(), Set.of("HABITUAL", "TEMPORARIO"), "Tipo Contrato", errors);
    requireNonNegative(uy.depositoAmount(), "Deposito Amount", errors);
    requireRange(uy.depositoMonths(), 0, 5, "Deposito Months", errors);
    requireNonNegative(uy.gastosComunes(), "Gastos Comunes", errors);
  }

  // --- Shared helpers ---

  private static void validateEnum(
      @Nullable String value, Set<String> validValues, String fieldLabel, List<String> errors) {
    if (value != null && !validValues.contains(value)) {
      errors.add(fieldLabel + " has an invalid value: " + value);
    }
  }

  private static void requireRange(
      @Nullable Integer value, int min, int max, String fieldLabel, List<String> errors) {
    if (value != null && (value < min || value > max)) {
      errors.add(fieldLabel + " must be between " + min + " and " + max);
    }
  }

  private static void requireNonNegative(
      @Nullable MoneyAmount value, String fieldLabel, List<String> errors) {
    if (value != null && value.value().compareTo(BigDecimal.ZERO) < 0) {
      errors.add(fieldLabel + " must be >= 0");
    }
  }

  private static void requireNonNegativeDecimal(
      @Nullable BigDecimal value, String fieldLabel, List<String> errors) {
    if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
      errors.add(fieldLabel + " must be >= 0");
    }
  }

  private static void requireNonNegativeInt(
      @Nullable Integer value, String fieldLabel, List<String> errors) {
    if (value != null && value < 0) {
      errors.add(fieldLabel + " must be >= 0");
    }
  }

  private static void requireMaxDecimal(
      @Nullable BigDecimal value, BigDecimal max, String fieldLabel, List<String> errors) {
    if (value != null && value.compareTo(max) > 0) {
      errors.add(fieldLabel + " must be <= " + max);
    }
  }
}
