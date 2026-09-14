package com.broadband.install.model;

/** 派单拦截/异常（超容未派、需改派或增派）。 */
public class DispatchException {
    public String workOrderId;
    public String timeSlot;
    public String reason;

    public DispatchException(String workOrderId, String timeSlot, String reason) {
        this.workOrderId = workOrderId;
        this.timeSlot = timeSlot;
        this.reason = reason;
    }
}
