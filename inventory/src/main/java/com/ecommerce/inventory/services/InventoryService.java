package com.ecommerce.inventory.services;

import com.ecommerce.inventory.dto.StockRequest;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.InventoryStatus;
import com.ecommerce.inventory.repository.InventoryRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepo inventoryRepo;


    public boolean hasSufficientStock(Long productId, Integer quantity) {
        return inventoryRepo.findById(productId)
                .map(inv-> quantity<=inv.getTotalQuantity())
                .orElse(false);
    }

    public void createInventory(Long productId) {
        Inventory newInventory= new Inventory();
        newInventory.setProductId(productId);
        newInventory.setTotalQuantity(0);
        newInventory.setReservedQuantity(0);
        newInventory.setStatus(InventoryStatus.OUT_OF_STOCK);

        inventoryRepo.save(newInventory);
    }

    @Transactional
    public void addStock(Long productId, int quantity){
       Inventory inventory= inventoryRepo.findById(productId).orElseThrow(
               ()-> new RuntimeException("Inventory Not Found")
       );

       inventory.setTotalQuantity(inventory.getTotalQuantity()+quantity);

        int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();

        if (available <= 0) {
            inventory.setStatus(InventoryStatus.OUT_OF_STOCK);
        } else if (available < 10) {
            inventory.setStatus(InventoryStatus.LOW_STOCK);
        } else {
            inventory.setStatus(InventoryStatus.IN_STOCK);
        }

       inventoryRepo.save(inventory);
    }
}
