package com.toppings.server.domain.restaurant.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toppings.common.constants.ResponseCode;
import com.toppings.common.dto.PubRequest;
import com.toppings.common.exception.GeneralException;
import com.toppings.server.domain.likes.dto.LikesPercent;
import com.toppings.server.domain.likes.dto.LikesPercentResponse;
import com.toppings.server.domain.likes.repository.LikeRepository;
import com.toppings.server.domain.notification.constant.AlarmType;
import com.toppings.server.domain.notification.dto.AlarmRequest;
import com.toppings.server.domain.notification.service.AlarmService;
import com.toppings.server.domain.restaurant.dto.RestaurantFilterSearchRequest;
import com.toppings.server.domain.restaurant.dto.RestaurantListResponse;
import com.toppings.server.domain.restaurant.dto.RestaurantMapSearchRequest;
import com.toppings.server.domain.restaurant.dto.RestaurantModifyRequest;
import com.toppings.server.domain.restaurant.dto.RestaurantRequest;
import com.toppings.server.domain.restaurant.dto.RestaurantResponse;
import com.toppings.server.domain.restaurant.entity.Restaurant;
import com.toppings.server.domain.restaurant.entity.RestaurantAttach;
import com.toppings.server.domain.restaurant.repository.RestaurantAttachRepository;
import com.toppings.server.domain.restaurant.repository.RestaurantRepository;
import com.toppings.server.domain.review.repository.ReviewRepository;
import com.toppings.server.domain.scrap.repository.ScrapRepository;
import com.toppings.server.domain.user.constant.Auth;
import com.toppings.server.domain.user.entity.User;
import com.toppings.server.domain.user.repository.UserHabitRepository;
import com.toppings.server.domain.user.repository.UserRepository;
import com.toppings.server.domain_global.utils.s3.S3Response;
import com.toppings.server.domain_global.utils.s3.S3Uploader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantService {

	private final RestaurantRepository restaurantRepository;

	private final UserRepository userRepository;

	private final RestaurantAttachRepository restaurantAttachRepository;

	private final LikeRepository likeRepository;

	private final UserHabitRepository userHabitRepository;

	private final ScrapRepository scrapRepository;

	private final ReviewRepository reviewRepository;

	private final AlarmService alarmService;

	private final S3Uploader s3Uploader;

	private final String imagePath = "restaurant/";

	/**
	 * ????????? ????????????
	 */
	@Transactional
	public Long register(
		RestaurantRequest request,
		Long userId
	) {
		final User user = getUserById(userId);
		final Restaurant restaurant = restaurantRepository.findRestaurantByCode(request.getCode()).orElse(null);
		if (restaurant != null)
			throw new GeneralException(ResponseCode.DUPLICATED_ITEM);

		final Restaurant saveRestaurant = RestaurantRequest.dtoToEntity(request, user);
		final List<RestaurantAttach> images = getRestaurantAttaches(request.getImages(), request.getCode(),
			saveRestaurant);

		saveRestaurant.updateThumbnail(images.get(0).getImage());
		restaurantRepository.save(saveRestaurant);
		restaurantAttachRepository.saveAll(images);
		return saveRestaurant.getId();
	}

	private List<RestaurantAttach> getRestaurantAttaches(
		List<String> base64Images,
		String restaurantCode,
		Restaurant restaurant
	) {
		final List<RestaurantAttach> images = new ArrayList<>();
		for (String image : base64Images) {
			byte[] decodedFile = DatatypeConverter.parseBase64Binary(image.substring(image.indexOf(",") + 1));
			S3Response s3Response = s3Uploader.uploadBase64(decodedFile, imagePath + restaurantCode + "/");
			images.add(RestaurantAttach.of(s3Response, restaurant));
		}
		return images;
	}

	private User getUserById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new GeneralException(ResponseCode.BAD_REQUEST));
	}

	/**
	 * ????????? ????????????
	 */
	@Transactional
	public Long modify(
		RestaurantModifyRequest request,
		Long restaurantId,
		Long userId
	) {
		final Restaurant restaurant = getRestaurantById(restaurantId);
		if (verifyRestaurantAndUser(userId, restaurant))
			throw new GeneralException(ResponseCode.BAD_REQUEST);

		final List<RestaurantAttach> images = modifyRestaurantAttach(request, restaurant);

		restaurant.updateRestaurantInfo(request, images.get(0).getImage());
		restaurant.updateMapInfo(request);
		return restaurant.getId();
	}

	private List<RestaurantAttach> modifyRestaurantAttach(
		RestaurantModifyRequest request,
		Restaurant restaurant
	) {
		if (isNotNullImage(request.getImages())) {
			// ?????? ????????? ??????
			restaurantAttachRepository.deleteAllByIdInBatch(
				restaurant.getImages().stream().map(RestaurantAttach::getId).collect(Collectors.toList()));

			// ?????? ????????? ??????
			final List<RestaurantAttach> images
				= getRestaurantAttaches(request.getImages(), request.getCode(), restaurant);
			restaurantAttachRepository.saveAll(images);
			return images;
		} else {
			throw new GeneralException(ResponseCode.BAD_REQUEST);
		}
	}

	private boolean isNotNullImage(List<String> images) {
		return images != null && !images.isEmpty();
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
		return restaurantRepository.findRestaurantByIdAndPublicYnNot(id, "N")
			.orElseThrow(() -> new GeneralException(ResponseCode.NOT_FOUND));
	}

	/**
	 * ????????? ????????????
	 */
	@Transactional
	public Long remove(
		Long restaurantId,
		Long userId
	) {
		final Restaurant restaurant = getRestaurantById(restaurantId);
		final User user = getUserById(userId);
		if (verifyRestaurantAndUser(user, restaurant))
			throw new GeneralException(ResponseCode.BAD_REQUEST);

		removeAllAssociation(restaurant);
		restaurantRepository.delete(restaurant);
		return restaurantId;
	}

	private void removeAllAssociation(Restaurant restaurant) {
		reviewRepository.deleteBatchByRestaurant(restaurant);
		likeRepository.deleteBatchByRestaurant(restaurant);
		scrapRepository.deleteBatchByRestaurant(restaurant);
		alarmService.removeAlarm(restaurant);
	}

	/**
	 *	????????? ?????? ?????? (??????)
	 */
	public List<RestaurantListResponse> findAllForFilter(
		RestaurantFilterSearchRequest searchRequest,
		Long userId
	) {
		if (searchRequest.isValidPoint())
			throw new GeneralException(ResponseCode.BAD_REQUEST);

		final List<RestaurantListResponse> restaurantListResponses;
		switch (searchRequest.getType()) {
			case Name:
				restaurantListResponses = getRestaurantListResponseByName(searchRequest);
				break;

			case Habit:
				restaurantListResponses = getRestaurantListResponsesByHabit(searchRequest);
				break;

			case Country:
				restaurantListResponses = getRestaurantListResponseByCountry(searchRequest);
				break;

			default:
				restaurantListResponses = Collections.emptyList();
		}

		if (userId != null)
			setIsLike(restaurantListResponses, userId);
		return restaurantListResponses;
	}

	/**
	 *	????????? ?????? ?????? (??????)
	 */
	public List<RestaurantListResponse> findAllForMap(
		RestaurantMapSearchRequest searchRequest,
		Long userId
	) {
		final List<RestaurantListResponse> restaurantListResponses
			= restaurantRepository.findAllBySearchForMap(searchRequest);
		if (userId != null)
			setIsLike(restaurantListResponses, userId);
		return restaurantListResponses;
	}

	private void setIsLike(
		List<RestaurantListResponse> restaurantListResponses,
		Long userId
	) {
		final List<Long> likesIds = getMyLikesIds(getUserById(userId));
		restaurantListResponses.forEach(restaurant -> restaurant.updateIsLike(likesIds.contains(restaurant.getId())));
	}

	private List<RestaurantListResponse> getRestaurantListResponseByCountry(RestaurantFilterSearchRequest searchRequest) {
		if (searchRequest.isNullCountry())
			throw new GeneralException(ResponseCode.BAD_REQUEST);

		return likeRepository.findRestaurantsByUserCountry(searchRequest);
	}

	private List<RestaurantListResponse> getRestaurantListResponsesByHabit(RestaurantFilterSearchRequest searchRequest) {
		if (searchRequest.isNullHabit())
			throw new GeneralException(ResponseCode.BAD_REQUEST);

		List<Long> ids = userHabitRepository.findUserIdByHabit(searchRequest);
		return likeRepository.findRestaurantsByUserHabit(ids, searchRequest);
	}

	private List<RestaurantListResponse> getRestaurantListResponseByName(RestaurantFilterSearchRequest searchRequest) {
		if (searchRequest.isNullName())
			throw new GeneralException(ResponseCode.BAD_REQUEST);

		return restaurantRepository.findAllByRestaurantName(searchRequest.getName());
	}

	private List<Long> getMyLikesIds(User user) {
		return likeRepository.findLikesByUser(user)
			.stream()
			.map(likes -> likes.getRestaurant().getId())
			.collect(Collectors.toList());
	}

	/**
	 *	????????? ?????? ??????
	 */
	public RestaurantResponse findOne(
		Long restaurantId,
		Long userId
	) {
		final Restaurant restaurant = getRestaurantById(restaurantId);
		final RestaurantResponse restaurantResponse = RestaurantResponse.entityToDto(restaurant);

		final List<String> images = getRestaurantImages(restaurant);
		User restaurantUser = restaurant.getUser();
		restaurantResponse.updateImages(images);
		restaurantResponse.updateUserInfo(restaurantUser);
		restaurantResponse.updateIsMine(restaurantUser.getId().equals(userId));

		if (userId != null) {
			User user = getUserById(userId);
			restaurantResponse.updateIsLike(likeRepository.findLikesByRestaurantAndUser(restaurant, user).isPresent());
			restaurantResponse.updateIsScrap(scrapRepository.findScrapByRestaurantAndUser(restaurant, user).isPresent());
		}
		return restaurantResponse;
	}

	private List<String> getRestaurantImages(Restaurant restaurant) {
		return restaurant.getImages()
			.stream()
			.map(RestaurantAttach::getImage)
			.collect(Collectors.toList());
	}

	/**
	 *	????????? ????????? ??????
	 */
	public LikesPercentResponse getLikesPercent(Long restaurantId) {
		final Restaurant restaurant = getRestaurantById(restaurantId);
		final Long totalCount = likeRepository.countByRestaurant(restaurant);

		final List<LikesPercent> countryLikePercents = likeRepository.findLikesPercentForCountry(restaurantId);
		final List<LikesPercent> habitLikePercents = likeRepository.findLikesPercentForHabit(restaurantId);

		setCountryLikesPercent(totalCount, countryLikePercents);
		setHabitLikesPercent(totalCount, habitLikePercents);

		return LikesPercentResponse.builder()
			.countryPercent(countryLikePercents)
			.habitPercent(habitLikePercents)
			.build();
	}

	private void setHabitLikesPercent(
		Long totalCount,
		List<LikesPercent> habitLikePercents
	) {
		habitLikePercents.forEach(habitLikes -> habitLikes.calculatePercent(totalCount));
	}

	private void setCountryLikesPercent(
		Long totalCount,
		List<LikesPercent> countryLikePercents
	) {
		countryLikePercents.forEach(countryLikes -> countryLikes.calculatePercent(totalCount));
	}

	/**
	 * ????????? ???????????? ?????? (????????????)
	 */
	@Transactional
	public Long modifyPub(
		PubRequest pubRequest,
		Long restaurantId
	) {
		final Restaurant restaurant = getRestaurantById(restaurantId);
		restaurant.updatePublicYn(pubRequest.getIsPub());

		if (!pubRequest.getIsPub()) {
			final AlarmRequest alarmRequest
				= AlarmRequest.of(null, restaurant, AlarmType.Reject, pubRequest.getCause());
			alarmService.registerAndSend(alarmRequest);
		}

		return restaurantId;
	}

	/**
	 * ????????? ?????? ?????? (????????????)
	 */
	public Page<RestaurantListResponse> findAllForAdmin(Pageable pageable) {
		return restaurantRepository.findAllForAdmin(pageable);
	}

	/**
	 * ????????? ?????? ?????? (????????????)
	 */
	public RestaurantResponse findOneForAdmin(Long restaurantId) {
		final Restaurant restaurant = getRestaurantById(restaurantId);
		final RestaurantResponse restaurantResponse = RestaurantResponse.entityToDto(restaurant);

		final List<String> images = getRestaurantImages(restaurant);
		restaurantResponse.updateImages(images);
		return restaurantResponse;
	}
}
