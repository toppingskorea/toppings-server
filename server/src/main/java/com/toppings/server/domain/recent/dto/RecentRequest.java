package com.toppings.server.domain.recent.dto;

import javax.validation.constraints.NotBlank;

import com.toppings.server.domain.recent.constant.RecentType;
import com.toppings.server.domain.recent.entity.Recent;
import com.toppings.server.domain.user.entity.User;
import com.toppings.server.domain_global.constants.SearchCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecentRequest {

    private Long id;

    @NotBlank(message = "검색어를 확인해주세요.")
    private String keyword;

    @NotBlank(message = "내용을 확인해주세요.")
    private String content;

    @NotBlank(message = "타입을 확인해주세요.")
    private RecentType type;

    private SearchCategory category;

    public static Recent dtoToEntity(RecentRequest request, User user) {
        return Recent.builder()
                .keyword(request.getKeyword())
                .content(request.getContent())
                .type(request.getType())
                .category(request.getCategory())
                .user(user)
                .build();
    }

    public static boolean verifyRestaurantType(RecentRequest recentRequest) {
        return recentRequest.getType().equals(RecentType.Restaurant) && recentRequest.getCategory() != null;
    }
}
