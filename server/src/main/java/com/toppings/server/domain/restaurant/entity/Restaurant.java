package com.toppings.server.domain.restaurant.entity;

import com.toppings.server.domain.restaurant.constant.FoodType;
import com.toppings.server.domain_global.entity.BaseEntity;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@DynamicUpdate
@DynamicInsert
@Table(name = "t_restaurant")
public class Restaurant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long id;

    @Column(name = "restaurant_name", columnDefinition = "varchar(100)")
    private String name;

    @Column(name = "restaurant_description", columnDefinition = "text")
    private String description;

    @Column(name = "restaurant_address", columnDefinition = "varchar(150)")
    private String address;

    @Column(name = "restaurant_latitude")
    private Double latitude;

    @Column(name = "restaurant_longitude")
    private Double longitude;

    @Column(name = "delete_yn", columnDefinition = "varchar(1) default 'N'")
    private String deleteYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "restaurant_type", columnDefinition = "varchar(100)")
    private FoodType type;
    
    // 사진 경로랑 주소

    // 좋아요

    // 식습관

    // 리뷰

    // 스크랩
}