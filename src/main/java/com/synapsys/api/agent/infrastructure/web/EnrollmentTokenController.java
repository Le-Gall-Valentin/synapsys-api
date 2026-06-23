package com.synapsys.api.agent.infrastructure.web;

import com.synapsys.api.agent.application.port.in.CreateEnrollmentTokenUseCase;
import com.synapsys.api.agent.application.port.in.ListEnrollmentTokensUseCase;
import com.synapsys.api.agent.application.port.in.RevokeEnrollmentTokenUseCase;
import com.synapsys.api.agent.domain.model.CreateEnrollmentTokenCommand;
import com.synapsys.api.agent.domain.model.EnrollmentTokenStatus;
import com.synapsys.api.agent.domain.model.EnrollmentTokenView;
import com.synapsys.api.agent.domain.model.IssuedToken;
import com.synapsys.api.agent.domain.model.RevokeEnrollmentTokenCommand;
import com.synapsys.api.agent.infrastructure.web.dto.CreateEnrollmentTokenRequest;
import com.synapsys.api.agent.infrastructure.web.dto.CreatedTokenResponse;
import com.synapsys.api.agent.infrastructure.web.dto.EnrollmentTokenResponse;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Tag(name = "Agents - Tokens d'enrôlement")
@SecurityRequirement(name = "cookieAuth")
@Validated
@RestController
@RequestMapping("/api/agents/enrollment-tokens")
public class EnrollmentTokenController {

    private final CreateEnrollmentTokenUseCase createUseCase;
    private final ListEnrollmentTokensUseCase listUseCase;
    private final RevokeEnrollmentTokenUseCase revokeUseCase;

    public EnrollmentTokenController(CreateEnrollmentTokenUseCase createUseCase,
                                     ListEnrollmentTokensUseCase listUseCase,
                                     RevokeEnrollmentTokenUseCase revokeUseCase) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.revokeUseCase = revokeUseCase;
    }

    @Operation(summary = "Créer un token d'enrôlement (admin)",
        description = "Génère un token à usage unique. ttlMinutes optionnel : omis = maximum configuré (24h par défaut), " +
            "sinon durée de validité demandée. Le token en clair n'est renvoyé qu'ici. Rate limit : 20 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Token créé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreatedTokenResponse.class))),
        @ApiResponse(responseCode = "400", description = "serverName invalide, ou ttlMinutes hors bornes (< 1 ou supérieur au maximum configuré)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<CreatedTokenResponse> create(@Valid @RequestBody CreateEnrollmentTokenRequest request,
                                                        @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        Duration ttl = request.ttlMinutes() == null ? null : Duration.ofMinutes(request.ttlMinutes());
        IssuedToken issued = createUseCase.create(
            new CreateEnrollmentTokenCommand(request.serverName(), ttl, caller.userId()));
        URI location = URI.create("/api/agents/enrollment-tokens/" + issued.id());
        // A freshly issued token is ACTIVE by construction (IssuedToken carries no status: not yet consumed/revoked/expired).
        return ResponseEntity.created(location).body(new CreatedTokenResponse(
            issued.id(), issued.serverName(), issued.rawToken(), EnrollmentTokenStatus.ACTIVE,
            issued.expiresAt(), issued.createdAt()));
    }

    @Operation(summary = "Lister les tokens d'enrôlement (admin)",
        description = "Liste paginée. Le token et son hash ne sont jamais exposés. Rate limit : 60 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste paginée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Paramètre invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<EnrollmentTokenResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt")
            @Pattern(regexp = "createdAt|expiresAt|serverName", message = "must be one of: createdAt, expiresAt, serverName") String sortBy,
            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "asc|desc", message = "must be 'asc' or 'desc'") String sortDirection) {
        SortRequest sort = new SortRequest(sortBy, "asc".equalsIgnoreCase(sortDirection));
        PageResult<EnrollmentTokenView> result = listUseCase.list(page, size, sort);
        PageResponse<EnrollmentTokenResponse> response = new PageResponse<>(
            result.content().stream()
                .map(v -> new EnrollmentTokenResponse(v.id(), v.serverName(), v.status(), v.expiresAt(), v.createdBy(), v.createdAt()))
                .toList(),
            result.totalElements(), result.page(), result.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Révoquer un token d'enrôlement (admin)",
        description = "Révoque un token ACTIVE non utilisé. Un token CONSUMED/EXPIRED/REVOKED renvoie 409. Rate limit : 20 req/fenêtre.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Token révoqué", content = @Content),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Rôle insuffisant",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Token introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Token non révocable (déjà utilisé, expiré ou révoqué)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/revoke")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> revoke(@Parameter(description = "UUID du token") @PathVariable UUID id,
                                       @Parameter(hidden = true) @CurrentUser AuthenticatedUser caller) {
        revokeUseCase.revoke(new RevokeEnrollmentTokenCommand(id, caller.userId()));
        return ResponseEntity.noContent().build();
    }
}
