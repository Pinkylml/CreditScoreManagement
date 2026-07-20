package com.montran.creditscore.infrastructure.persistence.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Flat, mutable representation of a {@link com.montran.creditscore.domain.model.CreditHistoryRecord}
 * used exclusively for file serialization. Stores dates as plain strings (yyyy-MM-dd)
 * because JAXB and Gson cannot natively handle {@code java.util.Date}.
 * The {@code amount} field is stored as a plain {@link String} so that the full decimal
 * precision of a {@link java.math.BigDecimal} is preserved round-trip (JAXB/Gson would
 * silently lose scale if the field were typed as {@code double}).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CreditHistoryRecord", propOrder = {
        "transactionId",
        "dueDateStr",
        "settlementDateStr",
        "transactionType",
        "amount",
        "status"
})
public class CreditHistoryStorageDto {

    @XmlElement(name = "type")
    private String transactionType;

    /** Stored as a plain string to preserve {@link java.math.BigDecimal} precision without loss. */
    @XmlElement(name = "amount")
    private String amount;

    @XmlElement(name = "status")
    private String status;

    @XmlElement(name = "dueDate")
    private String dueDateStr;

    @XmlElement(name = "settlementDate")
    private String settlementDateStr;

    @XmlElement(name = "transactionId")
    private String transactionId;

    public CreditHistoryStorageDto() {
    }

    // getters and setters

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDueDateStr() {
        return dueDateStr;
    }

    public void setDueDateStr(String dueDateStr) {
        this.dueDateStr = dueDateStr;
    }

    public String getSettlementDateStr() {
        return settlementDateStr;
    }

    public void setSettlementDateStr(String settlementDateStr) {
        this.settlementDateStr = settlementDateStr;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
