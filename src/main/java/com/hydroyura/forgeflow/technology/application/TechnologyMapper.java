package com.hydroyura.forgeflow.technology.application;

import com.hydroyura.forgeflow.technology.api.OperationView;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessView;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import java.util.List;

/**
 * Маппер технологического процесса в публичное представление.
 */
public class TechnologyMapper {

    public TechnologyProcessView toView(TechnologyProcess process) {
        List<OperationView> operations = process.operations().stream()
                .map(operation -> new OperationView(operation.number(), operation.name()))
                .toList();
        return new TechnologyProcessView(
                process.getId(),
                process.getNumber(),
                process.getName(),
                process.getPartId(),
                process.isDefaultProcess(),
                process.getCreatedAt(),
                process.getUpdatedAt(),
                operations);
    }
}
