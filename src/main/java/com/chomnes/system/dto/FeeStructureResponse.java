package com.chomnes.system.dto;

import com.chomnes.system.model.FeeStructure;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeStructureResponse(
    Integer feeId,
    Integer classId,
    Integer semesterId,
    String feeName,
    BigDecimal amount,
    LocalDate dueDate
) {
    public static FeeStructureResponse from(FeeStructure feeStructure) {
        return new FeeStructureResponse(
            feeStructure.getFeeId(),
            feeStructure.getClassId(),
            feeStructure.getSemesterId(),
            feeStructure.getFeeName(),
            feeStructure.getAmount(),
            feeStructure.getDueDate()
        );
    }
}
