package ru.xask.ordermicroservice.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.xask.ordermicroservice.exception.IllegalOrderStatusTransitionException;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class OrderStatusStrategyImpl implements OrderStatusStrategy {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "NEW", Set.of("PROCESSING", "CANCELLED"),   // ← разрешены PROCESSING и CANCELLED
            "PROCESSING", Set.of("SHIPPED", "CANCELLED"),
            "SHIPPED", Set.of("DELIVERED"),
            "DELIVERED", Set.of(),
            "CANCELLED", Set.of()
    );

    @Override
    public void validateTransition(String current, String newStatus) {
        String currentUpper = current.toUpperCase();
        String newUpper = newStatus.toUpperCase();

        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentUpper, Set.of());
        if (!allowed.contains(newUpper)) {
            throw new IllegalOrderStatusTransitionException(
                    String.format("Недопустимый переход из '%s' в '%s'. Разрешённые переходы из '%s': %s",
                            currentUpper, newUpper, currentUpper, allowed)
            );
        }
        log.info("Переход из {} в {} разрешён", currentUpper, newUpper);
    }
}
