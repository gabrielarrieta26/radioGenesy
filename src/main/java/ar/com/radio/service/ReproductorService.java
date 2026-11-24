package ar.com.radio.service;

import ar.com.radio.entities.HistorialReproduccion;
import ar.com.radio.entities.Pistas;
import ar.com.radio.repository.HistorialReproduccionRepository;
import ar.com.radio.repository.PistasRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Estado único de la transmisión: lo comparten el estudio y la vista del oyente. */
@Service
public class ReproductorService {

    private final PistasRepository pistaRepository;
    private final HistorialReproduccionRepository historialRepository;

    private Long pistaId;
    private boolean reproduciendo;
    private double posicion;
    private long actualizadoEpoch;
    private long version;
    private Long ultimoRegistrado;
    private final Map<String, Long> presencia = new ConcurrentHashMap<>();

    private static final long VENTANA_PRESENCIA_SEGUNDOS = 5;

    public ReproductorService(PistasRepository pistaRepository,
                              HistorialReproduccionRepository historialRepository) {
        this.pistaRepository = pistaRepository;
        this.historialRepository = historialRepository;
    }

    public record Estado(PistaDto pista, boolean reproduciendo, double posicion,
                         long version, String sonido) {
    }

    public record PistaDto(Long id, String titulo, String artista, String urlAudio, String icono) {
    }

    public synchronized Estado estado() {
        if (pistaId == null) {
            return new Estado(null, false, 0, version, sonidoActivo());
        }
        Pistas pista = pistaRepository.findById(pistaId).orElse(null);
        if (pista == null) {
            pistaId = null;
            reproduciendo = false;
            posicion = 0;
            version++;
            return new Estado(null, false, 0, version, sonidoActivo());
        }
        return new Estado(dto(pista), reproduciendo, posicionCalculada(), version, sonidoActivo());
    }

    public synchronized boolean reproducir(Long id) {
        Pistas pista = pistaRepository.findById(id).orElse(null);
        if (pista == null) {
            return false;
        }
        pistaId = id;
        reproduciendo = true;
        posicion = 0;
        actualizadoEpoch = ahora();
        version++;
        registrar(pista);
        return true;
    }

    public synchronized boolean pausar() {
        if (pistaId == null || !reproduciendo) {
            return false;
        }
        posicion = posicionCalculada();
        reproduciendo = false;
        actualizadoEpoch = ahora();
        version++;
        return true;
    }

    public synchronized boolean reanudar() {
        if (pistaId == null || reproduciendo) {
            return false;
        }
        reproduciendo = true;
        actualizadoEpoch = ahora();
        version++;
        return true;
    }

    /** Avanza al siguiente tema de la playlist. Si `versionEsperada` no coincide, otro cliente ya lo hizo. */
    public synchronized Estado siguiente(Long versionEsperada) {
        if (versionEsperada != null && versionEsperada != version) {
            return estado();
        }
        List<Pistas> ordenadas = pistaRepository.findAll(
                Sort.by("orden").ascending().and(Sort.by("id").ascending()));
        if (ordenadas.isEmpty()) {
            pistaId = null;
            reproduciendo = false;
            posicion = 0;
            version++;
            return estado();
        }
        int actual = -1;
        for (int i = 0; i < ordenadas.size(); i++) {
            if (ordenadas.get(i).getId().equals(pistaId)) {
                actual = i;
                break;
            }
        }
        reproducir(ordenadas.get((actual + 1) % ordenadas.size()).getId());
        return estado();
    }

    public synchronized Estado fijarPosicion(double segundos) {
        if (pistaId == null) {
            return estado();
        }
        posicion = Math.max(0, segundos);
        actualizadoEpoch = ahora();
        version++;
        return estado();
    }

    public synchronized boolean estaReproduciendo() {
        return reproduciendo;
    }

    /** Cada vista avisa de que sigue abierta al consultar el estado. */
    public void registrarPresencia(String rol) {
        if ("oyente".equals(rol) || "estudio".equals(rol)) {
            presencia.put(rol, ahora());
        }
    }

    /** Solo una vista suena: el oyente si está abierta; si no, el estudio. */
    private String sonidoActivo() {
        long limite = ahora() - VENTANA_PRESENCIA_SEGUNDOS;
        boolean oyente = presencia.getOrDefault("oyente", 0L) >= limite;
        boolean estudio = presencia.getOrDefault("estudio", 0L) >= limite;
        if (oyente) return "oyente";
        if (estudio) return "estudio";
        return "ninguno";
    }

    private double posicionCalculada() {
        if (!reproduciendo) {
            return posicion;
        }
        return posicion + Math.max(0, ahora() - actualizadoEpoch);
    }

    private void registrar(Pistas pista) {
        if (ultimoRegistrado != null && ultimoRegistrado.equals(pista.getId())) {
            return;
        }
        historialRepository.save(new HistorialReproduccion(
                pista.getTitulo(),
                pista.getArtista(),
                LocalDateTime.now(),
                pista.getIconoUrl()
        ));
        ultimoRegistrado = pista.getId();
    }

    private PistaDto dto(Pistas pista) {
        return new PistaDto(
                pista.getId(),
                pista.getTitulo(),
                pista.getArtista(),
                pista.getUrlAudio(),
                pista.getIconoUrl()
        );
    }

    private long ahora() {
        return Instant.now().getEpochSecond();
    }
}
