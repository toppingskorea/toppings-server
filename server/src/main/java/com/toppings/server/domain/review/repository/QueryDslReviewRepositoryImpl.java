package com.toppings.server.domain.review.repository;

import static com.toppings.server.domain.review.entity.QReview.*;
import static com.toppings.server.domain.review.entity.QReviewAttach.*;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.toppings.server.domain.review.dto.ReviewAttachResponse;
import com.toppings.server.domain.review.dto.ReviewListResponse;
import com.toppings.server.domain.review.dto.ReviewResponse;
import com.toppings.server.domain.review.entity.ReviewAttach;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryDslReviewRepositoryImpl implements QueryDslReviewRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<ReviewListResponse> findReviewByRestaurantId(
		Long restaurantId,
		Long userId
	) {
		return queryFactory.select(
			Projections.fields(ReviewListResponse.class, review.id, review.description, review.thumbnail,
				review.updateDate.as("modifiedAt"), review.user.name, review.user.country,
				getIsMine(userId).as("isMine")
			))
			.from(review)
			.innerJoin(review.user)
			.where(review.restaurant.id.eq(restaurantId))
			.orderBy(review.updateDate.desc())
			.fetch();
	}

	private BooleanExpression getIsMine(Long userId) {
		return userId != null ? review.user.id.eq(userId) : Expressions.asBoolean(false);
	}
}
