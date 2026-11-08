package com.mecanicadm.mecanicadm_finance_api.core.payment.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MercadoPagoOrder {

    private String id;
    private String type;
    private String processingMode;
    private String externalReference;
    private String totalAmount;
    private String totalPaidAmount;
    private String createdDate;
    private String lastUpdatedDate;
    private String countryCode;
    private String status;
    private String statusDetail;
    private String captureMode;
    private String description;
    private Transactions transactions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProcessingMode() {
        return processingMode;
    }

    public void setProcessingMode(String processingMode) {
        this.processingMode = processingMode;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(String totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(String statusDetail) {
        this.statusDetail = statusDetail;
    }

    public String getCaptureMode() {
        return captureMode;
    }

    public void setCaptureMode(String captureMode) {
        this.captureMode = captureMode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Transactions getTransactions() {
        return transactions;
    }

    public void setTransactions(Transactions transactions) {
        this.transactions = transactions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Transactions {

        private List<Payment> payments;

        public List<Payment> getPayments() {
            return payments;
        }

        public void setPayments(List<Payment> payments) {
            this.payments = payments;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Payment {

        private String id;
        private String amount;
        private String paidAmount;
        private String taxesAmount;
        private String referenceId;
        private String status;
        private String statusDetail;
        private String expirationTime;
        private PaymentMethod paymentMethod;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getPaidAmount() {
            return paidAmount;
        }

        public void setPaidAmount(String paidAmount) {
            this.paidAmount = paidAmount;
        }

        public String getTaxesAmount() {
            return taxesAmount;
        }

        public void setTaxesAmount(String taxesAmount) {
            this.taxesAmount = taxesAmount;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public void setReferenceId(String referenceId) {
            this.referenceId = referenceId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusDetail() {
            return statusDetail;
        }

        public void setStatusDetail(String statusDetail) {
            this.statusDetail = statusDetail;
        }

        public String getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(String expirationTime) {
            this.expirationTime = expirationTime;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class PaymentMethod {

        private String id;
        private String type;
        private String token;
        private Integer installments;
        private String installmentAmount;
        private String statementDescriptor;
        private String ticketUrl;
        private String barcodeContent;
        private String reference;
        private String verificationCode;
        private String financialInstitution;
        private String digitableLine;
        private String qrCode;
        private String qrCodeBase64;
        private String e2eId;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Integer getInstallments() {
            return installments;
        }

        public void setInstallments(Integer installments) {
            this.installments = installments;
        }

        public String getInstallmentAmount() {
            return installmentAmount;
        }

        public void setInstallmentAmount(String installmentAmount) {
            this.installmentAmount = installmentAmount;
        }

        public String getStatementDescriptor() {
            return statementDescriptor;
        }

        public void setStatementDescriptor(String statementDescriptor) {
            this.statementDescriptor = statementDescriptor;
        }

        public String getTicketUrl() {
            return ticketUrl;
        }

        public void setTicketUrl(String ticketUrl) {
            this.ticketUrl = ticketUrl;
        }

        public String getBarcodeContent() {
            return barcodeContent;
        }

        public void setBarcodeContent(String barcodeContent) {
            this.barcodeContent = barcodeContent;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public String getVerificationCode() {
            return verificationCode;
        }

        public void setVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
        }

        public String getFinancialInstitution() {
            return financialInstitution;
        }

        public void setFinancialInstitution(String financialInstitution) {
            this.financialInstitution = financialInstitution;
        }

        public String getDigitableLine() {
            return digitableLine;
        }

        public void setDigitableLine(String digitableLine) {
            this.digitableLine = digitableLine;
        }

        public String getQrCode() {
            return qrCode;
        }

        public void setQrCode(String qrCode) {
            this.qrCode = qrCode;
        }

        public String getQrCodeBase64() {
            return qrCodeBase64;
        }

        public void setQrCodeBase64(String qrCodeBase64) {
            this.qrCodeBase64 = qrCodeBase64;
        }

        public String getE2eId() {
            return e2eId;
        }

        public void setE2eId(String e2eId) {
            this.e2eId = e2eId;
        }
    }
}
