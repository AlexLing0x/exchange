package com.example.exchange.order;

import com.example.exchange.assets.AssetService;
import com.example.exchange.enums.AssetEnum;
import com.example.exchange.enums.Direction;
import com.example.exchange.model.trade.OrderEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OrderService {

    final
    AssetService assetService;

    public OrderService(AssetService assetService) {
        this.assetService = assetService;
    }

    //所有活动订单
    // Order ID => OrderEntity
    final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();
    //用户活动订单
    //User ID => Map(Order ID => OrderEntity)
    final ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    /**
     * 创建订单
     *
     * @param sequenceId
     * @param ts
     * @param orderId
     * @param userId
     * @param direction
     * @param price
     * @param quantity
     * @return
     */
    public OrderEntity createOrder(long sequenceId, long ts, Long orderId, Long userId, Direction direction, BigDecimal price, BigDecimal quantity) {

        switch (direction) {
            case BUY -> {
                //买入，冻结USD
                if (!assetService.tryfreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL -> {
                // 卖出，冻结BTC
                if (!assetService.tryfreeze(userId, AssetEnum.BTC, quantity)) {
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setSequenceId(sequenceId);
        order.userId = userId;
        order.direction = direction;
        order.price = price;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createdAt = order.updatedAt = ts;

        this.activeOrders.put(order.getId(), order);
        //从用户活动订单中取出某个用户订单
        ConcurrentMap<Long, OrderEntity> uOrders = this.userOrders.get(order.getUserId());
        if (uOrders == null) {
            uOrders = new ConcurrentHashMap<>();
        }
        uOrders.put(order.getId(), order);
        this.userOrders.put(order.getUserId(), uOrders);
        return order;
    }

    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return this.activeOrders;
    }

    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }

    public ConcurrentMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    /**
     * 删除活动订单
     *
     * @param orderId
     */
    public void removeOrder(Long orderId) {
        OrderEntity removedOrder = this.activeOrders.remove(orderId);
        if (removedOrder == null) {
            throw new IllegalArgumentException("Order not found by orderId in activeOrders : " + orderId);
        }
        ConcurrentMap<Long, OrderEntity> uOrders = userOrders.get(removedOrder.getUserId());
        if (uOrders == null) {
            throw new IllegalArgumentException("User order not found by userId : " + removedOrder.getUserId());
        }
        if (uOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders : " + orderId);
        }
    }


    public void debug() {
        System.out.println("---------- orders ----------");
        List<OrderEntity> orders = new ArrayList<>(this.activeOrders.values());
        Collections.sort(orders);
        for (OrderEntity order : orders) {
            System.out.println("  " + order.id + " " + order.direction + " price: " + order.price + " unfilled: "
                    + order.unfilledQuantity + " quantity: " + order.quantity + " sequenceId: " + order.sequenceId
                    + " userId: " + order.userId);
        }
        System.out.println("---------- // orders ----------");
    }

}
