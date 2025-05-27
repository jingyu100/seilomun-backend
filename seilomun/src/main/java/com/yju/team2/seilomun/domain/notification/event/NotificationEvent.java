package com.yju.team2.seilomun.domain.notification.event;

import com.yju.team2.seilomun.domain.notification.enums.NotificationType;

public interface NotificationEvent {

    NotificationType getType();
    Long getSenderId();
    Character getSenderType();
    Object getEventData();
    String getEventId();

}