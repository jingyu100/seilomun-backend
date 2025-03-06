package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
