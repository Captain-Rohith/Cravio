package com.javacravio.cravio.restaurant.repository;

import com.javacravio.cravio.restaurant.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByH3IndexIn(Set<String> h3Indexes);
}

