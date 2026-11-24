package ar.com.radio.repository;
import ar.com.radio.entities.Pistas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface PistasRepository extends JpaRepository<Pistas, Long>{
    List<Pistas> findByProgramaIdOrderByOrdenAsc(Long programaId);

    boolean existsByUrlAudio(String urlAudio);
}
