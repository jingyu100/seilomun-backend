package com.yju.team2.seilomun.domain.event.repository;

import com.yju.team2.seilomun.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
