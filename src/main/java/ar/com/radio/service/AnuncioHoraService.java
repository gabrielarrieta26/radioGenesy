package ar.com.radio.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AnuncioHoraService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] UNIDADES = {
            "", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
            "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis",
            "diecisiete", "dieciocho", "diecinueve", "veinte"
    };

    private static final String[] VEINTIENES = {
            "", "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco",
            "veintiséis", "veintisiete", "veintiocho", "veintinueve"
    };

    private static final String[] DECENAS = {
            "", "", "", "treinta", "cuarenta", "cincuenta"
    };

    private final AtomicLong secuencia = new AtomicLong();
    private volatile Anuncio ultimo;
    private volatile boolean automatico = false;

    public record Anuncio(long id, String texto, String hora, long fechaEpoch) {
    }

    public Anuncio anunciar() {
        LocalTime ahora = LocalTime.now();
        Anuncio anuncio = new Anuncio(
                secuencia.incrementAndGet(),
                textoHora(ahora),
                ahora.format(HORA),
                Instant.now().getEpochSecond()
        );
        this.ultimo = anuncio;
        return anuncio;
    }

    public Anuncio ultimo() {
        return ultimo;
    }

    public boolean isAutomatico() {
        return automatico;
    }

    public void setAutomatico(boolean automatico) {
        this.automatico = automatico;
    }

    /** Anuncio programado para el toque exacto de cada hora. */
    @Scheduled(cron = "0 0 * * * *")
    public void anuncioAutomatico() {
        if (automatico) {
            anunciar();
        }
    }

    private String textoHora(LocalTime hora) {
        int h = hora.getHour();
        int m = hora.getMinute();
        String minutos = enPalabras(m);

        if (h == 1) {
            return minutos.equals("cero") ? "Es la una" : "Es la una con " + minutos + " minutos";
        }
        String horas = enPalabras(h);
        return minutos.equals("cero")
                ? "Son las " + horas + " horas"
                : "Son las " + horas + " horas con " + minutos + " minutos";
    }

    private String enPalabras(int numero) {
        if (numero <= 20) {
            return UNIDADES[numero];
        }
        if (numero < 30) {
            return VEINTIENES[numero - 20];
        }
        int decena = numero / 10;
        int unidad = numero % 10;
        return unidad == 0 ? DECENAS[decena] : DECENAS[decena] + " y " + UNIDADES[unidad];
    }
}
