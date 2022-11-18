package com.toppings.server.domain.review.repository;

import java.util.List;

import com.toppings.server.domain.review.dto.ReviewListResponse;
import com.toppings.server.domain.review.dto.ReviewResponse;

public interface QueryDslReviewRepository {

	List<ReviewListResponse> findReviewByRestaurantId(
		Long restaurantId,
		Long userId
	);
}
