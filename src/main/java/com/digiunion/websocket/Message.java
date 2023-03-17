package com.digiunion.websocket;

import java.time.LocalDate;

import jakarta.json.bind.annotation.JsonbAnnotation;
import jakarta.json.bind.annotation.JsonbProperty;
@JsonbAnnotation
public record Message(@JsonbProperty String id, @JsonbProperty("message_type") Type type, @JsonbProperty("timestamp") LocalDate timestamp) {}
