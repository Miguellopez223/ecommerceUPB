package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.integracion.StereumCreateChargeRequest;
import com.upb.ecommerce.core.integracion.StereumCreateChargeResponse;
import com.upb.ecommerce.core.integracion.StereumService;
import com.upb.ecommerce.core.integracion.SistemaExternoService;
import com.upb.ecommerce.core.integracion.TiendaExternoRequest;
import com.upb.ecommerce.core.integracion.TiendaExternoResponse;
import com.upb.ecommerce.core.integracion.UsuarioExternoRequest;
import com.upb.ecommerce.core.integracion.UsuarioExternoResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Endpoints de integración con sistemas externos. Solo accesibles por ADMIN.
 *
 * <ul>
 *   <li><b>/externo/**</b> — consume el backend de otro ecommerce par
 *       ({@link SistemaExternoService}). Requiere haber llamado antes a
 *       {@code POST /api/auth/externo} para cachear el token.</li>
 *   <li><b>/stereum/**</b> — genera cobros (QR) genéricos vía Stereum
 *       ({@link StereumService}).</li>
 * </ul>
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/integracion")
@PreAuthorize("hasRole('ADMIN')")
public class IntegracionController {

    private final SistemaExternoService sistemaExternoService;
    private final StereumService stereumService;

    // --- Sistema externo: usuarios ------------------------------------------

    @GetMapping("/externo/usuarios/{tiendaId}")
    public ResponseEntity<List<UsuarioExternoResponse>> listarUsuariosExternos(@PathVariable Long tiendaId) {
        try {
            return ResponseEntity.ok(sistemaExternoService.listarUsuarios(tiendaId));
        } catch (Exception e) {
            throw badGateway("listar usuarios del sistema externo", e);
        }
    }

    @PostMapping("/externo/usuarios")
    public ResponseEntity<UsuarioExternoResponse> crearUsuarioExterno(
            @RequestBody UsuarioExternoRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(sistemaExternoService.crearUsuario(request));
        } catch (Exception e) {
            throw badGateway("crear usuario en el sistema externo", e);
        }
    }

    // --- Sistema externo: tiendas -------------------------------------------

    @GetMapping("/externo/tiendas")
    public ResponseEntity<List<TiendaExternoResponse>> listarTiendasExternas() {
        try {
            return ResponseEntity.ok(sistemaExternoService.listarTiendas());
        } catch (Exception e) {
            throw badGateway("listar tiendas del sistema externo", e);
        }
    }

    @PostMapping("/externo/tiendas")
    public ResponseEntity<TiendaExternoResponse> crearTiendaExterna(
            @RequestBody TiendaExternoRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(sistemaExternoService.crearTienda(request));
        } catch (Exception e) {
            throw badGateway("crear tienda en el sistema externo", e);
        }
    }

    // --- Stereum: generación de cobro (QR) ----------------------------------

    @PostMapping("/stereum/cargo")
    public ResponseEntity<StereumCreateChargeResponse> crearCargoStereum(
            @Valid @RequestBody StereumCreateChargeRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(stereumService.crearCargo(request));
        } catch (Exception e) {
            throw badGateway("generar el cobro en Stereum", e);
        }
    }

    // --- helpers ------------------------------------------------------------

    private ResponseStatusException badGateway(String accion, Exception e) {
        log.error("Error al {}", accion, e);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al " + accion);
    }
}
