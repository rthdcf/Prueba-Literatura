package com.alura.Literatura.principal;

import com.alura.Literatura.dto.AutorDTO;
import com.alura.Literatura.dto.LibroDTO;
import com.alura.Literatura.dto.RespuestaLibrosDTO;
import com.alura.Literatura.model.Autor;
import com.alura.Literatura.model.Libro;
import com.alura.Literatura.service.AutorService;
import com.alura.Literatura.service.ConsumoAPI;
import com.alura.Literatura.service.ConvierteDatos;
import com.alura.Literatura.service.LibroService;
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

    @Autowired
    private LibroService libroService;

    @Autowired
    private AutorService autorService;

    @Autowired
    private ConsumoAPI consumoAPI;

    @Autowired
    private ConvierteDatos convierteDatos;

    private static final String BASE_URL = "http://gutendex.com/books";

    public void mostrarMenu() {
        Scanner scanner = new Scanner(System.in);
        int opcion;

        do {
            System.out.println("____________________________________");
            System.out.println("*** LITERATURA ***");
            System.out.println("1. Buscar libro por titulo");
            System.out.println("2. Listar libros registrados");
            System.out.println("3. Listar autores registrados");
            System.out.println("4. Listar autores vivos en un año");
            System.out.println("5. Listar libros por idioma");
            System.out.println("6. Limpiar base de datos");
            System.out.println("0. Salir");
            System.out.print("Seleccione una opción: ");
            opcion = scanner.nextInt();
            scanner.nextLine();

            switch (opcion) {
                case 1:
                    System.out.println(" ");
                    System.out.print("Ingrese el titulo del libro: ");
                    String titulo = scanner.nextLine();
                    try {
                        String encodedTitulo = URLEncoder.encode(titulo, StandardCharsets.UTF_8);
                        String json = consumoAPI.obtenerDatos(BASE_URL + "?search=" + encodedTitulo);
                        RespuestaLibrosDTO respuestaLibrosDTO = convierteDatos.obtenerDatos(json, RespuestaLibrosDTO.class);
                        List<LibroDTO> librosDTO = respuestaLibrosDTO.getLibros();

                        if (librosDTO == null || librosDTO.isEmpty()) {
                            System.out.println("📚 Libro no encontrado en la API");
                        } else {
                            boolean libroRegistrado = false;

                            for (LibroDTO libroDTO : librosDTO) {
                                if (libroDTO.getTitulo() != null && libroDTO.getTitulo().equalsIgnoreCase(titulo)) {

                                    Optional<Libro> libroExistente = libroService.obtenerLibroPorTitulo(titulo);
                                    if (libroExistente.isPresent()) {
                                        System.out.println("Detalle: Clave (titulo)= (" + titulo + ") ya existe.");
                                        System.out.println("No se puede registrar el mismo libro más de una vez.");
                                        libroRegistrado = true;
                                        break;
                                    }

                                    // Construir y guardar el libro
                                    Libro libro = new Libro();
                                    libro.setTitulo(libroDTO.getTitulo());
                                    String idioma = (libroDTO.getIdiomas() != null && !libroDTO.getIdiomas().isEmpty())
                                            ? libroDTO.getIdiomas().get(0)
                                            : "desconocido";
                                    libro.setIdioma(idioma);
                                    libro.setNumeroDescargas(libroDTO.getNumeroDescargas());

                                    // Manejo seguro de autor (puede venir vacío)
                                    Autor autor = null;
                                    if (libroDTO.getAutores() != null && !libroDTO.getAutores().isEmpty()) {
                                        AutorDTO primerAutorDTO = libroDTO.getAutores().get(0);
                                        autor = autorService.obtenerAutorPorNombre(primerAutorDTO.getNombre())
                                                .orElseGet(() -> {
                                                    Autor nuevoAutor = new Autor();
                                                    nuevoAutor.setNombre(primerAutorDTO.getNombre());
                                                    nuevoAutor.setAnoNacimiento(primerAutorDTO.getAnoNacimiento());
                                                    nuevoAutor.setAnoFallecimiento(primerAutorDTO.getAnoFallecimiento());
                                                    return autorService.crearAutor(nuevoAutor);
                                                });
                                    }
                                    libro.setAutor(autor);

                                    libroService.crearLibro(libro);
                                    System.out.println("✅ Libro registrado: " + libro.getTitulo());
                                    mostrarDetallesLibro(libroDTO);
                                    libroRegistrado = true;
                                    break;
                                }
                            }

                            if (!libroRegistrado) {
                                System.out.println("No se encontró un libro exactamente con el título proporcionado.");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error al obtener datos de la API: " + e.getMessage());
                    }
                    break;

                case 2:
                    libroService.listarLibros().forEach(libro -> {
                        System.out.println("_____________________________________________");
                        System.out.println("**** LIBRO ****");
                        System.out.println("Titulo: " + libro.getTitulo());
                        System.out.println("Autor: " + (libro.getAutor() != null ? libro.getAutor().getNombre() : "Desconocido"));
                        System.out.println("Idioma: " + libro.getIdioma());
                        System.out.println("Número de descargas: " + libro.getNumeroDescargas());
                        System.out.println(" ");
                    });
                    break;

                case 3:
                    autorService.listarAutores().forEach(autor -> {
                        System.out.println("_____________________________________________");
                        System.out.println("**** AUTOR ****");
                        System.out.println("Autor: " + autor.getNombre());
                        System.out.println("Fecha de nacimiento: " + autor.getAnoNacimiento());
                        System.out.println("Fecha de fallecimiento: " + (autor.getAnoFallecimiento() != null ? autor.getAnoFallecimiento() : "-"));
                        String libros = (autor.getLibros() == null || autor.getLibros().isEmpty())
                                ? "-"
                                : autor.getLibros().stream().map(Libro::getTitulo).collect(Collectors.joining(", "));
                        System.out.println("Libros: [" + libros + "]");
                        System.out.println( " ");
                    });
                    break;

                case 4:
                    System.out.print("Ingrese el año para buscar autores vivos: ");
                    int ano = scanner.nextInt();
                    scanner.nextLine();
                    List<Autor> autoresVivos = autorService.listarAutoresVivosEnAno(ano);
                    if (autoresVivos.isEmpty()) {
                        System.out.println("No se encontraron autores vivos en el año " + ano);
                    } else {
                        autoresVivos.forEach(autor -> {
                            System.out.println("_____________________________________________");
                            System.out.println("**** AUTOR ****");
                            System.out.println("Autor: " + autor.getNombre());
                            System.out.println("Fecha de nacimiento: " + autor.getAnoNacimiento());
                            System.out.println("Fecha de fallecimiento: " + (autor.getAnoFallecimiento() != null ? autor.getAnoFallecimiento() : "-"));
                            System.out.println("Cantidad de libros: " + (autor.getLibros() != null ? autor.getLibros().size() : 0));
                            System.out.println( " ");
                        });
                    }
                    break;

                case 5:
                    System.out.println("Ingrese el idioma (es | en | fr | pt): ");
                    String idioma = scanner.nextLine();
                    if ("es".equalsIgnoreCase(idioma) || "en".equalsIgnoreCase(idioma)
                            || "fr".equalsIgnoreCase(idioma) || "pt".equalsIgnoreCase(idioma)) {
                        libroService.listarLibrosPorIdioma(idioma).forEach(libro -> {
                            System.out.println("_____________________________________________");
                            System.out.println("**** LIBRO ****");
                            System.out.println("Titulo: " + libro.getTitulo());
                            System.out.println("Autor: " + (libro.getAutor() != null ? libro.getAutor().getNombre() : "Desconocido"));
                            System.out.println("Idioma: " + libro.getIdioma());
                            System.out.println("Número de descargas: " + libro.getNumeroDescargas());
                            System.out.println( " " );
                        });
                    } else {
                        System.out.println("Idioma no válido. Intente de nuevo.");
                    }
                    break;

                case 6: // 🔥 añadido
                    System.out.print("⚠️ ¿Seguro que deseas eliminar todos los libros y autores? (s/n): ");
                    String confirmacion = scanner.nextLine();
                    if (confirmacion.equalsIgnoreCase("s")) {
                        libroService.eliminarTodos(); // deberías crear este método en LibroService
                        autorService.eliminarTodos(); // y este en AutorService
                        System.out.println("✅ Base de datos limpiada correctamente.");
                    } else {
                        System.out.println("❌ Operación cancelada.");
                    }
                    break;

                case 0:
                    System.out.println("Saliendo . . .");
                    break;

                default:
                    System.out.println("Opción no válida. Intente nuevamente.");
            }
        } while (opcion != 0);

        scanner.close();
    }

    private void mostrarDetallesLibro(LibroDTO libroDTO) {
        System.out.println("_____________________________________________");
        System.out.println("*** LIBRO ***");
        System.out.println("Título: " + libroDTO.getTitulo());

        // Autores legibles
        String autoresStr = (libroDTO.getAutores() == null || libroDTO.getAutores().isEmpty())
                ? "Desconocido"
                : libroDTO.getAutores().stream()
                .map(a -> {
                    StringBuilder sb = new StringBuilder(a.getNombre());
                    boolean tieneFechas = (a.getAnoNacimiento() != 0) || (a.getAnoFallecimiento() != null);
                    if (tieneFechas) {
                        sb.append(" (");
                        if (a.getAnoNacimiento() != 0) sb.append(a.getAnoNacimiento());
                        sb.append(" - ");
                        if (a.getAnoFallecimiento() != null) sb.append(a.getAnoFallecimiento());
                        sb.append(")");
                    }
                    return sb.toString();
                })
                .collect(Collectors.joining(", "));
        System.out.println("Autor(es): " + autoresStr);

        // Idiomas legibles
        String idiomasStr = (libroDTO.getIdiomas() == null || libroDTO.getIdiomas().isEmpty())
                ? "Desconocido"
                : String.join(", ", libroDTO.getIdiomas());
        System.out.println("Idioma(s): " + idiomasStr);

        System.out.println("Número de descargas: " + libroDTO.getNumeroDescargas());
        System.out.println(" ");
    }
}
