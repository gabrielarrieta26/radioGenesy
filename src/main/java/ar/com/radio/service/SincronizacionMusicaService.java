package ar.com.radio.service;

import ar.com.radio.entities.Pistas;
import ar.com.radio.entities.Programas;
import ar.com.radio.repository.PistasRepository;
import ar.com.radio.repository.ProgramasRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class SincronizacionMusicaService {

    private static final Set<String> AUDIO = Set.of(".mp3", ".wav", ".ogg", ".m4a", ".aac", ".flac", ".opus");
    private static final List<String> IMAGENES = List.of(".jpg", ".jpeg", ".png", ".webp", ".svg");

    private final PistasRepository pistaRepository;
    private final ProgramasRepository programaRepository;

    @Value("${radio.musica.carpeta:C:/proyectos/radio/musica}")
    private String carpeta;

    @Value("${radio.musica.programa-id:1}")
    private Long programaId;

    private volatile boolean automatica = false;
    private volatile Resultado ultimoResultado;

    public SincronizacionMusicaService(PistasRepository pistaRepository,
                                       ProgramasRepository programaRepository) {
        this.pistaRepository = pistaRepository;
        this.programaRepository = programaRepository;
    }

    public record Resultado(int encontrados, int importados, int omitidos,
                            List<String> temas, List<String> errores, String carpeta, String programa) {
    }

    public Resultado sincronizar(String carpetaEspecificada, Long programaEspecificado, boolean recursivo) {
        String ruta = (carpetaEspecificada != null && !carpetaEspecificada.isBlank())
                ? carpetaEspecificada.trim() : carpeta;
        Long idPrograma = programaEspecificado != null ? programaEspecificado : programaId;

        Programas programa = programaRepository.findById(idPrograma).orElse(null);
        if (programa == null) {
            Resultado error = new Resultado(0, 0, 0, List.of(),
                    List.of("No existe el programa con id " + idPrograma), ruta, "—");
            this.ultimoResultado = error;
            return error;
        }

        Path raiz;
        try {
            raiz = Path.of(ruta).toAbsolutePath().normalize();
        } catch (Exception ex) {
            Resultado error = new Resultado(0, 0, 0, List.of(),
                    List.of("Ruta inválida: " + ex.getMessage()), ruta, programa.getNombre());
            this.ultimoResultado = error;
            return error;
        }

        if (!Files.isDirectory(raiz)) {
            Resultado error = new Resultado(0, 0, 0, List.of(),
                    List.of("La carpeta no existe: " + raiz), raiz.toString(), programa.getNombre());
            this.ultimoResultado = error;
            return error;
        }

        List<Path> audios = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(raiz, recursivo ? Integer.MAX_VALUE : 1)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> esAudio(p.getFileName().toString()))
                    .sorted()
                    .forEach(audios::add);
        } catch (IOException ex) {
            errores.add("No se pudo leer la carpeta: " + ex.getMessage());
        }

        int importados = 0;
        int omitidos = 0;
        List<String> temas = new ArrayList<>();
        int ordenBase = pistaRepository.findByProgramaIdOrderByOrdenAsc(idPrograma).size();

        for (Path archivo : audios) {
            String url = urlRelativa(raiz, archivo);
            if (pistaRepository.existsByUrlAudio(url)) {
                omitidos++;
                continue;
            }

            String nombre = quitarExtension(archivo.getFileName().toString());
            String artista = null;
            String titulo = nombre;
            int separador = nombre.indexOf(" - ");
            if (separador > 0 && separador < nombre.length() - 3) {
                artista = nombre.substring(0, separador).trim();
                titulo = nombre.substring(separador + 3).trim();
            }

            String portada = buscarPortada(archivo, raiz);
            try {
                pistaRepository.save(new Pistas(
                        titulo.isBlank() ? nombre : titulo,
                        artista,
                        url,
                        null,
                        portada,
                        ordenBase + importados + 1,
                        programa
                ));
                importados++;
                temas.add(titulo + (artista != null ? " — " + artista : ""));
            } catch (Exception ex) {
                errores.add(nombre + ": " + ex.getMessage());
            }
        }

        Resultado resultado = new Resultado(audios.size(), importados, omitidos,
                temas, errores, raiz.toString(), programa.getNombre());
        this.ultimoResultado = resultado;
        return resultado;
    }

    @Scheduled(fixedDelayString = "${radio.musica.intervalo-ms:300000}")
    public void sincronizacionAutomatica() {
        if (automatica) {
            sincronizar(null, null, true);
        }
    }

    public boolean isAutomatica() {
        return automatica;
    }

    public void setAutomatica(boolean automatica) {
        this.automatica = automatica;
    }

    public Resultado getUltimoResultado() {
        return ultimoResultado;
    }

    public String getCarpeta() {
        return carpeta;
    }

    public Long getProgramaId() {
        return programaId;
    }

    private boolean esAudio(String nombre) {
        String lower = nombre.toLowerCase(Locale.ROOT);
        return AUDIO.stream().anyMatch(lower::endsWith);
    }

    private String quitarExtension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto > 0 ? nombre.substring(0, punto) : nombre;
    }

    private String buscarPortada(Path audio, Path raiz) {
        String base = quitarExtension(audio.getFileName().toString());
        Path carpetaArchivo = audio.getParent();
        for (String extension : IMAGENES) {
            Path candidato = carpetaArchivo.resolve(base + extension);
            if (Files.isRegularFile(candidato)) {
                return urlRelativa(raiz, candidato);
            }
        }
        return null;
    }

    private String urlRelativa(Path raiz, Path archivo) {
        String relativa = raiz.relativize(archivo).toString().replace('\\', '/');
        String[] partes = relativa.split("/");
        return "/musica/" + String.join("/", Arrays.stream(partes)
                .map(parte -> URLEncoder.encode(parte, StandardCharsets.UTF_8).replace("+", "%20"))
                .toList());
    }
}
