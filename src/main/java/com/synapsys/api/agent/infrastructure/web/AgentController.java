package com.synapsys.api.agent.infrastructure.web;

import com.synapsys.api.agent.application.port.in.DeleteAgentUseCase;
import com.synapsys.api.agent.application.port.in.GetAgentStatisticsUseCase;
import com.synapsys.api.agent.application.port.in.ListAgentsUseCase;
import com.synapsys.api.agent.application.port.in.RevokeAgentUseCase;
import com.synapsys.api.agent.domain.model.AgentStatistics;
import com.synapsys.api.agent.domain.model.AgentView;
import com.synapsys.api.agent.domain.model.DeleteAgentCommand;
import com.synapsys.api.agent.domain.model.RevokeAgentCommand;
import com.synapsys.api.agent.infrastructure.web.dto.AgentResponse;
import com.synapsys.api.agent.infrastructure.web.dto.AgentStatisticsResponse;
import com.synapsys.api.agent.infrastructure.web.dto.PageResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import com.synapsys.api.shared.security.AuthenticatedUser;
import com.synapsys.api.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Agents")
@SecurityRequirement(name = "cookieAuth")
@Validated
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final ListAgentsUseCase listUseCase;
    private final RevokeAgentUseCase revokeUseCase;
    private final DeleteAgentUseCase deleteUseCase;
    private final GetAgentStatisticsUseCase statisticsUseCase;

    public AgentController(ListAgentsUseCase listUseCase, RevokeAgentUseCase revokeUseCase,
                          DeleteAgentUseCase deleteUseCase, GetAgentStatisticsUseCase statisticsUseCase) {
        this.listUseCase = listUseCase;
        this.revokeUseCase = revokeUseCase;
        this.deleteUseCase = deleteUseCase;
        this.statisticsUseCase = statisticsUseCase;
    }

    @Operation(summary = "Lister les agents (admin)",
        description = "Liste paginée avec statut dérivé (PENDING/ACTIVE/INACTIVE/REVOKED). Rate limit : 60 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste paginée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AgentResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "enrolledAt")
            @Pattern(regexp = "enrolledAt|serverName|lastActivityAt", message = "must be one of: enrolledAt, serverName, lastActivityAt") String sortBy,
            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "asc|desc", message = "must be 'asc' or 'desc'") String sortDirection,
            @Parameter(description = "Filtre optionnel : agents dont le nom de serveur ou l'adresse IP contient cette chaîne (insensible à la casse)")
            @RequestParam(required = false) @Size(max = 254, message = "must be at most 254 characters") String search) {
        SortRequest sort = new SortRequest(sortBy, "asc".equalsIgnoreCase(sortDirection));
        PageResult<AgentView> result = listUseCase.list(page, size, sort, search);
        PageResponse<AgentResponse> response = new PageResponse<>(
            result.content().stream()
                .map(v -> new AgentResponse(v.id(), v.serverName(), v.ipAddress(), v.status(),
                    v.fingerprint(), v.enrolledAt(), v.lastActivityAt()))
                .toList(),
            result.totalElements(), result.page(), result.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Statistiques des agents (admin)",
        description = "Compteurs actifs / inactifs / en attente / révoqués / total. Rate limit : 60 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistiques",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AgentStatisticsResponse.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/statistics")
    @RateLimiting(max = 60)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<AgentStatisticsResponse> statistics() {
        AgentStatistics s = statisticsUseCase.statistics();
        return ResponseEntity.ok(new AgentStatisticsResponse(s.active(), s.inactive(), s.pending(), s.revoked(), s.total()));
    }

    @Operation(summary = "Révoquer un agent (admin)",
        description = "Révocation durable + coupure immédiate de la connexion live + purge de présence. Rate limit : 20 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Agent révoqué", content = @Content),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Agent introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Agent déjà révoqué",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/revoke")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> revoke(@Parameter(description = "UUID de l'agent") @PathVariable UUID id,
                                       @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        revokeUseCase.revoke(new RevokeAgentCommand(id, caller.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer un agent (admin)",
        description = "Autorisée uniquement si l'agent est REVOKED (cycle révocation -> suppression). Sinon 409. Rate limit : 20 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Agent supprimé", content = @Content),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Agent introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Agent non révoqué (révoquer d'abord)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@Parameter(description = "UUID de l'agent") @PathVariable UUID id) {
        deleteUseCase.delete(new DeleteAgentCommand(id));
        return ResponseEntity.noContent().build();
    }
}
