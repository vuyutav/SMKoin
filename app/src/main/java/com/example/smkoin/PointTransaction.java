package com.example.smkoin;

final class PointTransaction {
    final String id;
    final String type;
    final String description;
    final String createdAt;
    final int pointsDelta;

    PointTransaction(String id, String type, String description, String createdAt, int pointsDelta) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
        this.pointsDelta = pointsDelta;
    }
}
