package com.synapsys.api.identity.infrastructure.web.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, long totalElements, int page, int size) {}