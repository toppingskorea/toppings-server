package com.toppings.server.domain.restaurant.dto;

import com.toppings.server.domain.restaurant.entity.Restaurant;
import com.toppings.server.domain.restaurant.entity.RestaurantAttach;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantAttachRequest {

	private String name;

	public static RestaurantAttach dtoToEntity(String name, Restaurant restaurant) {
		return RestaurantAttach.builder()
			.image(name)
			.restaurant(restaurant)
			.build();
	}
}
