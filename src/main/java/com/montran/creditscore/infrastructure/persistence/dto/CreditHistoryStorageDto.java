package com.montran.creditscore.infrastructure.persistence.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @docs Data Transfer Object representing a historical transaction entry for
 *       file-based persistence schemes.
 *       <p>
 *       <b>Design Justification:</b> Decouples serialization structures from
 *       the
 *       immutable Core Domain models. Features mutable attributes and a
 *       zero-argument
 *       constructor to meet the reflection demands of JAXB and Gson libraries.
 *       </p>
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

    @XmlElement(name = "amount")
    private double amount;

    @XmlElement(name = "status")
    private String status;

    @XmlElement(name = "dueDate")
    private String dueDateStr;

    @XmlElement(name = "settlementDate")
    private String settlementDateStr;

    @XmlElement(name = "transactionId")
    private String transactionId;

    /**
     * @docs Default zero-argument constructor required for reflection-based data
     *       serialization.
     */

    public CreditHistoryStorageDto() {
    }

    // getters and setters

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
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
