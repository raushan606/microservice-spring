package com.raushan.orderservice.service;

import com.raushan.orderservice.dto.InventoryResponse;
import com.raushan.orderservice.dto.OrderLineItemsDto;
import com.raushan.orderservice.dto.OrderRequest;
import com.raushan.orderservice.model.Order;
import com.raushan.orderservice.model.OrderLineItems;
import com.raushan.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;


    public void placeOrder(OrderRequest orderRequest) {

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID()
                .toString());

        List<OrderLineItems> objects = orderRequest.getLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(objects);

        List<String> skuCodes = order.getOrderLineItemsList()
                .stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        // Call Inventory Service and place order if product is in stock
        InventoryResponse[] inventoryResponses = webClientBuilder.build()
                .get()
                .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes)
                        .build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        assert inventoryResponses != null;
        Boolean isInStock = Arrays.stream(inventoryResponses)
                .allMatch(InventoryResponse::getIsInStock);

        if (Boolean.TRUE.equals(isInStock) && inventoryResponses.length == skuCodes.size()) {
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is out of Stock, Please Try again!!");
        }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        return orderLineItems;
    }
}
