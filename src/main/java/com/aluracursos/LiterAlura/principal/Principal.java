package com.aluracursos.LiterAlura.principal;

import com.aluracursos.LiterAlura.model.dto.Datos;
import com.aluracursos.LiterAlura.model.dto.DatosAutor;
import com.aluracursos.LiterAlura.model.dto.DatosLibros;
import com.aluracursos.LiterAlura.model.entidad.Autor;
import com.aluracursos.LiterAlura.model.entidad.Libro;
import com.aluracursos.LiterAlura.repository.AutorRepository;
import com.aluracursos.LiterAlura.repository.LibroRepository;
import com.aluracursos.LiterAlura.service.ApiService;
import com.aluracursos.LiterAlura.service.ConvierteDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

@Component
public class Principal {
    private static final String URL_BASE = "https://gutendex.com/books/";
    private final Scanner teclado;
    private boolean ejecutando;

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ConvierteDatos conversor;

    public Principal() {
        this.teclado = new Scanner(System.in);
        this.ejecutando = true;
    }

    public void mostrarMenu() {
        int opcion;
        do {
            System.out.println("\n=== Literalura ===");
            System.out.println("1- Buscar libro por título");
            System.out.println("2- Lista de libros registrados");
            System.out.println("3- Lista de autores registrados");
            System.out.println("4- Buscar autores vivos en un determinado año");
            System.out.println("5- Lista de libros por idioma");
            System.out.println("0- Salir");
            System.out.println("====================================");
            System.out.print("Seleccione una opción: ");

            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1 -> buscarLibroPorTitulo();
                case 2 -> listaDeLibrosRegistrados();
                case 3 -> listaDeAutoresRegistrados();
                case 4 -> buscarAutoresVivosPorAnio();
                case 5 -> listarLibrosPorIdioma();
                case 0 -> salir();
                default -> System.out.println("La opcion ingresada no es valida. Por favor, intente nuevamente.");
            }
        } while (opcion != 0);
    }

    private void buscarLibroPorTitulo() {
        try {
            System.out.print("\nIngrese el título del libro: ");
            String titulo = teclado.nextLine().trim();
            if (titulo.isEmpty()) { System.out.println("El título no puede estar vacío."); return; }

            libroRepository.findByTituloContainingIgnoreCase(titulo)
                    .ifPresentOrElse(
                            libro -> System.out.println("\nMensaje: [ El libro ingresado ya fue registrado en la BD ]\n\n" + libro),
                            () -> buscarEnApiExterna(titulo)
                    );
        } catch (Exception e) {
            System.out.println("Error durante la búsqueda: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buscarEnApiExterna(String titulo) {
        try {
            System.out.println("Buscando...");
            String json = apiService.obtenerDatos(URL_BASE + "?search=" + URLEncoder.encode(titulo, StandardCharsets.UTF_8));
            Datos datos = json == null || json.isEmpty() ? null : conversor.obtenerDatos(json, Datos.class);

            if (datos == null || datos.resultados() == null || datos.resultados().isEmpty()) {
                System.out.println("El titulo [ "+ titulo + " ] del libro ingresado no se encontro." );
            } else {
                manejarResultados(datos.resultados(), titulo);
            }
        } catch (Exception e) {
            System.out.println("Error al buscar en la API: " + e.getMessage());
        }
    }

    private void manejarResultados(List<DatosLibros> resultados, String tituloLibro) {
        resultados.stream()
                .filter(libro -> libro.titulo().equalsIgnoreCase(tituloLibro))
                .findFirst()
                .ifPresentOrElse(
                        this::registrarLibroConAutor,
                        () -> System.out.println("No se encontró ningún libro con el titulo: " + tituloLibro)
                );
    }

    private void registrarLibroConAutor(DatosLibros datosLibro) {
        try {
            if (datosLibro.autor() == null || datosLibro.autor().isEmpty()) {
                System.out.println("El libro no tiene autor registrado.");
                return;
            }

            Autor autor = autorRepository.findByNombre(datosLibro.autor().get(0).nombre())
                    .orElseGet(() -> {
                        System.out.println("Registrando nuevo autor: " + datosLibro.autor().get(0).nombre());
                        return autorRepository.save(new Autor(datosLibro.autor().get(0)));
                    });

            if (libroRepository.findByTituloContainingIgnoreCase(datosLibro.titulo()).isPresent()) {
                System.out.println("El libro ya existe en la base de datos.");
                return;
            }

            System.out.println("\nEl libro se guardó exitosamente en la BD.");
            System.out.println(libroRepository.save(new Libro(datosLibro, autor)));

        } catch (Exception e) {
            System.out.println("Error al guardar el libro y el autor: " + e.getMessage());
        }
    }

    private void listaDeLibrosRegistrados() {
        List<Libro> libros = libroRepository.findAll();
        System.out.println("\n=== Libros Registrados ===");
        if (libros.isEmpty()) {
            System.out.println("No se encontraron libros registrados en la BD.");
        } else {
            libros.forEach(System.out::println);
        }
    }

    private void listaDeAutoresRegistrados() {
        List<Autor> autores = autorRepository.findAutoresConLibros();

        if (autores.isEmpty()) {
            System.out.println("\n=== Autores Registrados ===");
            System.out.println("No se encontraron autores registrados en la BD.");
            return;
        }
        System.out.println("\n=== Autores Registrados ===");
        autores.forEach(autor -> {
            String fechaNacimiento = Optional.ofNullable(autor.getFechaDeNacimiento()).orElse("No disponible");
            String fechaFallecimiento = Optional.ofNullable(autor.getFechaFallecimiento()).orElse("No disponible");

            System.out.printf("\nAutor: %s " +
                            "| Fecha de nacimiento: %s " +
                            "| Fecha de fallecimiento: %s%n",
                    autor.getNombre(), fechaNacimiento, fechaFallecimiento);

            System.out.println("\nLibros:");
            autor.getLibros().forEach(libro ->
                    System.out.printf("- %s (Idioma: %s, Descargas: %s)%n", libro.getTitulo(), libro.getIdioma(), libro.getNumeroDeDescargas())
            );
            System.out.println("\n--------------------------------------------------------------------------------------");
        });
    }

    private void buscarAutoresVivosPorAnio() {
        try {
            System.out.println("\n=== Autores vivos por año ===");
            System.out.print("Ingrese el año para consultar: ");
            int anio = Integer.parseInt(teclado.nextLine().trim());

            if (anio < 0 || anio > 2025) {
                System.out.println("El año ingresado no es válido.");
                return;
            }

            List<Autor> autores = autorRepository.findAll().stream()
                    .filter(autor -> estaVivoEnAnio(autor, anio))
                    .collect(Collectors.toList());

            if (autores.isEmpty()) {
                System.out.println("No se encontraron autores vivos para el año " + anio + ".");
            } else {
                System.out.println("Autores vivos en " + anio + ":");
                autores.forEach(autor ->
                        System.out.printf("- %s (Nacimiento: %s)%n", autor.getNombre(), autor.getFechaDeNacimiento())
                );
            }

        } catch (NumberFormatException e) {
            System.out.println("Por favor, ingrese un año válido en formato numérico.");
        } catch (Exception e) {
            System.out.println("Error al procesar la consulta: " + e.getMessage());
        }
    }


    private boolean estaVivoEnAnio(Autor autor, int anio) {
        String fechaNacimiento = autor.getFechaDeNacimiento();
        if (fechaNacimiento == null || fechaNacimiento.isEmpty()) {
            return false;
        }
        try {
            int anioNacimiento = Integer.parseInt(fechaNacimiento);
            return anioNacimiento <= anio;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void listarLibrosPorIdioma() {
        System.out.println("\n=== Libros por Idioma ===");
        System.out.println("Idiomas disponibles: ES (Español), EN (Inglés), FR (Francés), PT (Portugués)");
        System.out.print("Ingrese el código del idioma: ");

        String idioma = teclado.nextLine().trim().toUpperCase();
        if (!esIdiomaValido(idioma)) {
            System.out.println("\n [ El idioma ingresado no es valido ]");
            return;
        }

        List<Libro> libros = libroRepository.findAll();
        List<Libro> librosFiltrados = libros.stream()
                .filter(libro -> libro.getIdioma().equalsIgnoreCase(idioma))
                .toList();

        if (librosFiltrados.isEmpty()) {
            System.out.println("No se encontraron libros en " + obtenerNombreIdioma(idioma));
            return;
        }

        System.out.println("\nLibros en " + obtenerNombreIdioma(idioma) + ":");
        librosFiltrados.forEach(libro ->
                System.out.printf("- %s (Autor: %s)%n",
                        libro.getTitulo(),
                        libro.getAutor().getNombre())
        );
    }

    private boolean esIdiomaValido(String idioma) {
        return idioma.matches("^(ES|EN|FR|PT)$");
    }

    private String obtenerNombreIdioma(String codigo) {
        return switch (codigo) {
            case "ES" -> "Español";
            case "EN" -> "Inglés";
            case "FR" -> "Francés";
            case "PT" -> "Portugués";
            default -> codigo;
        };
    }

    private void salir() {
        System.out.println("\n¡Gracias por utilizar nuestra aplicacion. Hasta pronto!");
        ejecutando = false;
    }

}
