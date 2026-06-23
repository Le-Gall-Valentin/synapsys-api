package com.synapsys.api.agent.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Réponse paginée générique")
public record PageResponse<T>(
    List<T> content,
    @Schema(example = "42") long totalElements,
    @Schema(example = "0") int page,
    @Schema(example = "20") int size
) {}
