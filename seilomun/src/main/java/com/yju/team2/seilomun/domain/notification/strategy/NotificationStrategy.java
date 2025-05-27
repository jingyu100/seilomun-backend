package com.yju.team2.seilomun.domain.notification.strategy;

import com.yju.team2.seilomun.domain.notification.event.NotificationEvent;

import java.util.List;

public interface NotificationStrategy {

    List<Long> getRecipients(NotificationEvent event);

    Character getRecipientType();

}