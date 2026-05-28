package com.upb.ecommerce.examen.service;

import com.upb.ecommerce.examen.dto.request.EstudianteRequest;
import com.upb.ecommerce.examen.dto.response.EstudianteResponse;
import com.upb.ecommerce.examen.entity.Estudiante;
import com.upb.ecommerce.examen.entity.Materia;
import com.upb.ecommerce.examen.repository.EstudianteRepository;
import com.upb.ecommerce.examen.repository.MateriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final MateriaRepository materiaRepository;

    public EstudianteService(EstudianteRepository estudianteRepository,
                             MateriaRepository materiaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.materiaRepository = materiaRepository;
    }

    public List<EstudianteResponse> listar() {
        return estudianteRepository.findAll().stream()
                .map(EstudianteResponse::fromEntity)
                .toList();
    }

    public EstudianteResponse obtenerPorId(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con id=" + id));
        return EstudianteResponse.fromEntity(estudiante);
    }

    public List<EstudianteResponse> listarPorMateria(Long materiaId) {
        return estudianteRepository.findByMateriaId(materiaId).stream()
                .map(EstudianteResponse::fromEntity)
                .toList();
    }

    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        Materia materia = materiaRepository.findById(request.getMateriaId())
                .orElseThrow(() -> new RuntimeException(
                        "Materia no encontrada con id=" + request.getMateriaId()));

        if (estudianteRepository.findByNroDocumento(request.getNroDocumento()).isPresent()) {
            throw new RuntimeException(
                    "Ya existe un estudiante con el documento: " + request.getNroDocumento());
        }

        Estudiante estudiante = new Estudiante();
        estudiante.setNombre(request.getNombre());
        estudiante.setApellido(request.getApellido());
        estudiante.setMateria(materia);
        estudiante.setNota(request.getNota());
        estudiante.setNroTelefono(request.getNroTelefono());
        estudiante.setNroDocumento(request.getNroDocumento());
        return EstudianteResponse.fromEntity(estudianteRepository.save(estudiante));
    }

    @Transactional
    public EstudianteResponse actualizar(Long id, EstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con id=" + id));

        Materia materia = materiaRepository.findById(request.getMateriaId())
                .orElseThrow(() -> new RuntimeException(
                        "Materia no encontrada con id=" + request.getMateriaId()));

        estudiante.setNombre(request.getNombre());
        estudiante.setApellido(request.getApellido());
        estudiante.setMateria(materia);
        estudiante.setNota(request.getNota());
        estudiante.setNroTelefono(request.getNroTelefono());
        estudiante.setNroDocumento(request.getNroDocumento());
        return EstudianteResponse.fromEntity(estudianteRepository.save(estudiante));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!estudianteRepository.existsById(id)) {
            throw new RuntimeException("Estudiante no encontrado con id=" + id);
        }
        estudianteRepository.deleteById(id);
    }
}
