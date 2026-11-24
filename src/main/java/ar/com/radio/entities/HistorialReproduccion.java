package ar.com.radio.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_reproducciones")
public class HistorialReproduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 120)
    private String artista;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "icono_url", length = 500)
    private String iconoUrl;

    public HistorialReproduccion() {
    }

    public HistorialReproduccion(String titulo, String artista, LocalDateTime fechaHora, String iconoUrl) {
        this.titulo = titulo;
        this.artista = artista;
        this.fechaHora = fechaHora;
        this.iconoUrl = iconoUrl;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getArtista() {
        return artista;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getIconoUrl() {
        return iconoUrl;
    }
}
