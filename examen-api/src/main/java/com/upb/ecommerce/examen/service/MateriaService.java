package com.upb.ecommerce.examen.service;

import com.upb.ecommerce.examen.dto.request.MateriaRequest;
import com.upb.ecommerce.examen.dto.response.MateriaResponse;
import com.upb.ecommerce.examen.entity.Materia;
import com.upb.ecommerce.examen.repository.MateriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MateriaService {

    private final MateriaRepository materiaRepository;

    public MateriaService(MateriaRepository materiaRepository) {
        this.materiaRepository = materiaRepository;
    }

    public List<MateriaResponse> listar() {
        return materiaRepository.findAll().stream()
                .map(MateriaResponse::fromEntity)
                .toList();
    }

    public MateriaResponse obtenerPorId(Long id) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Materia no encontrada con id=" + id));
        return MateriaResponse.fromEntity(materia);
    }

    @Transactional
    public MateriaResponse crear(MateriaRequest request) {
        if (materiaRepository.findBySigla(request.getSigla()).isPresent()) {
            throw new RuntimeException("Ya existe una materia con la sigla: " + request.getSigla());
        }
        Materia materia = new Materia();
        materia.setNombre(request.getNombre());
        materia.setSigla(request.getSigla());
        return MateriaResponse.fromEntity(materiaRepository.save(materia));
    }

    @Transactional
    public MateriaResponse actualizar(Long id, MateriaRequest request) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Materia no encontrada con id=" + id));
        materia.setNombre(request.getNombre());
        materia.setSigla(request.getSigla());
        return MateriaResponse.fromEntity(materiaRepository.save(materia));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!materiaRepository.existsById(id)) {
            throw new RuntimeException("Materia no encontrada con id=" + id);
        }
        materiaRepository.deleteById(id);
    }
}
