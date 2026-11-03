package com.mecanicadm.mecanicadm_api.core.workorder.domain;

import java.util.Objects;

public class WorkOrderBudgetVehicle {

    private String licensePlate;
    private String model;
    private String brand;
    private Short modelYear;

    public WorkOrderBudgetVehicle() {
    }

    private WorkOrderBudgetVehicle(String licensePlate, String model, String brand, Short modelYear) {
        this.licensePlate = licensePlate;
        this.model = model;
        this.brand = brand;
        this.modelYear = modelYear;
    }

    public static WorkOrderBudgetVehicle create(String licensePlate, String model, String brand, Short modelYear) {
        return new WorkOrderBudgetVehicle(licensePlate, model, brand, modelYear);
    }

    public static WorkOrderBudgetVehicle restore(String licensePlate, String model, String brand, Short modelYear) {
        return new WorkOrderBudgetVehicle(licensePlate, model, brand, modelYear);
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Short getModelYear() {
        return modelYear;
    }

    public void setModelYear(Short modelYear) {
        this.modelYear = modelYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderBudgetVehicle that = (WorkOrderBudgetVehicle) o;
        return Objects.equals(licensePlate, that.licensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licensePlate);
    }
}
