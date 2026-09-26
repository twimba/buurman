# Tenancy-rule candidates — unverified leads (2026-09-26)

**These are unverified candidates, not catalog data.** Each line below was written by an
adversarial verifier during the 2026-09-26 rent-regulation audit, after that verifier had loaded
an official government source. The audit's second-pass cross-check workflow — the step that would
normally confirm or refute a lead against the official body and a second, independent official
source before anything reaches `rent-regulations.json` — died on a session limit with **zero**
verifiers finished. None of these leads were cross-checked. None of them are in
`docs/rent-regulations/2026-09-26-audit.md`, which only records changes that a completed
cross-check approved.

**Before any entry here may be seeded into `tenancyRules`:**
1. Re-fetch the cited source (or find the current official version if the URL is stale) and
   confirm the fact still holds.
2. Cross-check it against a second official source independent of the first.
3. Only then add it to `rent-regulations.json` via the normal `update-rent-regulations` skill flow
   (Step 3 → `tenancyRules`), with its own freshly-verified `sourceUrl`.

Each item below quotes the lead's own wording rather than paraphrasing it into something more
confident than the source supports. Where a lead gives no `sourceUrl` of its own, that is noted
rather than invented.

---

## AT — `TENANCY_DURATION`

- **Proposed label/value:** "Minimum fixed term" / "5 years (raised from 3)"
- **Caveat the lead itself carries:** the raise to 5 years has a carve-out — it stays 3 years
  "sofern der Vermieter zum Zeitpunkt der Befristung kein Unternehmer im Sinn des
  Konsumentenschutzgesetzes ... war" (i.e. 3 years still applies where the landlord is not a
  business/trader under the Consumer Protection Act). A faithful entry cannot drop this exception.
- **Proposed effectiveFrom:** 2026-01-01 (leads state "applies to leases concluded or renewed
  after 31 Dec 2025")
- **Legal basis:** MRG § 29 Abs 1 Z 3 lit b, Abs 3 lit b and Abs 4, as amended by 5. MILG
  (BGBl I 114/2025) Art 2 Z 3–5; commencement via MRG § 49k Abs 4.
- **Source URLs given by the leads:**
  - https://www.parlament.gv.at/dokument/XXVIII/BNR/111/fname_1729608.pdf (Lead AT-L1)
  - https://ogd.ris.bka.gv.at/Dokumente/BgblAuth/BGBLA_2025_I_114/BGBLA_2025_I_114.html (Lead AT-L9,
    the official gazette copy)
- **Lead's own wording (AT-L1):** "MRG § 29 Befristungsdauer raised 3 → 5 years: 5. MILG Art 2 Z
  3–5 replaces 'mindestens drei Jahre' with 'mindestens fünf Jahre oder, sofern der Vermieter zum
  Zeitpunkt der Befristung kein Unternehmer im Sinn des Konsumentenschutzgesetzes ... war,
  mindestens drei Jahre' in § 29 Abs 1 Z 3 lit b, Abs 3 lit b and Abs 4; per § 49k Abs 4 it applies
  to leases concluded or renewed after 31 Dec 2025."

## DK — `FEES_AND_PENALTIES`

- **Proposed label/value:** "Reminder fee (påkravsgebyr)" / "DKK 344"
- **Proposed effectiveFrom:** 2027-01-01
- **Legal basis:** lejeloven § 182, stk. 2 (and almenlejeloven § 90, stk. 2); VEJ nr 9959 af
  08/09/2026, pt. 1.10. The 2026 figure it replaces (DKK 335) is VEJ nr 9886 af 04/09/2025, pt.
  1.10.
- **Source URL given by the lead:** https://www.retsinformation.dk/eli/retsinfo/2026/9959
- **Lead's own wording (DK-L8):** "2027 påkravsgebyr is DKK 344, not 335: VEJ nr 9959 af
  08/09/2026, pt. 1.10 'Gebyr for påkrav ved for sen betaling (lejelovens § 182, stk. 2): 344 kr.'
  (and almenlejelovens § 90, stk. 2: 344 kr.)." Corroborated by DK-L1: "the lejelovens § 182, stk.
  2 påkravsgebyr rises from DKK 335 (2026) to DKK 344 with effect 1 January 2027."

## IE — `FEES_AND_PENALTIES`

- **Proposed label/value:** "Fixed payment notice (rent-setting breach)" / "EUR 200 within 28 days
  (prescribable up to EUR 1,000)"
- **Proposed effectiveFrom:** the leads give two different dates for two different aspects of this
  and do not resolve which governs the amount above — recorded as a caveat, not resolved here:
  - Lead IE-L1 dates the Act itself: "Housing and Residential Tenancies (Miscellaneous Provisions)
    Act 2026 (No. 33 of 2026), enacted 22 July 2026."
  - Lead IE-L15 dates a *different* RTB-published fee schedule to 14 Sep 2026: "New enforcement
    figures effective 14 Sep 2026 ... fixed payment notices of €200 for rent breaches and €100 for
    all other listed breaches, across six breach types."
- **Legal basis:** Housing and Residential Tenancies (Miscellaneous Provisions) Act 2026 (No. 33 of
  2026), fixed payment notice section.
- **Source URLs given by the leads:**
  - https://www.irishstatutebook.ie/eli/2026/act/33/enacted/en/print.html (Lead IE-L2, statutory
    amount and prescribable ceiling)
  - https://rtb.ie/about/news/important-changes-to-rental-law-from-14-september-2026/ (Lead
    IE-L15, the 14-Sep-2026 figures and the six breach types, including "Setting rent above the
    legal limit")
- **Lead's own wording (IE-L2):** "Fixed payment notices: the statutory amount is 200 euro payable
  within 28 days, 'or such greater amount not exceeding 1,000 euro as may be prescribed' — the
  researcher captured only the RTB's current 200/100 euro figures and missed the prescribable
  ceiling."

## LU — `LEASE_FORM`

- **Proposed label/value:** "Written lease" / "Mandatory, sous peine de nullité, with eight
  mandatory clauses"
- **Proposed effectiveFrom:** 2024-08-01 ("in force since 01/08/2024", per the lead)
- **Legal basis:** Art. 5(1), loi modifiée du 21 septembre 2006 sur le bail à usage d'habitation
  (texte coordonné du 1er août 2024).
- **Source URL:** **none given by this lead.** LU-L17 cites only "Art. 5(1)" with no URL. Other
  leads in the same file cite the coordinated-text PDF for this same law at
  https://logement.public.lu/dam-assets/documents/legislation/lois/bl-loi-modifiee-du-21-09-2006.pdf
  (e.g. Lead LU-L3) — plausibly the same document — but LU-L17 itself does not say so, so this URL
  is noted as a likely-same-source guess, not something the lead asserted.
- **Lead's own wording (LU-L17):** "Art. 5(1), in force since 01/08/2024: a written lease is
  mandatory 'sous peine de nullité' with eight mandatory clauses (including the furniture
  supplement and notice of the commission des loyers). The proposals mention the deposit and
  agency-fee changes but not this, although it is the change most likely to affect a
  lease-generating product."

## US — `REGISTRATION` (regionCode `DC`)

- **Proposed label/value:** "DC RentRegistry filing" / "Mandatory"
- **Proposed effectiveFrom:** 2025-12-01 ("All rent increases taking effect on or after December
  1, 2025 must be filed electronically")
- **Legal basis:** not a statute citation in the lead itself — sourced to the RHC's 2026
  certification notice (the lead does not name a D.C. Code section for the filing mandate).
- **Source URL given by the lead:**
  https://rhc.dc.gov/sites/default/files/dc/sites/rhc/page_content/attachments/2026%20RHC%20CPI-COLA-MFI%20Notice.pdf
- **Lead's own wording (US-L2):** "DC filing/notice framework change not captured on any DC row:
  the 2026 RHC Certification and Notice states the RentRegistry portal
  (https://rentregistry.dc.gov/) is now live and its use is mandatory by law, and that 'All rent
  increases taking effect on or after December 1, 2025 must be filed electronically by the housing
  provider'; tenants claiming the age/disability exemption must also file there."

## CZ — `OTHER`

- **Proposed label/value:** "Housing-support framework" / "zákon č. 175/2025 Sb."
- **Proposed effectiveFrom:** 2026-01-01 ("Účinnost od 01.01.2026"; the act's Platnost/enactment
  date is 16.06.2025)
- **Legal basis:** zákon č. 175/2025 Sb. (zákon o podpoře bydlení), with companion act
  176/2025 Sb.
- **Source URLs given by the lead:** https://www.zakonyprolidi.cz/cs/2025-175 and
  https://www.zakonyprolidi.cz/cs/2025-176
- **Lead's own wording (CZ-L2):** "Zákon č. 175/2025 Sb., zákon o podpoře bydlení — the first
  national housing-support framework, Platnost od 16.06.2025, Účinnost od 01.01.2026 ... It
  creates housing counselling, a register of flats and of 'potřební', supportive measures,
  contributions and an information system, and it took effect inside the year the 2026 row
  covers. The CZ file does not mention it anywhere." Note this does **not** cap private rents
  (confirmed by the related Lead CZ-L9: "It does not cap private rents, so it does not alter
  §2249") — consistent with `OTHER` rather than a rent-cap topic.

## FI — `NOTICE_PERIOD`

- **Proposed label/value:** "Increase takes effect" / "From the start of the rent period falling
  at least one month after notice; no retroactive charging"
- **Proposed effectiveFrom:** 2026-10-01
- **Important caveat the leads carry:** this is a **new** rule replacing the current § 27, which
  the leads say has **no** statutory minimum notice period at all (Lead FI-L5: "The currently
  in-force § 27 imposes NO minimum period"). It also does **not** apply to leases concluded before
  2026-10-01 (Lead FI-L4/FI-L14 transitional rule) — those stay under the old, no-minimum § 27
  "jollei tämän lain tultua voimaan toisin sovita" (unless otherwise agreed after the new law takes
  effect).
- **Legal basis:** AHVL 481/1995 § 27(2), as amended by laki asuinhuoneiston vuokrauksesta
  muuttamisesta 531/2026 (in force 1.10.2026).
- **Source URL given by the lead:**
  https://opendata.finlex.fi/finlex/avoindata/v1/akn/fi/act/statute/2026/531
- **Lead's own wording (FI-L2):** "NEW free-market notice rule from 1.10.2026 — new § 27(2):
  'Korotettu vuokra tulee voimaan aikaisintaan kuukauden kuluttua ilmoituksen tekemistä lähinnä
  seuraavan vuokranmaksukauden alusta, eikä sitä saa periä takautuvasti.' i.e. one month from the
  start of the next rent-payment period, no retroactive charging, written notice must state the
  increase, the new rent and the effective date."

---

## Leads that were too vague / incomplete to record faithfully here

None of the 7 leads mapped to the brief's original candidates were too vague to record — each had
a specific value, date and source. Two cross-cutting gaps are worth flagging for whoever runs the
follow-up verification pass, rather than folding into the entries above:

- **IE** has two candidate dates (22 Jul 2026 statute enactment vs. 14 Sep 2026 RTB-published
  figures) for what may or may not be the same fee schedule — the leads don't resolve which
  `effectiveFrom` is correct, so this needs the verification pass to settle, not a guess here.
- **LU** has no `sourceUrl` in the lead itself for the written-lease requirement; the plausible
  document is inferred from a sibling lead about the same law, not asserted by LU-L17.

Both `missed/*.md` scratch files also contain many other leads (10–17 per country) beyond the 7
captured here — accounting changes, index values, structural gaps, etc. Those are out of scope for
this document (they mostly concern `rules[]`, not `tenancyRules`) and were not reviewed for
persistence here.
