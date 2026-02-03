package com.buurman.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamSettings {

    private PaymentSettings payments;
    private RegionalSettings regional;

    public TeamSettings() {
        this.payments = new PaymentSettings();
        this.regional = new RegionalSettings();
    }

    public PaymentSettings getPayments() {
        return payments;
    }

    public void setPayments(PaymentSettings payments) {
        this.payments = payments;
    }

    public RegionalSettings getRegional() {
        return regional;
    }

    public void setRegional(RegionalSettings regional) {
        this.regional = regional;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentSettings {
        private Integer paymentsAheadCount = 3;
        private Boolean autoGenerationEnabled = true;

        public PaymentSettings() {
        }

        public Integer getPaymentsAheadCount() {
            return paymentsAheadCount;
        }

        public void setPaymentsAheadCount(Integer paymentsAheadCount) {
            this.paymentsAheadCount = paymentsAheadCount;
        }

        public Boolean getAutoGenerationEnabled() {
            return autoGenerationEnabled;
        }

        public void setAutoGenerationEnabled(Boolean autoGenerationEnabled) {
            this.autoGenerationEnabled = autoGenerationEnabled;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RegionalSettings {
        private String defaultCurrency = "EUR";
        private String defaultCountry = "Netherlands";
        private String timezone = "Europe/Amsterdam";
        private String dateFormat = "DD/MM/YYYY";
        private String fiscalYearStartMonth = "01";

        public RegionalSettings() {
        }

        public String getDefaultCurrency() {
            return defaultCurrency;
        }

        public void setDefaultCurrency(String defaultCurrency) {
            this.defaultCurrency = defaultCurrency;
        }

        public String getDefaultCountry() {
            return defaultCountry;
        }

        public void setDefaultCountry(String defaultCountry) {
            this.defaultCountry = defaultCountry;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public String getFiscalYearStartMonth() {
            return fiscalYearStartMonth;
        }

        public void setFiscalYearStartMonth(String fiscalYearStartMonth) {
            this.fiscalYearStartMonth = fiscalYearStartMonth;
        }
    }
}
