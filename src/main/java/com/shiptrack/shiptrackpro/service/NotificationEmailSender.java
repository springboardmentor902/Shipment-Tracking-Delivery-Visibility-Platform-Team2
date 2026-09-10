package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.entity.Notification;

/**
 * Delivery boundary for notifications. Keeping this separate from the
 * persistence service makes delivery failures non-fatal and easy to replace
 * with SMS or push implementations later.
 */
public interface NotificationEmailSender {

    void send(Notification notification);
}
