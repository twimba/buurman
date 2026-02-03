package com.buurman.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamSettings {

    private PaymentSettings payments;

    public TeamSettings() {
        this.payments = new PaymentSettings();
    }

    public PaymentSettings getPayments() {
        return payments;
    }

    public void setPayments(PaymentSettings payments) {
        this.payments = payments;
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
}
