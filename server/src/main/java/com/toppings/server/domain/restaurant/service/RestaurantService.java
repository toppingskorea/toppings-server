package com.toppings.server.domain.restaurant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toppings.common.constants.ResponseCode;
import com.toppings.common.exception.GeneralException;
import com.toppings.server.domain.restaurant.dto.RestaurantAttachRequest;
import com.toppings.server.domain.restaurant.dto.RestaurantModifyRequest;
import com.toppings.server.domain.restaurant.dto.RestaurantRequest;
import com.toppings.server.domain.restaurant.dto.RestaurantResponse;
import com.toppings.server.domain.restaurant.entity.Restaurant;
import com.toppings.server.domain.restaurant.entity.RestaurantAttach;
import com.toppings.server.domain.restaurant.repository.RestaurantAttachRepository;
import com.toppings.server.domain.restaurant.repository.RestaurantRepository;
import com.toppings.server.domain.user.constant.Auth;
import com.toppings.server.domain.user.entity.User;
import com.toppings.server.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantService {

	private final RestaurantRepository restaurantRepository;

	private final UserRepository userRepository;

	private final RestaurantAttachRepository restaurantAttachRepository;

	/**
	 * 음식점 등록하기
	 */
	@Transactional
	public RestaurantResponse register(
		RestaurantRequest request,
		Long userId
	) {
		// TODO: user 만들 때 그냥 빌더로 만들지 고민 + 맨 처음 이미지가 목록 이미지라서 따로 저장할까 고민
		User user = getUserById(userId);
		Restaurant restaurant = restaurantRepository.findRestaurantByCode(request.getCode()).orElse(null);
		if (restaurant != null)
			throw new GeneralException(ResponseCode.DUPLICATED_ITEM);

		Restaurant saveRestaurant = restaurantRepository.save(RestaurantRequest.dtoToEntity(request, user));
		List<String> images = registerRestaurantAttach(request, saveRestaurant);

		RestaurantResponse restaurantResponse = RestaurantResponse.entityToDto(saveRestaurant);
		restaurantResponse.setImages(images);
		return restaurantResponse;
	}

	private List<String> registerRestaurantAttach(
		RestaurantRequest request,
		Restaurant restaurant
	) {
		List<RestaurantAttach> restaurantAttaches = new ArrayList<>();
		for (String image : request.getImages())
			restaurantAttaches.add(RestaurantAttachRequest.dtoToEntity(image, restaurant));
		restaurantAttachRepository.saveAll(restaurantAttaches);

		return restaurantAttaches.stream()
			.map(RestaurantAttach::getImage)
			.collect(Collectors.toList());
	}

	private User getUserById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new GeneralException(ResponseCode.BAD_REQUEST));
	}

	/**
	 * 음식점 수정하기
	 */
	@Transactional
	public RestaurantResponse modify(
		RestaurantModifyRequest request,
		Long restaurantId,
		Long userId
	) {
		Restaurant restaurant = getRestaurantById(restaurantId);
		if (verifyRestaurantAndUser(userId, restaurant))
			throw new GeneralException(ResponseCode.BAD_REQUEST);

		List<String> images = modifyRestaurantAttach(request, restaurant);
		RestaurantResponse restaurantResponse = RestaurantResponse.entityToDto(restaurant);
		restaurantResponse.setImages(images);

		RestaurantModifyRequest.setRestaurantInfo(request, restaurant, images.get(0));
		RestaurantModifyRequest.setMapInfo(request, restaurant);
		return restaurantResponse;
	}

	private List<String> modifyRestaurantAttach(
		RestaurantModifyRequest request,
		Restaurant restaurant
	) {
		List<RestaurantAttach> restaurantAttaches = new ArrayList<>();
		if (request.getImages() != null && !request.getImages().isEmpty()) {
			// 기존 이미지 제거
			restaurantAttachRepository.deleteAllByIdInBatch(
				restaurant.getImages().stream().map(RestaurantAttach::getId).collect(Collectors.toList()));

			// 신규 이미지 등록
			for (String image : request.getImages())
				restaurantAttaches.add(RestaurantAttachRequest.dtoToEntity(image, restaurant));
			restaurantAttachRepository.saveAll(restaurantAttaches);
		} else {
			throw new GeneralException(ResponseCode.BAD_REQUEST);
		}

		return restaurantAttaches.stream()
			.map(RestaurantAttach::getImage)
			.collect(Collectors.toList());
	}

	private boolean verifyRestaurantAndUser(
		User user,
		Restaurant restaurant
	) {
		return !user.getRole().equals(Auth.ROLE_ADMIN) && !restaurant.getUser().getId().equals(user.getId());
	}

	private boolean verifyRestaurantAndUser(
		Long userId,
		Restaurant restaurant
	) {
		return !restaurant.getUser().getId().equals(userId);
	}

	private Restaurant getRestaurantById(Long id) {
		return restaurantRepository.findById(id)
			.orElseThrow(() -> new GeneralException(ResponseCode.BAD_REQUEST));
	}

	/**
	 * 음식점 삭제하기
	 */
	@Transactional
	public Long remove(
		Long restaurantId,
		Long userId
	) {
		Restaurant restaurant = getRestaurantById(restaurantId);
		User user = getUserById(userId);
		if (verifyRestaurantAndUser(user, restaurant))
			throw new GeneralException(ResponseCode.BAD_REQUEST);

		restaurantRepository.deleteById(restaurantId);
		return restaurantId;
	}

	public Object findAll() {

		return null;
	}

	public RestaurantResponse findOne(Long restaurantId) {
		Restaurant restaurant = getRestaurantById(restaurantId);
		// TODO : 식습관 / 국적별 좋아요 퍼센트 작업 추가
		return null;
	}
}
