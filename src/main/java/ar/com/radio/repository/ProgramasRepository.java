package ar.com.radio.repository;

import ar.com.radio.entities.Programas;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramasRepository extends JpaRepository<Programas, Long> {

    @EntityGraph(attributePaths = "pistas")
    List<Programas> findByActivoTrueOrderByHoraInicioAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}
