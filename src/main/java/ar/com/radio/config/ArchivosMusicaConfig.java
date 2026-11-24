package ar.com.radio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ArchivosMusicaConfig implements WebMvcConfigurer {

    @Value("${radio.musica.carpeta:C:/proyectos/radio/musica}")
    private String carpeta;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = Path.of(carpeta).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/musica/**")
                .addResourceLocations(ubicacion)
                .setCachePeriod(0);
    }
}
