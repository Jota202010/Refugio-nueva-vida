package com.refugio.nueva_vida.proyecto_de_aula.config;

import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.model.FotoPerro;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.FotoPerroRepository;
import com.refugio.nueva_vida.proyecto_de_aula.repository.PerroRepository;
import com.refugio.nueva_vida.proyecto_de_aula.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Component
@Profile("docker")
public class DockerDemoDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DockerDemoDataInitializer.class);
    private static final List<CuentaDemo> CUENTAS_DEMO = List.of(
        new CuentaDemo("adoptante1", "María Fernanda", "adoptante1@refugionuevavida.local",
            "Adopta2026!"),
        new CuentaDemo("adoptante2", "Carlos Andrés", "adoptante2@refugionuevavida.local",
            "Luna2026!"),
        new CuentaDemo("adoptante3", "Valentina", "adoptante3@refugionuevavida.local",
            "Max2026!")
    );

    private final UsuarioRepository usuarioRepository;
    private final PerroRepository perroRepository;
    private final FotoPerroRepository fotoPerroRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final Path uploadDirectory;

    public DockerDemoDataInitializer(UsuarioRepository usuarioRepository,
                                     PerroRepository perroRepository,
                                     FotoPerroRepository fotoPerroRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${app.demo.admin-password}") String adminPassword,
                                     @Value("${app.upload.dir:uploads/fotos}") String uploadDirectory) {
        this.usuarioRepository = usuarioRepository;
        this.perroRepository = perroRepository;
        this.fotoPerroRepository = fotoPerroRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearAdministradorDemo();
        crearUsuariosDemo();
        crearPerrosDemo();
    }

    private void crearAdministradorDemo() {
        if (usuarioRepository.existsByUsuario("admin")
                || usuarioRepository.existsByEmail("admin@refugionuevavida.local")) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setUsuario("admin");
        admin.setNombre("Administrador de demostración");
        admin.setEmail("admin@refugionuevavida.local");
        admin.setTelefono("0000000000");
        admin.setDireccion("Refugio Nueva Vida");
        admin.setContrasena(passwordEncoder.encode(adminPassword));
        admin.setRol(Usuario.Rol.administrador);
        usuarioRepository.save(admin);
        logger.info("Cuenta local de demostración creada: admin");
    }

    private void crearUsuariosDemo() {
        for (CuentaDemo cuenta : CUENTAS_DEMO) {
            if (usuarioRepository.existsByUsuario(cuenta.usuario())
                    || usuarioRepository.existsByEmail(cuenta.email())) {
                continue;
            }

            Usuario usuario = new Usuario();
            usuario.setUsuario(cuenta.usuario());
            usuario.setNombre(cuenta.nombre());
            usuario.setEmail(cuenta.email());
            usuario.setTelefono("3000000000");
            usuario.setDireccion("Bogotá, Colombia");
            usuario.setContrasena(passwordEncoder.encode(cuenta.contrasena()));
            usuario.setRol(Usuario.Rol.usuario);
            usuarioRepository.save(usuario);
        }
        logger.info("Cuentas locales de adopción de demostración verificadas.");
    }

    private void crearPerrosDemo() {
        Perro luna = new Perro();
        luna.setNombre("Luna");
        luna.setEdad("2 años");
        luna.setSexo(Perro.Sexo.Hembra);
        luna.setEstado(Perro.Estado.RESCATADO);
        luna.setEsterilizado(true);
        luna.setVacunado(true);
        luna.setDescripcion("Cariñosa y tranquila; está lista para encontrar un hogar.");
        luna.setNivelSalud(Perro.NivelSalud.SANO);
        luna.setSociabilidad(Perro.Sociabilidad.ALTA);
        luna.setEstadoPublicacion(Perro.EstadoPublicacion.PUBLICADO);

        Perro max = new Perro();
        max.setNombre("Max");
        max.setEdad("3 años");
        max.setSexo(Perro.Sexo.Macho);
        max.setEstado(Perro.Estado.ACOGIDO);
        max.setEsterilizado(true);
        max.setVacunado(true);
        max.setDescripcion("Activo y sociable; disfruta los paseos y jugar al aire libre.");
        max.setNivelSalud(Perro.NivelSalud.SANO);
        max.setSociabilidad(Perro.Sociabilidad.ALTA);
        max.setEstadoPublicacion(Perro.EstadoPublicacion.PUBLICADO);

        Perro coco = crearPerro("Coco", "1 año", Perro.Sexo.Macho, Perro.Estado.ACOGIDO,
            false, true, "Pequeño, curioso y muy cariñoso; disfruta conocer personas nuevas.",
            Perro.Sociabilidad.ALTA);
        Perro rocky = crearPerro("Rocky", "4 años", Perro.Sexo.Macho, Perro.Estado.RESCATADO,
            true, true, "Leal y atento; está aprendiendo a confiar y disfruta los paseos tranquilos.",
            Perro.Sociabilidad.MEDIA);
        Perro nala = crearPerro("Nala", "2 años", Perro.Sexo.Hembra, Perro.Estado.ACOGIDO,
            true, true, "Dulce y juguetona; busca una familia con tiempo para compartir.",
            Perro.Sociabilidad.ALTA);
        Perro bruno = crearPerro("Bruno", "5 años", Perro.Sexo.Macho, Perro.Estado.RESCATADO,
            true, true, "Un compañero sereno y noble que disfruta descansar cerca de su gente.",
            Perro.Sociabilidad.MEDIA);

        registrarPerroDemo(luna, "luna.jpg");
        registrarPerroDemo(max, "max.jpg");
        registrarPerroDemo(coco, "coco.jpg");
        registrarPerroDemo(rocky, "rocky.jpg");
        registrarPerroDemo(nala, "nala.jpg");
        registrarPerroDemo(bruno, "bruno.jpg");
        logger.info("Perros e imágenes de demostración verificados para la ejecución local en Docker.");
    }

    private Perro crearPerro(String nombre, String edad, Perro.Sexo sexo, Perro.Estado estado,
                             boolean esterilizado, boolean vacunado, String descripcion,
                             Perro.Sociabilidad sociabilidad) {
        Perro perro = new Perro();
        perro.setNombre(nombre);
        perro.setEdad(edad);
        perro.setSexo(sexo);
        perro.setEstado(estado);
        perro.setEsterilizado(esterilizado);
        perro.setVacunado(vacunado);
        perro.setDescripcion(descripcion);
        perro.setNivelSalud(Perro.NivelSalud.SANO);
        perro.setSociabilidad(sociabilidad);
        perro.setEstadoPublicacion(Perro.EstadoPublicacion.PUBLICADO);
        return perro;
    }

    private void registrarPerroDemo(Perro perroDemo, String nombreImagen) {
        Perro perro = perroRepository.findByNombreIgnoreCase(perroDemo.getNombre())
            .orElseGet(() -> perroRepository.save(perroDemo));
        if (fotoPerroRepository.findByPerroAndEsPerfilTrue(perro).isPresent()) {
            return;
        }

        boolean esPerroDemoAnterior = perro.getDescripcion() != null
            && perro.getDescripcion().equals(perroDemo.getDescripcion());
        if (perro != perroDemo && !esPerroDemoAnterior) {
            return;
        }

        String rutaImagen = "demo/dogs/" + nombreImagen;
        Path destino = uploadDirectory.resolve(nombreImagen);
        try {
            Files.createDirectories(uploadDirectory);
            if (Files.notExists(destino)) {
                try (InputStream imagen = new ClassPathResource(rutaImagen).getInputStream()) {
                    Files.copy(imagen, destino, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo instalar la imagen de demostración " + nombreImagen, e);
        }

        fotoPerroRepository.save(new FotoPerro(perro, "/fotos/" + nombreImagen, true, 0));
    }

    private record CuentaDemo(String usuario, String nombre, String email, String contrasena) {}
}
