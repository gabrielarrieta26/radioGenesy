package ar.com.radio.repository;

import ar.com.radio.entities.HistorialReproduccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialReproduccionRepository extends JpaRepository<HistorialReproduccion, Long> {

    List<HistorialReproduccion> findTop5ByOrderByFechaHoraDesc();

    List<HistorialReproduccion> findTop10ByOrderByFechaHoraDesc();
}
