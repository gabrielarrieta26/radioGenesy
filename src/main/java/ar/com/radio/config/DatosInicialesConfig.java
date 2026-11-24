package ar.com.radio.config;

import ar.com.radio.entities.EmisionConfig;
import ar.com.radio.entities.Pistas;
import ar.com.radio.entities.Programas;
import ar.com.radio.repository.EmisionConfigRepository;
import ar.com.radio.repository.PistasRepository;
import ar.com.radio.repository.ProgramasRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.List;

@Configuration
public class DatosInicialesConfig {

    @Bean
    CommandLineRunner cargarDatosPrueba(
            ProgramasRepository programaRepository,
            PistasRepository pistaRepository,
            EmisionConfigRepository emisionConfigRepository
    ) {
        return args -> {
            if (emisionConfigRepository.count() == 0) {
                emisionConfigRepository.save(new EmisionConfig(
                        "Radio genesy",
                        "http://localhost:8000/stream",
                        true
                ));
            }

            Programas manana = obtenerOCrearPrograma(
                    programaRepository,
                    "Mañana Rock & Clásicos",
                    "Bloque matutino con rock nacional, clásicos y temas para arrancar el día",
                    LocalTime.of(8, 0),
                    LocalTime.of(13, 0)
            );

            Programas noche = obtenerOCrearPrograma(
                    programaRepository,
                    "Trasnoche Electrónica & Chill",
                    "Selección continua para madrugada y turnos largos",
                    LocalTime.of(0, 0),
                    LocalTime.of(6, 0)
            );

            if (pistaRepository.count() == 0) {
                // Rutas relativas servidas por esta app: Thymeleaf les agrega el context path (/radio).
                pistaRepository.saveAll(List.of(
                        new Pistas("Track Demo 01", "Artista Local", "/audio/demo-1.wav", 4, null, 1, manana),
                        new Pistas("Track Demo 02", "Banda Clásica", "/audio/demo-2.wav", 4, null, 2, manana),
                        new Pistas("Night Pulse 01", "Synth Lab", "/audio/demo-3.wav", 4, null, 1, noche),
                        new Pistas("Tema Prueba 03", "Banda Indie", "/audio/demo-1.wav", 4, null, 3, manana),
                        new Pistas("Tema Prueba 04", "Rock Nacional", "/audio/demo-2.wav", 4, null, 4, manana),
                        new Pistas("Night Loop 02", "Synth Wave", "/audio/demo-3.wav", 4, null, 2, noche)
                ));
            }
        };
    }

    private Programas obtenerOCrearPrograma(ProgramasRepository programaRepository, String nombre,
                                            String descripcion, LocalTime horaInicio, LocalTime horaFin) {
        return programaRepository.findAll().stream()
                .filter(programa -> nombre.equalsIgnoreCase(programa.getNombre()))
                .findFirst()
                .orElseGet(() -> programaRepository.save(
                        new Programas(nombre, descripcion, horaInicio, horaFin)
                ));
    }
}
