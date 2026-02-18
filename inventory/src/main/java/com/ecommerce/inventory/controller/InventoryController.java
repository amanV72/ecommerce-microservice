package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.StockRequest;
import com.ecommerce.inventory.services.InventoryService;
import jakarta.ws.rs.GET;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    ///Admin endpoint
    @PostMapping("/{productId}/restock")
    public ResponseEntity<Void> addStock(
            @PathVariable Long productId,
            @RequestBody StockRequest request
            ){
        if (request.getQuantity() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        inventoryService.addStock(productId,request.getQuantity());
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{productId}/availability")
    public ResponseEntity<Boolean> checkInventory(
            @PathVariable Long productId,
            @RequestParam("qty")Integer quantity
            ){
        boolean isAvailable=inventoryService.hasSufficientStockForRest(productId,quantity);

        return ResponseEntity.ok(isAvailable);
    }

}
