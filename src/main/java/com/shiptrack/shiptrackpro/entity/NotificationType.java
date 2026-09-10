package com.shiptrack.shiptrackpro.entity;

/** Domain events that can result in a user notification. */
public enum NotificationType {
    SHIPMENT_UPDATE,
    DELAY_WARNING,
    DELIVERY_ALERT,
    ETA_UPDATE,
    MANUAL
}
