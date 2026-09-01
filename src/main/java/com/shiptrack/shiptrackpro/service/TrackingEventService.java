package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.TrackingEventRequest;
import com.shiptrack.shiptrackpro.dto.TrackingEventResponse;

import java.util.List;

public interface TrackingEventService {

    TrackingEventResponse addTrackingEvent(Long shipmentId, TrackingEventRequest request);

    List<TrackingEventResponse> getTrackingEvents(Long shipmentId);
}
