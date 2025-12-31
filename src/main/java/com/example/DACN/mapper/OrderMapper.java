package com.example.DACN.mapper;

import com.example.DACN.dto.response.CreateOrderResponse;
import com.example.DACN.dto.response.CustomerOrderResponse;
import com.example.DACN.dto.response.OrderStatusHistoryResponse;
import com.example.DACN.dto.response.SellerOrderResponse;
import com.example.DACN.entity.Order;
import com.example.DACN.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "orderId", target = "orderId")
    @Mapping(target = "paymentUrl", ignore = true)
    CreateOrderResponse toCreateOrderResponse(Order order);

    @Mapping(source = "orderId", target = "orderId")
    @Mapping(source = "shop.shopId", target = "shopId")
    @Mapping(source = "shop.shopName", target = "shopName")
    @Mapping(target = "currentStatus", ignore = true)
    @Mapping(target = "itemCount", ignore = true)
    CustomerOrderResponse toCustomerOrderResponse(Order order);

    @Mapping(source = "orderId", target = "orderId")
    @Mapping(source = "user.email", target = "customerEmail")
    @Mapping(source = "user.fullName", target = "customerName")
    @Mapping(target = "currentStatus", ignore = true)
    @Mapping(target = "itemCount", ignore = true)
    SellerOrderResponse toSellerOrderResponse(Order order);

    OrderStatusHistoryResponse toOrderStatusHistoryResponse(OrderStatusHistory orderStatusHistory);
}
