package ar.com.radio.controller;

import ar.com.radio.entities.HistorialReproduccion;
import ar.com.radio.entities.Pistas;
import ar.com.radio.repository.HistorialReproduccionRepository;
import ar.com.radio.repository.PistasRepository;
import ar.com.radio.service.AnuncioHoraService;
import ar.com.radio.service.ReproductorService;
import ar.com.radio.service.SincronizacionMusicaService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RadioApiController {

    private final PistasRepository pistaRepository;
    private final HistorialReproduccionRepository historialRepository;
    private final AnuncioHoraService anuncioHoraService;
    private final SincronizacionMusicaService sincronizacionMusicaService;
    private final ReproductorService reproductorService;

    public RadioApiController(PistasRepository pistaRepository,
                              HistorialReproduccionRepository historialRepository,
                              AnuncioHoraService anuncioHoraService,
                              SincronizacionMusicaService sincronizacionMusicaService,
                              ReproductorService reproductorService) {
        this.pistaRepository = pistaRepository;
        this.historialRepository = historialRepository;
        this.anuncioHoraService = anuncioHoraService;
        this.sincronizacionMusicaService = sincronizacionMusicaService;
        this.reproductorService = reproductorService;
    }

    @GetMapping("/cola")
    public List<PistaCola> cola() {
        Sort orden = Sort.by("orden").ascending().and(Sort.by("id").ascending());
        return pistaRepository.findAll(orden).stream()
                .map(pista -> new PistaCola(
                        pista.getId(),
                        pista.getTitulo(),
                        pista.getArtista(),
                        pista.getUrlAudio(),
                        pista.getIconoUrl()
                ))
                .toList();
    }

    @GetMapping("/reproductor")
    public ReproductorService.Estado estadoReproductor(
            @RequestParam(name = "rol", required = false) String rol) {
        reproductorService.registrarPresencia(rol);
        return reproductorService.estado();
    }

    @PostMapping("/reproductor/reproducir/{id}")
    public ResponseEntity<ReproductorService.Estado> reproducir(@PathVariable Long id) {
        if (!reproductorService.reproducir(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reproductorService.estado());
    }

    @PostMapping("/reproductor/pausar")
    public ReproductorService.Estado pausar() {
        reproductorService.pausar();
        return reproductorService.estado();
    }

    @PostMapping("/reproductor/reanudar")
    public ReproductorService.Estado reanudar() {
        reproductorService.reanudar();
        return reproductorService.estado();
    }

    @PostMapping("/reproductor/siguiente")
    public ReproductorService.Estado siguiente(@RequestParam(name = "version", required = false) Long version) {
        return reproductorService.siguiente(version);
    }

    @PostMapping("/reproductor/posicion")
    public ReproductorService.Estado posicion(@RequestParam(name = "segundos") double segundos) {
        return reproductorService.fijarPosicion(segundos);
    }

    @PostMapping("/reproducir/{id}")
    public ResponseEntity<Void> registrarReproduccion(@PathVariable Long id) {
        Pistas pista = pistaRepository.findById(id).orElse(null);
        if (pista == null) {
            return ResponseEntity.notFound().build();
        }
        historialRepository.save(new HistorialReproduccion(
                pista.getTitulo(),
                pista.getArtista(),
                LocalDateTime.now(),
                pista.getIconoUrl()
        ));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/anuncio-hora")
    public AnuncioHoraService.Anuncio anunciarHora() {
        return anuncioHoraService.anunciar();
    }

    @GetMapping("/anuncio-hora")
    public Map<String, Object> ultimoAnuncio() {
        AnuncioHoraService.Anuncio anuncio = anuncioHoraService.ultimo();
        if (anuncio == null) {
            return Map.of("automatico", anuncioHoraService.isAutomatico());
        }
        return Map.of(
                "id", anuncio.id(),
                "texto", anuncio.texto(),
                "hora", anuncio.hora(),
                "fechaEpoch", anuncio.fechaEpoch(),
                "automatico", anuncioHoraService.isAutomatico()
        );
    }

    @PostMapping("/anuncio-hora/automatico")
    public Map<String, Object> configurarAutomatico(@RequestParam boolean activo) {
        anuncioHoraService.setAutomatico(activo);
        return Map.of("automatico", anuncioHoraService.isAutomatico());
    }

    @GetMapping("/sincronizacion")
    public Map<String, Object> estadoSincronizacion() {
        return Map.of(
                "automatica", sincronizacionMusicaService.isAutomatica(),
                "carpeta", sincronizacionMusicaService.getCarpeta()
        );
    }

    @PostMapping("/sincronizacion/automatica")
    public Map<String, Object> configurarSincronizacionAutomatica(@RequestParam boolean activo) {
        sincronizacionMusicaService.setAutomatica(activo);
        return Map.of("automatica", sincronizacionMusicaService.isAutomatica());
    }

    public record PistaCola(Long id, String titulo, String artista, String urlAudio, String icono) {
    }
}
