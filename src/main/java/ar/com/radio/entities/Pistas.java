package ar.com.radio.entities;
import jakarta.persistence.*;

@Entity
@Table(name = "pistas_audio")
public class Pistas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 120)
    private String artista;

    @Column(name = "url_audio", nullable = false, length = 500)
    private String urlAudio;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(name = "icono_url", length = 500)
    private String iconoUrl;

    @Column(nullable = false)
    private Integer orden = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id", nullable = false)
    private Programas programa;

    public Pistas() {
    }

    public Pistas(String titulo, String artista, String urlAudio, Integer duracionSegundos,
                  String iconoUrl, Integer orden, Programas programa) {
        this.titulo = titulo;
        this.artista = artista;
        this.urlAudio = urlAudio;
        this.duracionSegundos = duracionSegundos;
        this.iconoUrl = iconoUrl;
        this.orden = orden;
        this.programa = programa;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public String getUrlAudio() {
        return urlAudio;
    }

    public void setUrlAudio(String urlAudio) {
        this.urlAudio = urlAudio;
    }

    public Integer getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(Integer duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public String getIconoUrl() {
        return iconoUrl;
    }

    public void setIconoUrl(String iconoUrl) {
        this.iconoUrl = iconoUrl;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Programas getPrograma() {
        return programa;
    }

    public void setPrograma(Programas programa) {
        this.programa = programa;
    }
}
