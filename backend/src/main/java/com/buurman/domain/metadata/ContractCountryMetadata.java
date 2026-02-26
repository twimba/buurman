package com.buurman.domain.metadata;

/**
 * Sealed interface for country-specific contract metadata. Each supported country has a dedicated
 * record with country-specific fields.
 */
public sealed interface ContractCountryMetadata
    permits
        // Tier 1 — Western Europe
        NlContractMetadata,
        DeContractMetadata,
        FrContractMetadata,
        BeContractMetadata,
        PtContractMetadata,
        EsContractMetadata,
        ItContractMetadata,
        UkContractMetadata,
        UsContractMetadata,
        // DACH + Nordics + Ireland
        AtContractMetadata,
        ChContractMetadata,
        DkContractMetadata,
        SeContractMetadata,
        FiContractMetadata,
        NoContractMetadata,
        IeContractMetadata,
        // Central & Eastern Europe
        PlContractMetadata,
        CzContractMetadata,
        HuContractMetadata,
        RoContractMetadata,
        BgContractMetadata,
        SkContractMetadata,
        SiContractMetadata,
        HrContractMetadata,
        LtContractMetadata,
        LvContractMetadata,
        EeContractMetadata,
        // Mediterranean & Benelux
        GrContractMetadata,
        MtContractMetadata,
        CyContractMetadata,
        LuContractMetadata,
        // Balkans
        RsContractMetadata,
        BaContractMetadata,
        AlContractMetadata,
        MeContractMetadata,
        MkContractMetadata,
        XkContractMetadata,
        // Americas
        CaContractMetadata,
        MxContractMetadata,
        BrContractMetadata,
        ArContractMetadata,
        ClContractMetadata,
        CoContractMetadata,
        PeContractMetadata,
        UyContractMetadata,
        // Fallback
        GenericContractMetadata {}
