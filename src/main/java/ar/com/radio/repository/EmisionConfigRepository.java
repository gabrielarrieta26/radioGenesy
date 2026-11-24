package ar.com.radio.repository;
import ar.com.radio.entities.EmisionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface EmisionConfigRepository extends JpaRepository<EmisionConfig,Long>{
    Optional<EmisionConfig> findFirstByEnVivoTrueOrderByIdAsc();
}
