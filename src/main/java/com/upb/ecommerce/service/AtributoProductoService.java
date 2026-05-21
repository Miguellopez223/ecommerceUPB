package com.upb.ecommerce.service;

import com.upb.ecommerce.dto.AtributoProductoRequest;
import com.upb.ecommerce.dto.AtributoProductoResponse;
import com.upb.ecommerce.repository.AtributoProductoRepository;
import com.upb.ecommerce.repository.ProductoRepository;
import com.upb.ecommerce.repository.entities.AtributoProducto;
import com.upb.ecommerce.repository.entities.Producto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AtributoProductoService {

    private final AtributoProductoRepository atributoProductoRepository;
    private final ProductoRepository productoRepository;

    public AtributoProductoService(AtributoProductoRepository atributoProductoRepository,
                                    ProductoRepository productoRepository) {
        this.atributoProductoRepository = atributoProductoRepository;
        this.productoRepository = productoRepository;
    }

    public List<AtributoProductoResponse> listarPorProducto(Long productoId) {
        return atributoProductoRepository.findByProductoId(productoId)
                .stream()
                .map(AtributoProductoResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AtributoProductoResponse agregar(AtributoProductoRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        AtributoProducto atributo = new AtributoProducto();
        atributo.setProducto(producto);
        atributo.setNombre(request.getNombre());
        atributo.setValor(request.getValor());
        return AtributoProductoResponse.fromEntity(atributoProductoRepository.save(atributo));
    }

    @Transactional
    public AtributoProductoResponse actualizar(Long id, AtributoProductoRequest request) {
        AtributoProducto atributo = atributoProductoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atributo no encontrado"));
        atributo.setNombre(request.getNombre());
        atributo.setValor(request.getValor());
        return AtributoProductoResponse.fromEntity(atributoProductoRepository.save(atributo));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!atributoProductoRepository.existsById(id)) {
            throw new RuntimeException("Atributo no encontrado");
        }
        atributoProductoRepository.deleteById(id);
    }
}
