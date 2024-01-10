package com.example.exchange.tradingapi.web.api;

import com.example.exchange.bean.TransferRequestBean;
import com.example.exchange.enums.UserType;
import com.example.exchange.message.event.TransferEvent;
import com.example.exchange.support.AbstractApiController;
import com.example.exchange.tradingapi.service.SendEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class TradingInternalApiController extends AbstractApiController {
    @Autowired
    SendEventService sendEventService;

    @PostMapping("/transfer")
    public Map<String, Boolean> transfer(@RequestBody TransferRequestBean transferRequest) {
        logger.info("transfer request: transferId={}, fromUserId={}, toUserId={}, asset={}, amount={}", transferRequest.transferId, transferRequest.fromUserId, transferRequest.toUserId, transferRequest.asset,
                transferRequest.amount);
        transferRequest.validate();

        var message = new TransferEvent();
        // IMPORTANT: set uniqueId to make sure the message will be sequenced only once:
        message.uniqueId = transferRequest.transferId;
        message.fromUserId = transferRequest.fromUserId;
        message.toUserId = transferRequest.toUserId;
        message.asset = transferRequest.asset;
        message.amount = transferRequest.amount;
        message.sufficient = transferRequest.fromUserId.longValue() != UserType.DEBT.getInternalUserId();
        this.sendEventService.sendMessage(message);
        logger.info("transfer event sent: {}", message);
        return Map.of("result", Boolean.TRUE);
    }

}
