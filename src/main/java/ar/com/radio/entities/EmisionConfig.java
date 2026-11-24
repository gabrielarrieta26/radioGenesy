package ar.com.radio.entities;
import jakarta.persistence.*;

@Entity
@Table(name = "emision_config")
public class EmisionConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_emisora", nullable = false, length = 120)
    private String nombreEmisora;

    @Column(name = "stream_url", nullable = false, length = 500)
    private String streamUrl;

    @Column(name = "en_vivo", nullable = false)
    private boolean enVivo = true;

    public EmisionConfig() {
    }

    public EmisionConfig(String nombreEmisora, String streamUrl, boolean enVivo) {
        this.nombreEmisora = nombreEmisora;
        this.streamUrl = streamUrl;
        this.enVivo = enVivo;
    }

    public Long getId() {
        return id;
    }

    public String getNombreEmisora() {
        return nombreEmisora;
    }

    public void setNombreEmisora(String nombreEmisora) {
        this.nombreEmisora = nombreEmisora;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public boolean isEnVivo() {
        return enVivo;
    }

    public void setEnVivo(boolean enVivo) {
        this.enVivo = enVivo;
    }
}
