package com.jieyou.photography.module.order.state;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态机纯 Java 单元测试（不依赖数据库）：覆盖 API 约定 4.2 的完整状态转移表。
 */
class OrderStateMachineTest {

    private static final Map<String, OrderAction> LEGAL = Map.of(
            "PENDING->CONFIRMED", OrderAction.CONFIRM,
            "PENDING->REJECTED", OrderAction.REJECT,
            "PENDING->CANCELLED", OrderAction.CANCEL,
            "CONFIRMED->SHOOTING_DONE", OrderAction.SHOOT_DONE,
            "SHOOTING_DONE->UPLOADED", OrderAction.UPLOAD,
            "UPLOADED->DELIVERED", OrderAction.DELIVER,
            "UPLOADED->MODIFYING", OrderAction.MODIFY,
            "MODIFYING->UPLOADED", OrderAction.UPLOAD);

    @Test
    void legalTransitionsMapToExpectedActions() {
        LEGAL.forEach((key, expectedAction) -> {
            String[] parts = key.split("->");
            OrderStatus from = OrderStatus.valueOf(parts[0]);
            OrderStatus to = OrderStatus.valueOf(parts[1]);
            assertEquals(expectedAction, OrderStateMachine.actionFor(from, to), key);
            assertTrue(OrderStateMachine.canTransition(from, to), key);
        });
    }

    @Test
    void allOtherTransitionsAreIllegal() {
        int legalCount = 0;
        for (OrderStatus from : OrderStatus.values()) {
            for (OrderStatus to : OrderStatus.values()) {
                String key = from.name() + "->" + to.name();
                OrderAction expected = LEGAL.get(key);
                if (expected == null) {
                    assertNull(OrderStateMachine.actionFor(from, to), "非法流转不应通过: " + key);
                    assertFalse(OrderStateMachine.canTransition(from, to), key);
                } else {
                    legalCount++;
                    assertEquals(expected, OrderStateMachine.actionFor(from, to), key);
                }
            }
        }
        assertEquals(LEGAL.size(), legalCount, "8 种状态共 8 条合法流转");
    }

    @Test
    void terminalStatesHaveNoOutgoingTransitions() {
        assertTrue(OrderStatus.REJECTED.isTerminal());
        assertTrue(OrderStatus.CANCELLED.isTerminal());
        assertTrue(OrderStatus.DELIVERED.isTerminal());
        assertFalse(OrderStatus.PENDING.isTerminal());
        assertFalse(OrderStatus.MODIFYING.isTerminal());

        assertTrue(OrderStateMachine.transitionsFrom(OrderStatus.REJECTED).isEmpty());
        assertTrue(OrderStateMachine.transitionsFrom(OrderStatus.CANCELLED).isEmpty());
        assertTrue(OrderStateMachine.transitionsFrom(OrderStatus.DELIVERED).isEmpty());
    }

    @Test
    void transitionsFromPENDINGAreCorrect() {
        assertEquals(Set.of(OrderStatus.CONFIRMED, OrderStatus.REJECTED, OrderStatus.CANCELLED),
                OrderStateMachine.transitionsFrom(OrderStatus.PENDING).keySet());
        assertEquals(Map.of(OrderStatus.UPLOADED, OrderAction.UPLOAD),
                OrderStateMachine.transitionsFrom(OrderStatus.MODIFYING));
    }

    @Test
    void nullStatusIsRejected() {
        assertNull(OrderStateMachine.actionFor(null, OrderStatus.PENDING));
        assertNull(OrderStateMachine.actionFor(OrderStatus.PENDING, null));
    }
}
