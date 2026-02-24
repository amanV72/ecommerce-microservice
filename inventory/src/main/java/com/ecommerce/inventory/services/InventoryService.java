package com.ecommerce.inventory.services;

import com.ecommerce.inventory.dto.eventDto.*;
import com.ecommerce.inventory.dto.OrderItemDTO;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.InventoryReservation;
import com.ecommerce.inventory.model.InventoryStatus;
import com.ecommerce.inventory.repository.InventoryRepo;
import com.ecommerce.inventory.repository.InventoryReservationRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepo inventoryRepo;
    private final InventoryReservationRepo reservationRepo;
    private final StreamBridge streamBridge;


    public boolean hasSufficientStockForRest(Long productId, Integer quantity) {
        return inventoryRepo.findById(productId)
                .map(inventory -> quantity <= (inventory.getTotalQuantity() - inventory.getReservedQuantity()))
                .orElse(false);
    }

    @Transactional
    public void hasSufficientStockForEvent(OrderCreatedEventDto order) {
        /// higher time complexity O(N)
//        return orderItems.stream().allMatch(item->
//            inventoryRepo.findById(item.getProductId())
//                    .map(inventory -> item.getQuantity()<=(inventory.getTotalQuantity()- inventory.getReservedQuantity()))
//                    .orElse(false)
//        );

        /// efficient
        List<Long> productIds = order.getItems().stream().map(OrderItemDTO::getProductId).toList();

        List<Inventory> inventories = inventoryRepo.findAllById(productIds);

        Map<Long, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, inv -> inv));


        boolean isAvailable = order.getItems().stream().allMatch(item -> {
                    Inventory inv = inventoryMap.get(item.getProductId());

                    return inv != null &&
                            item.getQuantity() <=
                                    (inv.getTotalQuantity() - inv.getReservedQuantity());

                }
        );

        if (!isAvailable) {
            //log.info("Stock is available");
            streamBridge.send("inventoryFailed-out-0", new InventoryFailedEvent(
                    order.getOrderId(),
                    order.getItems(),
                    "INSUFFICIENT STOCKS"
            ));
            return;
        }
        List<InventoryReservation> reservationList = new ArrayList<>();
        for (OrderItemDTO dto : order.getItems()) {
            Inventory inventory = inventoryMap.get(dto.getProductId());

            inventory.setReservedQuantity(
                    inventory.getReservedQuantity() + dto.getQuantity()
            );

            InventoryReservation reservation = new InventoryReservation();
            reservation.setProductId(dto.getProductId());
            reservation.setQuantity(dto.getQuantity());
            reservation.setOrderId(order.getOrderId());
            reservationList.add(reservation);
        }
        inventoryRepo.saveAll(inventories);
        reservationRepo.saveAll(reservationList);

        streamBridge.send("inventoryReserved-out-0", new InventoryReservedEvent(
                order.getOrderId(),
                order.getUserId(),
                order.getTotalAmount()
        ));
    }

    public void initializeInventory(Long productId) {
        Inventory newInventory = new Inventory();
        newInventory.setProductId(productId);
        newInventory.setTotalQuantity(0);
        newInventory.setReservedQuantity(0);
        newInventory.setStatus(InventoryStatus.OUT_OF_STOCK);

        inventoryRepo.save(newInventory);
    }

    @Transactional
    public void addStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepo.findById(productId).orElseThrow(
                () -> new RuntimeException("Inventory Not Found")
        );

        inventory.setTotalQuantity(inventory.getTotalQuantity() + quantity);

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

    @Transactional
    public void revertInventory(PaymentFailedEvent event) {
        List<InventoryReservation> reservations = reservationRepo.findByOrderId(event.getOrderId()).orElseThrow();
        if (reservations.isEmpty()) return;
        List<Long> productIds = reservations.stream().map(InventoryReservation::getProductId).toList();


        List<Inventory> inventories = inventoryRepo.findAllById(productIds);
        Map<Long, Inventory> quantityMap = inventories.stream().collect(Collectors.toMap(Inventory::getProductId, inv -> inv));

        for (InventoryReservation reserved : reservations) {
            Inventory inventory = quantityMap.get(reserved.getProductId());

            if (inventory != null) {
                inventory.setReservedQuantity(
                        inventory.getReservedQuantity() - reserved.getQuantity()
                );

            }

        }
        reservationRepo.deleteAll(reservations);
        streamBridge.send("inventoryReverted-out-0",new InventoryRevertedEvent(
                event.getOrderId(),
                event.getUserId(),
                "INVENTORY RESERVED"
        ));


    }
}
