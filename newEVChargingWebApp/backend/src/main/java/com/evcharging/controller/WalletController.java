package com.evcharging.controller;

import com.evcharging.dto.AmountRequest;
import com.evcharging.dto.TopupFailureRequest;
import com.evcharging.dto.TopupVerifyRequest;
import com.evcharging.dto.WalletResponse;
import com.evcharging.model.WalletTransaction;
import com.evcharging.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Every endpoint works on the logged-in user's own wallet; a userId from the client is never used.
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse getWallet() {
        return walletService.getWallet();
    }

    @GetMapping("/transactions")
    public List<WalletTransaction> transactions() {
        return walletService.getTransactions();
    }

    /**
     * DEV ONLY - returns 403 unless WALLET_DEMO_TOPUP=true.
     */
    @PostMapping("/add")
    public WalletResponse addMoney(@Valid @RequestBody AmountRequest request) {
        return walletService.demoTopup(request.amount());
    }

    @PostMapping("/topup/create-order")
    public Map<String, Object> createTopupOrder(@Valid @RequestBody AmountRequest request) {
        return walletService.createTopupOrder(request.amount());
    }

    @PostMapping("/topup/verify")
    public WalletResponse verifyTopup(@Valid @RequestBody TopupVerifyRequest request) {
        return walletService.verifyTopup(request);
    }

    @PostMapping("/topup/fail")
    public void failTopup(@Valid @RequestBody TopupFailureRequest request) {
        walletService.verifyTopupFailure(request);
    }
}
