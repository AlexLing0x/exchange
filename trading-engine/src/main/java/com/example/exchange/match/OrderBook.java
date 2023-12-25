package com.example.exchange.match;

import com.example.exchange.common.bean.OrderBookItemBean;
import com.example.exchange.common.enums.Direction;
import com.example.exchange.common.model.trade.OrderEntity;

import java.util.*;

public class OrderBook {
    public final Direction direction; //方向
    public final TreeMap<OrderKey, OrderEntity> book; // 排序树


    public OrderBook(Direction direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
    }

    public OrderEntity getFirst() {
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }

    public boolean remove(OrderEntity order) {
        return this.book.remove(new OrderKey(order.getSequenceId(), order.getPrice())) != null;
    }

    public boolean add(OrderEntity order) {
        return this.book.put(new OrderKey(order.getSequenceId(), order.getPrice()), order) != null;
    }

    public boolean exist(OrderEntity order) {
        return this.book.containsKey(new OrderKey(order.getSequenceId(), order.getPrice()));
    }

    public int size() {
        return this.book.size();
    }

    /**
     * @param maxDepth
     * @return
     */
    public List<OrderBookItemBean> getOrderBook(int maxDepth) {
        List<OrderBookItemBean> items = new ArrayList<>(maxDepth);
        OrderBookItemBean prevItem = null;
        for (OrderKey key : this.book.keySet()) {
            OrderEntity order = this.book.get(key);
            if (prevItem == null) {
                prevItem = new OrderBookItemBean(order.getPrice(), order.getUnfilledQuantity());
                items.add(prevItem);
            } else {
                if (order.getPrice().compareTo(prevItem.getPrice()) == 0) {
                    prevItem.addQuantity(order.getUnfilledQuantity());
                } else {
                    if (items.size() >= maxDepth) {
                        break;
                    }
                    prevItem = new OrderBookItemBean(order.getPrice(), order.getUnfilledQuantity());
                    items.add(prevItem);
                }
            }
        }
        return items;
    }


    @Override
    public String toString() {
        if (this.book.isEmpty()) {
            return "(empty)";
        }
        List<String> orders = new ArrayList<>(10);
        for (Map.Entry<OrderKey, OrderEntity> entry : this.book.entrySet()) {
            OrderEntity order = entry.getValue();
            orders.add("  " + order.price + " " + order.unfilledQuantity + " " + order.toString());
        }
        if (direction == Direction.SELL) {
            Collections.reverse(orders);
        }
        return String.join("\n", orders);
    }

    private static final Comparator<OrderKey> SORT_SELL = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格低在前:
            int cmp = o1.price().compareTo(o2.price());
            // 时间早在前:
            return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
        }
    };

    private static final Comparator<OrderKey> SORT_BUY = (o1, o2) -> {
        // 价格高在前:
        int cmp = o2.price().compareTo(o1.price());
        // 时间早在前:
        return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
    };
}
