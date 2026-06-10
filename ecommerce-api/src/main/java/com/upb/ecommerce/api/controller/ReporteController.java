package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.response.MovimientoInventarioResponse;
import com.upb.ecommerce.core.dto.response.ReporteAnaliticoResponse;
import com.upb.ecommerce.core.dto.response.ReporteClienteResponse;
import com.upb.ecommerce.core.dto.response.ReporteProductosResponse;
import com.upb.ecommerce.core.dto.response.ReporteVentasResponse;
import com.upb.ecommerce.core.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Reportes analíticos de la tienda (5 pestañas). Solo accesibles por ADMIN.
 *
 * <p>Los parámetros {@code desde}/{@code hasta} son fechas ISO (yyyy-MM-dd) opcionales;
 * por defecto se reportan los últimos 30 días.
 */
@RestController
@RequestMapping("/api/reportes/tienda/{tiendaId}")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/analitico")
    public ResponseEntity<ReporteAnaliticoResponse> analitico(
            @PathVariable Long tiendaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.analitico(tiendaId, desde, hasta));
    }

    @GetMapping("/ventas")
    public ResponseEntity<ReporteVentasResponse> ventas(
            @PathVariable Long tiendaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.ventas(tiendaId, desde, hasta));
    }

    @GetMapping("/productos")
    public ResponseEntity<ReporteProductosResponse> productos(
            @PathVariable Long tiendaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "30") int diasSinMovimiento) {
        return ResponseEntity.ok(reporteService.productos(tiendaId, desde, hasta, diasSinMovimiento));
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<ReporteClienteResponse>> clientes(
            @PathVariable Long tiendaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.clientes(tiendaId, desde, hasta));
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<MovimientoInventarioResponse>> movimientos(
            @PathVariable Long tiendaId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.movimientos(tiendaId, tipo, usuarioId, desde, hasta));
    }
}
