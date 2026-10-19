package com.jieyou.photography.module.order.state;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 订单状态机，依据 API 约定 4.2/第 6 章。
 * 纯 Java 实现，不依赖数据库与 Spring，便于单元测试覆盖完整状态转移表。
 */
public final class OrderStateMachine {

    private static final Map<OrderStatus, Map<OrderStatus, OrderAction>> TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {
        register(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderAction.CONFIRM);
        register(OrderStatus.PENDING, OrderStatus.REJECTED, OrderAction.REJECT);
        register(OrderStatus.PENDING, OrderStatus.CANCELLED, OrderAction.CANCEL);
        register(OrderStatus.CONFIRMED, OrderStatus.SHOOTING_DONE, OrderAction.SHOOT_DONE);
        register(OrderStatus.SHOOTING_DONE, OrderStatus.UPLOADED, OrderAction.UPLOAD);
        register(OrderStatus.UPLOADED, OrderStatus.DELIVERED, OrderAction.DELIVER);
        register(OrderStatus.UPLOADED, OrderStatus.MODIFYING, OrderAction.MODIFY);
        register(OrderStatus.MODIFYING, OrderStatus.UPLOADED, OrderAction.UPLOAD);
    }

    private OrderStateMachine() {
    }

    private static void register(OrderStatus from, OrderStatus to, OrderAction action) {
        TRANSITIONS.computeIfAbsent(from, key -> new EnumMap<>(OrderStatus.class)).put(to, action);
    }

    /**
     * 返回合法流转对应的动作；非法流转返回 null。
     */
    public static OrderAction actionFor(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return null;
        }
        Map<OrderStatus, OrderAction> targets = TRANSITIONS.get(from);
        return targets == null ? null : targets.get(to);
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return actionFor(from, to) != null;
    }

    /**
     * 某一状态的合法后继状态与动作（只读）。
     */
    public static Map<OrderStatus, OrderAction> transitionsFrom(OrderStatus from) {
        Map<OrderStatus, OrderAction> targets = TRANSITIONS.get(from);
        return targets == null ? Map.of() : Collections.unmodifiableMap(targets);
    }
}
