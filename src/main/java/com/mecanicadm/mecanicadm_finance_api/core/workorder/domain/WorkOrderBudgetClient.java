package com.mecanicadm.mecanicadm_finance_api.core.workorder.domain;

import java.util.Objects;
import java.util.UUID;

public class WorkOrderBudgetClient {

    private UUID id;
    private String name;
    private String email;
    private String document;
    private String phone;

    public WorkOrderBudgetClient() {
    }

    private WorkOrderBudgetClient(UUID id, String name, String email, String document, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.document = document;
        this.phone = phone;
    }

    public static WorkOrderBudgetClient create(String name, String email, String document, String phone) {
        return new WorkOrderBudgetClient(UUID.randomUUID(), name, email, document, phone);
    }

    public static WorkOrderBudgetClient restore(UUID id, String name, String email, String document, String phone) {
        return new WorkOrderBudgetClient(id == null ? UUID.randomUUID() : id, name, email, document, phone);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderBudgetClient that = (WorkOrderBudgetClient) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
