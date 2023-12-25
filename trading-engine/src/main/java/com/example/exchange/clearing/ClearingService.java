package com.example.exchange.clearing;

import com.example.exchange.assets.AssetService;
import com.example.exchange.assets.Transfer;
import com.example.exchange.common.enums.AssetEnum;
import com.example.exchange.common.model.trade.OrderEntity;
import com.example.exchange.match.MatchDetailRecord;
import com.example.exchange.match.MatchResult;
import com.example.exchange.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class  ClearingService {

    final
    AssetService assetService;
    final
    OrderService orderService;

    public ClearingService(AssetService assetService, OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    public void clearMatchResult(MatchResult result) {
        OrderEntity taker = result.takerOrder;

        switch (taker.getDirection()) {
            case BUY -> {
                // 买入时，按Maker的价格成交：
                for (MatchDetailRecord detail : result.matchDetails) {
                    if (log.isDebugEnabled()) {
                        log.debug("clear buy matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                                detail.price(), detail.quantity(), detail.takerOrder().getId(), detail.makerOrder().getId(),
                                detail.takerOrder().userId, detail.makerOrder().userId);
                    }
                    OrderEntity maker = detail.makerOrder(); // 挂单
                    BigDecimal matched = detail.quantity(); //成交数量

                    if (taker.price.compareTo(maker.price) > 0) {
                        // 实际买入价比报价低，部分USD退回账户:
                        BigDecimal unfreezeQuote = taker.price.subtract(maker.price).multiply(matched);
                        log.debug("unfree extra unused quote {} back to taker user {}", unfreezeQuote, taker.userId);
                        assetService.unfreeze(taker.userId, AssetEnum.USD, unfreezeQuote);
                    }

                    // 买方USD转入卖方账户:
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.USD, maker.price.multiply(matched));
                    // 卖方BTC转入买方账户:
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.BTC, matched);
                    // 删除完全成交的Maker:
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.getId());
                    }
                }
                // 删除完全成交的Taker:
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.getId());
                }
            }
            case SELL -> {
                for (MatchDetailRecord detail : result.matchDetails) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "clear sell matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                                detail.price(), detail.quantity(), detail.takerOrder().getId(), detail.makerOrder().getId(),
                                detail.takerOrder().userId, detail.makerOrder().userId);
                    }
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matched = detail.quantity();
                    // 卖方BTC转入买方账户:
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.BTC, matched);
                    // 买方USD转入卖方账户:
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.USD, maker.price.multiply(matched));
                    // 删除完全成交的Maker:
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.getId());
                    }
                }
                // 删除完全成交的Taker:
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.getId());
                }
            }
            default -> throw new IllegalArgumentException("InValid direction.");
        }
    }

    /**
     * 用户取消订单，则取消清算，解冻资产
     * @param order
     */
    public void clearCancelOrder(OrderEntity order) {
        switch (order.direction) {
            case BUY -> {
                // 解冻USD = 价格 x 未成交数量
                assetService.unfreeze(order.userId, AssetEnum.USD, order.price.multiply(order.unfilledQuantity));
            }
            case SELL -> {
                // 解冻BTC = 未成交数量
                assetService.unfreeze(order.userId, AssetEnum.BTC, order.unfilledQuantity);
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
        // 从OrderService中删除订单:
        orderService.removeOrder(order.getId());
    }
}
