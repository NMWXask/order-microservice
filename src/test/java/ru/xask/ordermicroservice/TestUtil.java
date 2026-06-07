package ru.xask.ordermicroservice;

import ru.xask.ordermicroservice.dto.OrderDto;
import ru.xask.ordermicroservice.dto.OrderItemDto;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.entity.OrderItem;

import java.util.ArrayList;
import java.util.List;


public class TestUtil {

    public static OrderDto createOrderDto() {
        return OrderDto.builder()
                .id(null)
                .customerName("Test Customer")
                .status("NEW")
                .items(List.of(
                        createOrderItemDto(),
                        createOrderItemDto("Mouse", 2, 25.50)
                ))
                .build();
    }


    public static OrderItemDto createOrderItemDto() {
        return createOrderItemDto("Laptop", 1, 999.99);
    }

    public static OrderItemDto createOrderItemDto(String productName, int quantity, double price) {
        return OrderItemDto.builder()
                .id(null)
                .productName(productName)
                .category("Electronics")
                .price(price)
                .quantity(quantity)
                .order(null) // связь установится позже
                .build();
    }


    public static Order createOrderEntity() {
        Order order = new Order();
        order.setId(null);
        order.setCustomerName("Test Customer Entity");
        order.setStatus("NEW");
        order.setItems(new ArrayList<>());
        return order;
    }

    public static Order createOrderEntityWithItems(int itemCount) {
        Order order = createOrderEntity();
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(createOrderItemEntity("Product " + (i + 1), i + 1, 10.0 * (i + 1)));
        }
        order.setItems(items);
        return order;
    }


    public static OrderItem createOrderItemEntity(String productName, int quantity, double price) {
        OrderItem item = new OrderItem();
        item.setId(null);
        item.setProductName(productName);
        item.setCategory("Electronics");
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setOrder(null);
        return item;
    }

    public static Order createSavedOrder(Long id, List<OrderItem> items) {
        Order saved = new Order();
        saved.setId(id);
        saved.setCustomerName("Saved Customer");
        saved.setStatus("NEW");
        saved.setItems(items != null ? new ArrayList<>(items) : new ArrayList<>());
        if (items != null) {
            for (OrderItem item : items) {
                item.setOrder(saved);
            }
        }
        return saved;
    }
}