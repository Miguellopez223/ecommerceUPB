package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.AtributoProductoRequest;
import com.upb.ecommerce.core.dto.response.AtributoProductoResponse;
import com.upb.ecommerce.data.repository.AtributoProductoRepository;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.domain.entities.AtributoProducto;
import com.upb.ecommerce.domain.entities.Producto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AtributoProductoService {

    private final AtributoProductoRepository atributoRepository;
    private final ProductoRepository productoRepository;

    public AtributoProductoService(AtributoProductoRepository atributoRepository,
                                    ProductoRepository productoRepository) {
        this.atributoRepository = atributoRepository;
        this.productoRepository = productoRepository;
    }

    public List<AtributoProductoResponse> listarPorProducto(Long productoId) {
        return atributoRepository.findByProductoId(productoId)
                .stream().map(AtributoProductoResponse::fromEntity).toList();
    }

    @Transactional
    public AtributoProductoResponse agregar(AtributoProductoRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        AtributoProducto atributo = new AtributoProducto();
        atributo.setProducto(producto);
        atributo.setNombre(request.getNombre());
        atributo.setValor(request.getValor());
        return AtributoProductoResponse.fromEntity(atributoRepository.save(atributo));
    }

    @Transactional
    public AtributoProductoResponse actualizar(Long id, AtributoProductoRequest request) {
        AtributoProducto atributo = atributoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atributo no encontrado"));
        atributo.setNombre(request.getNombre());
        atributo.setValor(request.getValor());
        return AtributoProductoResponse.fromEntity(atributoRepository.save(atributo));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!atributoRepository.existsById(id)) throw new RuntimeException("Atributo no encontrado");
        atributoRepository.deleteById(id);
    }
}
