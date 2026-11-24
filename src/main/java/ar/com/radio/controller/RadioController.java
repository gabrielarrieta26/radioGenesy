package ar.com.radio.controller;

import ar.com.radio.entities.EmisionConfig;
import ar.com.radio.entities.HistorialReproduccion;
import ar.com.radio.entities.Pistas;
import ar.com.radio.entities.Programas;
import ar.com.radio.repository.EmisionConfigRepository;
import ar.com.radio.repository.HistorialReproduccionRepository;
import ar.com.radio.repository.PistasRepository;
import ar.com.radio.repository.ProgramasRepository;
import ar.com.radio.service.SincronizacionMusicaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class RadioController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ProgramasRepository programaRepository;
    private final PistasRepository pistaRepository;
    private final EmisionConfigRepository emisionConfigRepository;
    private final HistorialReproduccionRepository historialRepository;
    private final SincronizacionMusicaService sincronizacionMusicaService;

    public RadioController(
            ProgramasRepository programaRepository,
            PistasRepository pistaRepository,
            EmisionConfigRepository emisionConfigRepository,
            HistorialReproduccionRepository historialRepository,
            SincronizacionMusicaService sincronizacionMusicaService
    ) {
        this.programaRepository = programaRepository;
        this.pistaRepository = pistaRepository;
        this.emisionConfigRepository = emisionConfigRepository;
        this.historialRepository = historialRepository;
        this.sincronizacionMusicaService = sincronizacionMusicaService;
    }

    /** Vista pública: reproductor + historial. */
    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("emision", emisionActual());
        model.addAttribute("historial", historialReciente(
                historialRepository.findTop5ByOrderByFechaHoraDesc()
        ));
        return "index";
    }

    /** Vista del operador: programación con sus playlists, carga de temas e historial. */
    @GetMapping("/admin")
    public String admin(
            @RequestParam(name = "programaId", required = false) Long programaId,
            Model model
    ) {
        List<Programas> programas = programaRepository.findByActivoTrueOrderByHoraInicioAsc();

        List<Pistas> pistas;
        if (programaId != null) {
            pistas = pistaRepository.findByProgramaIdOrderByOrdenAsc(programaId);
        } else {
            pistas = pistaRepository.findAll();
        }

        model.addAttribute("emision", emisionActual());
        model.addAttribute("programas", programas);
        model.addAttribute("pistas", pistas);
        model.addAttribute("programaSeleccionadoId", programaId);
        model.addAttribute("historial", historialReciente(
                historialRepository.findTop10ByOrderByFechaHoraDesc()
        ));
        model.addAttribute("sincronizacion", sincronizacionMusicaService.getUltimoResultado());
        model.addAttribute("carpetaMusica", sincronizacionMusicaService.getCarpeta());
        model.addAttribute("sincronizacionAutomatica", sincronizacionMusicaService.isAutomatica());

        return "admin";
    }

    @PostMapping("/sincronizar")
    public String sincronizarMusica(
            @RequestParam(name = "carpeta", required = false) String carpeta,
            @RequestParam(name = "programaId", required = false) Long programaId,
            @RequestParam(name = "recursivo", required = false) boolean recursivo
    ) {
        sincronizacionMusicaService.sincronizar(carpeta, programaId, recursivo);
        Long destino = programaId != null ? programaId : sincronizacionMusicaService.getProgramaId();
        return "redirect:/admin?programaId=" + destino + "&synced=1";
    }

    @PostMapping("/programas")
    public String guardarPrograma(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin
    ) {
        if (nombre != null && !nombre.isBlank() && horaInicio != null && horaFin != null
                && !programaRepository.existsByNombreIgnoreCase(nombre.trim())) {
            Programas programa = programaRepository.save(new Programas(
                    nombre.trim(),
                    (descripcion != null && !descripcion.isBlank()) ? descripcion.trim() : null,
                    horaInicio,
                    horaFin
            ));
            return "redirect:/admin?programaId=" + programa.getId();
        }
        return "redirect:/admin";
    }

    @PostMapping("/pistas")
    public String guardarPista(
            @RequestParam String titulo,
            @RequestParam(required = false) String artista,
            @RequestParam String urlAudio,
            @RequestParam(required = false) Integer duracionSegundos,
            @RequestParam(required = false) String iconoUrl,
            @RequestParam Long programaId
    ) {
        Programas programa = programaRepository.findById(programaId).orElse(null);
        if (programa != null && urlAudio != null && !urlAudio.isBlank() && titulo != null && !titulo.isBlank()) {
            int siguienteOrden = pistaRepository.findByProgramaIdOrderByOrdenAsc(programaId).size() + 1;
            Pistas nuevaPista = new Pistas(
                    titulo.trim(),
                    (artista != null && !artista.isBlank()) ? artista.trim() : null,
                    urlAudio.trim(),
                    duracionSegundos,
                    (iconoUrl != null && !iconoUrl.isBlank()) ? iconoUrl.trim() : null,
                    siguienteOrden,
                    programa
            );
            pistaRepository.save(nuevaPista);
        }
        return "redirect:/admin?programaId=" + programaId;
    }

    @PostMapping("/pistas/{id}/eliminar")
    public String eliminarPista(@PathVariable Long id,
                                @RequestParam(name = "programaId", required = false) Long programaId) {
        pistaRepository.deleteById(id);
        return programaId != null ? "redirect:/admin?programaId=" + programaId : "redirect:/admin";
    }

    private EmisionConfig emisionActual() {
        return emisionConfigRepository
                .findFirstByEnVivoTrueOrderByIdAsc()
                .orElseGet(() -> emisionConfigRepository.findAll().stream().findFirst().orElse(null));
    }

    private List<HistorialItem> historialReciente(List<HistorialReproduccion> registros) {
        List<HistorialItem> items = new ArrayList<>();
        for (HistorialReproduccion registro : registros) {
            items.add(new HistorialItem(
                    registro.getTitulo(),
                    registro.getArtista(),
                    registro.getFechaHora().format(HORA),
                    registro.getIconoUrl()
            ));
        }
        Collections.reverse(items);
        return items;
    }

    public record HistorialItem(String titulo, String artista, String hora, String icono) {
    }
}
