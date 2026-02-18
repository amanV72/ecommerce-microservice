package com.ecommerce.inventory.services;

import com.ecommerce.inventory.dto.eventDto.InventoryFailedEvent;
import com.ecommerce.inventory.dto.eventDto.OrderCreatedEventDto;
import com.ecommerce.inventory.dto.OrderItemDTO;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.InventoryStatus;
import com.ecommerce.inventory.repository.InventoryRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepo inventoryRepo;
    private final StreamBridge streamBridge;


    public boolean hasSufficientStockForRest(Long productId, Integer quantity) {
        return inventoryRepo.findById(productId)
                .map(inventory -> quantity<=(inventory.getTotalQuantity()-inventory.getReservedQuantity()))
                .orElse(false);
    }

    public void hasSufficientStockForEvent(OrderCreatedEventDto order) {
        /// higher time complexity O(N)
//        return orderItems.stream().allMatch(item->
//            inventoryRepo.findById(item.getProductId())
//                    .map(inventory -> item.getQuantity()<=(inventory.getTotalQuantity()- inventory.getReservedQuantity()))
//                    .orElse(false)
//        );

        /// efficient
        List<Long> productIds= order.getItems().stream().map(OrderItemDTO::getProductId).toList();

        List<Inventory> inventories= inventoryRepo.findAllById(productIds);

        Map<Long,Integer> inventoryMap= inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId,Inventory::getTotalQuantity));

        boolean isAvailable= order.getItems().stream().allMatch(item->
                inventoryMap.containsKey(item.getProductId()) &&
                item.getQuantity() <= inventoryMap.get(item.getProductId())
                );

        if(isAvailable){
            log.info("Stock is available");

        }else{
            streamBridge.send("inventoryFailed-out-0",new InventoryFailedEvent(
                    order.getOrderId(),
                    order.getItems(),
                    "INSUFFICIENT STOCKS"
            ));
        }
    }

    public void initializeInventory(Long productId) {
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
