package com.upb.ecommerce.api.scheduler;

import com.upb.ecommerce.core.service.CarritoService;
import com.upb.ecommerce.data.repository.CarritoRepository;
import com.upb.ecommerce.domain.entities.Carrito;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Barrido programado de carritos abandonados.
 *
 * <p>Cada 8 horas busca carritos que sigan ACTIVO pero cuya última modificación sea de hace
 * más de {@code carrito.abandono.horas} (1 h por defecto): son clientes que metieron productos
 * al carrito y no completaron la compra. Los procesa por páginas (lotes) y, por cada carrito,
 * lanza un trabajador {@code @Async} que le envía un recordatorio por correo y lo marca como
 * ABANDONADO.
 *
 * <p>Es el mismo patrón del ejemplo de clase (tarea {@code @Scheduled} que recorre páginas y
 * dispara trabajos en paralelo), adaptado al dominio del ecommerce.
 */
@Slf4j
@Component
public class CarritoAbandonadoJob {

    /** Tamaño de cada lote/página que se procesa de una vez. */
    private static final int TAMANIO_LOTE = 50;
    /** Tope de seguridad de lotes por ejecución (evita un bucle infinito ante errores). */
    private static final int MAX_LOTES = 1000;

    private final CarritoRepository carritoRepository;
    private final CarritoService carritoService;

    @Value("${carrito.abandono.horas:1}")
    private long horasAbandono;

    public CarritoAbandonadoJob(CarritoRepository carritoRepository, CarritoService carritoService) {
        this.carritoRepository = carritoRepository;
        this.carritoService = carritoService;
    }

    // "segundo minuto hora dia mes diaSemana" -> 0 0 */8 * * * = a las 00:00, 08:00 y 16:00.
    @Scheduled(cron = "${carrito.abandono.cron:0 0 */8 * * *}")
    public void barrerCarritosAbandonados() {
        LocalDateTime limite = LocalDateTime.now().minusHours(horasAbandono);
        log.info("Barrido de carritos abandonados: ACTIVO sin cambios desde antes de {}", limite);

        // Ordenamos por el más antiguo primero. Siempre leemos la página 0 porque, al marcar
        // cada carrito como ABANDONADO, deja de cumplir el filtro (sale del conjunto ACTIVO),
        // así que el siguiente lote vuelve a quedar al inicio.
        Pageable pageable = PageRequest.of(0, TAMANIO_LOTE, Sort.by("fechaActualizacion").ascending());

        int lote = 0;
        int totalProcesados = 0;
        Page<Carrito> pagina;
        do {
            pagina = carritoRepository.findByEstadoAndFechaActualizacionBefore("ACTIVO", limite, pageable);
            if (pagina.isEmpty()) {
                break;
            }
            log.info("Procesando lote {} ({} carritos)", lote, pagina.getNumberOfElements());

            // Lanza un trabajador @Async por carrito y guarda su Future.
            List<Future<Long>> trabajadores = new ArrayList<>();
            for (Carrito carrito : pagina.getContent()) {
                trabajadores.add(carritoService.procesarCarritoAbandonado(carrito.getId()));
            }

            // Espera a que todos los trabajadores del lote terminen antes de seguir.
            for (Future<Long> trabajador : trabajadores) {
                try {
                    Long id = trabajador.get();
                    totalProcesados++;
                    log.info("Carrito {} procesado", id);
                } catch (Exception e) {
                    log.warn("Error procesando un carrito del lote {}: {}", lote, e.getMessage());
                }
            }
            lote++;
        } while (lote < MAX_LOTES);

        log.info("Barrido finalizado. Carritos procesados: {}", totalProcesados);
    }
}
