package com.example.exchange.assets;

import com.example.exchange.common.enums.AssetEnum;
import com.example.exchange.common.support.LoggerSupport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AssetService extends LoggerSupport {


    // UserId -> Map(AssetEnum -> Assets[available/frozen])
    //定义资产结构
    final ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> userAssets = new ConcurrentHashMap<>();


    public Map<AssetEnum, Asset> getAssets(Long userId) {
        Map<AssetEnum, Asset> assets = userAssets.get(userId);
        if (assets == null) {
            return Map.of();
        }
        return assets;
    }

    public ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> getUserAssets() {
        return userAssets;
    }

    /**
     * 转账操作（存入资产不用校验余额）
     *
     * @param type
     * @param fromUser
     * @param toUser
     * @param assetId
     * @param amount
     * @param checkBalance
     * @return
     */
    public boolean tryTransfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount, boolean checkBalance) {

        if (amount.signum() == 0) {
            return true;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Negative amount");
        }
        // 获取来源用户资产
        Asset fromAsset = getAsset(fromUser, assetId);
        if (fromAsset == null) {
            fromAsset = initAssets(fromUser, assetId);
        }
        //获取目标用户资产
        Asset toAsset = getAsset(toUser, assetId);
        if (toAsset == null) {
            toAsset = initAssets(toUser, assetId);
        }
        return switch (type) {
            case AVAILABLE_TO_AVAILABLE -> {
                //需要检查余额且余额不足
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                //交易
                fromAsset.available = fromAsset.getAvailable().subtract(amount);
                toAsset.available = toAsset.getAvailable().add(amount);
                yield true;
            }
            case AVAILABLE_TO_FROZEN -> {
                // 需要检查余额且余额不足:
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                //交易
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.frozen = toAsset.frozen.add(amount);
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                // 需要检查余额且余额不足:
                if (checkBalance && fromAsset.frozen.compareTo(amount) < 0) {
                    yield false;
                }
                //交易
                fromAsset.frozen = fromAsset.frozen.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            default -> throw new IllegalArgumentException("invalid type: " + type);
        };
    }

    /**
     * 资产不存在时初始化用户资产
     */
    private Asset initAssets(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> map = userAssets.get(userId);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            userAssets.put(userId, map);
        }
        Asset zeroAsset = new Asset();
        map.put(assetId, zeroAsset);
        return zeroAsset;
    }

    /**
     * 获取目标用户资产
     */
    private Asset getAsset(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> assets = userAssets.get(userId);
        if (assets == null) {
            return null;
        }
        return assets.get(assetId);
    }

    /**
     * 常规转账操作（需检验余额）
     */
    public void transfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount) {
        if (!tryTransfer(type, fromUser, toUser, assetId, amount, true)) {
            throw new RuntimeException("Trans failed for " + type + ",from user " + fromUser + " to user " + toUser
                    + ", asset = " + assetId + ", amount = " + amount);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("transfer asset {}, from {} => {}, amount {}", assetId, fromUser, toUser, amount);
        }
    }

    /**
     * 冻结资产
     */
    public boolean tryfreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        return tryTransfer(Transfer.AVAILABLE_TO_FROZEN, userId, userId, assetId, amount, true);
    }

    /**
     * 解冻资产
     */
    public boolean unfreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        return tryTransfer(Transfer.FROZEN_TO_AVAILABLE, userId, userId, assetId, amount, true);
    }

    public void debug() {
        System.out.println("---------- assets ----------");
        List<Long> userIds = new ArrayList<>(userAssets.keySet());
        Collections.sort(userIds);
        for (Long userId : userIds) {
            System.out.println("  user " + userId + " ----------");
            Map<AssetEnum, Asset> assets = userAssets.get(userId);
            List<AssetEnum> assetIds = new ArrayList<>(assets.keySet());
            Collections.sort(assetIds);
            for (AssetEnum assetId : assetIds) {
                System.out.println("    " + assetId + ": " + assets.get(assetId));
            }
        }
        System.out.println("---------- // assets ----------");
    }

}
