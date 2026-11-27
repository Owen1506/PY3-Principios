package Almacenamiento;

import estrategiasAsignacion.secuencial;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Map;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class discoVirtual implements AutoCloseable {

    private String Nombre;
    private long Tamaño;
    private RandomAccessFile disco;

    private static final int TAMAÑO_SECTOR = 512;
    private static final int INICIO_PARTICION_LBA = 8;
    private static final int BLOQUES_FCB = 32;

    public void crearDisco(String Nombre, long TamañoMB)
            throws IOException {
        crearDisco(Nombre, TamañoMB, null);
    }

    private void crearDisco(String Nombre, long TamañoMB, Path directorio)
            throws IOException {

        if (disco != null) {
            throw new IllegalStateException(
                    "Ya existe un disco abierto");
        }

        if (Nombre == null || Nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del disco es obligatorio");
        }

        if (TamañoMB <= 0) {
            throw new IllegalArgumentException(
                    "El tamaño debe ser mayor que cero");
        }

        File archivo = directorio == null
                ? new File(Nombre)
                : directorio.resolve(Nombre).toFile();

        if (archivo.exists()) {
            throw new IOException(
                    "Ya existe un archivo con ese nombre");
        }

        long tamañoBytes;

        try {
            tamañoBytes = Math.multiplyExact(
                    TamañoMB, 1024L * 1024L);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "El tamaño del disco es demasiado grande");
        }

        RandomAccessFile nuevo = new RandomAccessFile(archivo, "rw");

        try {
            nuevo.setLength(tamañoBytes);
        } catch (IOException e) {
            nuevo.close();
            throw e;
        }

        this.Nombre = Nombre;
        this.Tamaño = tamañoBytes;
        this.disco = nuevo;
    }

    public void abrirDisco(String nombre)
            throws IOException {

        if (disco != null) {
            throw new IllegalStateException(
                    "Ya existe un disco abierto");
        }

        File archivo = new File(nombre);

        if (!archivo.isFile()) {
            throw new IOException(
                    "El disco virtual no existe");
        }

        RandomAccessFile abierto = new RandomAccessFile(archivo, "rw");

        try {
            // Comprobar que contiene un MBR reconocible.
            MBR mbr = MBR.leer(abierto);

            if (mbr.getParticion().getFinBytes() > abierto.length()) {
                throw new IOException(
                        "La particion supera el tamaño del disco");
            }

            // Comprobar el VCB del File System.
            long inicio = mbr.getParticion().getInicioBytes();

            VolumeControlBlock.leer(
                    abierto, inicio + TAMAÑO_SECTOR);

        } catch (IOException | RuntimeException e) {
            abierto.close();
            throw e;
        }

        this.Nombre = nombre;
        this.Tamaño = abierto.length();
        this.disco = abierto;
    }

    public void formatear(String nombre,
            long tamañoMB,
            int tamañoBloque,
            int estrategia) throws IOException {
        formatear(nombre, tamañoMB, tamañoBloque, estrategia, null, null);
    }

    public static void formatear(Path archivo, long tamañoMB, int tamañoBloque,
            char[] contraseña, boolean reemplazar) throws IOException {
        if (contraseña == null || contraseña.length == 0) {
            throw new IllegalArgumentException("La contraseña de root es obligatoria");
        }
        prepararImagen(archivo, tamañoMB, tamañoBloque, 1, contraseña, reemplazar);
    }

    public static void reformatear(Path archivo, long tamañoMB,
            int tamañoBloque, int estrategia) throws IOException {
        prepararImagen(archivo, tamañoMB, tamañoBloque, estrategia, null, true);
    }

    private static void prepararImagen(Path archivo, long tamañoMB, int tamañoBloque,
            int estrategia, char[] contraseña, boolean reemplazar) throws IOException {
        Path destino = archivo.toAbsolutePath();
        if (!reemplazar && Files.exists(destino)) {
            throw new FileAlreadyExistsException(archivo.toString());
        }
        String nombre = destino.getFileName().toString();
        Path directorio = Files.createTempDirectory(destino.getParent(), ".format-");
        Path nuevo = directorio.resolve(nombre);
        try {
            try (discoVirtual disco = new discoVirtual()) {
                disco.formatear(nombre, tamañoMB, tamañoBloque, estrategia, directorio, contraseña);
            }
            if (reemplazar) {
                Files.move(nuevo, destino, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(nuevo, destino);
            }
        } finally {
            Files.deleteIfExists(nuevo);
            Files.deleteIfExists(directorio);
        }
    }

    private void formatear(String nombre, long tamañoMB, int tamañoBloque,
            int estrategia, Path directorio, char[] contraseña) throws IOException {

        if (tamañoBloque != 512 &&
                tamañoBloque != 1024 &&
                tamañoBloque != 2048 &&
                tamañoBloque != 4096) {

            throw new IllegalArgumentException(
                    "Tamaño de bloque no permitido");
        }

        // 1 = contigua, 2 = enlazada, 3 = indexada.
        if (estrategia < 1 || estrategia > 3) {
            throw new IllegalArgumentException(
                    "Estrategia de asignacion invalida");
        }

        if (nombre == null ||
                nombre.getBytes(StandardCharsets.UTF_8).length > 64) {

            throw new IllegalArgumentException(
                    "Nombre del File System invalido");
        }

        // Calcular primero el espacio necesario.
        long tamañoBytes;

        try {
            tamañoBytes = Math.multiplyExact(
                    tamañoMB, 1024L * 1024L);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Tamaño demasiado grande");
        }

        long inicioParticion = INICIO_PARTICION_LBA * 512L;

        long sectores = (tamañoBytes - inicioParticion) / 512L;

        if (sectores <= 0 || sectores > 0xFFFFFFFFL) {
            throw new IllegalArgumentException(
                    "Capacidad incompatible con este MBR");
        }

        long totalBloques = (sectores * 512L) / tamañoBloque;

        // Cada bit del bitmap representa un bloque.
        long bitsPorBloqueBitmap = tamañoBloque * 8L;

        long bloquesBitmap = (totalBloques + bitsPorBloqueBitmap - 1)
                / bitsPorBloqueBitmap;

        // Bloque 0: arranque.
        // Bloque 1: reservado (incluye el VCB si aplica).
        long inicioBitmap = 2;

        long inicioFCB = inicioBitmap + bloquesBitmap;
        long inicioDatos = inicioFCB + BLOQUES_FCB;

        if (inicioDatos >= totalBloques) {
            throw new IllegalArgumentException(
                    "No queda espacio para almacenar archivos");
        }

        long tamañoBitmap = bloquesBitmap * tamañoBloque;
        if (tamañoBitmap > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El bitmap es demasiado grande");
        }
        byte[] bitmap = new byte[(int) tamañoBitmap];
        secuencial.marcar(bitmap, 0, inicioDatos, true);

        long primerBloqueInicial = -1;
        long libres = totalBloques - inicioDatos;
        if (contraseña != null) {
            // El HOME vacío tiene un FCB y se le asignarán datos al recibir entradas.
            primerBloqueInicial = secuencial.reservar(bitmap, inicioDatos, totalBloques, 3);
            libres -= 3;
        }

        // Crear el archivo físico.
        crearDisco(nombre, tamañoMB, directorio);

        // Crear la única partición.
        Particion particion = new Particion(
                INICIO_PARTICION_LBA,
                sectores,
                0x7F,
                false);

        // Registrar el MBR.
        MBR mbr = new MBR(particion);
        mbr.escribir(disco);

        // Inicializar el Boot Control Block.
        BootControlBlock boot = new BootControlBlock(false);

        boot.escribir(disco, particion);

        // Crear la información general del volumen.
        VolumeControlBlock vcb = new VolumeControlBlock(
                nombre,
                tamañoBloque,
                estrategia,
                totalBloques,
                libres,
                inicioBitmap,
                bloquesBitmap,
                inicioFCB,
                BLOQUES_FCB,
                inicioDatos);

        // El VCB comienza 512 bytes después del
        // inicio de la partición.
        vcb.escribir(
                disco,
                particion.getInicioBytes() + TAMAÑO_SECTOR);

        long posicionBitmap = particion.getInicioBytes()
                + inicioBitmap * tamañoBloque;

        disco.seek(posicionBitmap);
        disco.write(bitmap);
        if (contraseña != null) {
            inicializarRaiz(vcb, particion.getInicioBytes(), primerBloqueInicial, contraseña);
        }
        disco.getFD().sync();
    }

    private void inicializarRaiz(VolumeControlBlock vcb, long particion,
            long primerBloque, char[] contraseña) throws IOException {
        escribirRegistroInicial(vcb, particion, FCB.RAIZ, FCB.DIRECTORIO, 0755,
                primerBloque, Directorios.codificar(Map.of("root", FCB.HOME_ROOT)));
        escribirRegistroInicial(vcb, particion, FCB.HOME_ROOT, FCB.DIRECTORIO, 0700,
                -1, new byte[0]);
        escribirRegistroInicial(vcb, particion, FCB.USUARIOS, FCB.REGISTRO_INTERNO, 0600,
                primerBloque + 1, Usuarios.crearRoot(contraseña));
        escribirRegistroInicial(vcb, particion, FCB.GRUPOS, FCB.REGISTRO_INTERNO, 0600,
                primerBloque + 2, Grupos.crearRoot());
    }

    private void escribirRegistroInicial(VolumeControlBlock vcb, long particion,
            int id, int tipo, int permisos, long bloque, byte[] contenido) throws IOException {
        int cantidad = contenido.length == 0 ? 0 : 1;
        if (contenido.length > vcb.getTamañoBloque()) {
            throw new IOException("El registro inicial supera el tamaño de bloque");
        }
        if (cantidad > 0) {
            escribir(particion + bloque * vcb.getTamañoBloque(), contenido);
        }
        new FCB(tipo, Usuarios.ROOT, Grupos.ROOT, permisos, bloque, cantidad, contenido.length)
                .escribir(this, vcb, particion, id);
    }

    public MBR leerMBR() throws IOException {
        verificarAbierto();
        return MBR.leer(disco);
    }

    public BootControlBlock leerBootControlBlock()
            throws IOException {

        verificarAbierto();

        Particion particion = leerMBR().getParticion();

        return BootControlBlock.leer(disco, particion);
    }

    public VolumeControlBlock leerVCB()
            throws IOException {

        verificarAbierto();

        Particion particion = leerMBR().getParticion();

        return VolumeControlBlock.leer(
                disco,
                particion.getInicioBytes() + TAMAÑO_SECTOR);
    }

    public byte[] leer(long posicion, int cantidad)
            throws IOException {

        verificarRango(posicion, cantidad);

        byte[] datos = new byte[cantidad];

        disco.seek(posicion);
        disco.readFully(datos);

        return datos;
    }

    public void escribir(long posicion, byte[] datos)
            throws IOException {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "No hay datos para escribir");
        }

        verificarRango(posicion, datos.length);

        disco.seek(posicion);
        disco.write(datos);
    }

    private void verificarAbierto() {
        if (disco == null) {
            throw new IllegalStateException(
                    "No existe un disco abierto");
        }
    }

    private void verificarRango(long posicion, long cantidad) {

        verificarAbierto();

        if (posicion < 0 ||
                cantidad < 0 ||
                posicion > Tamaño ||
                cantidad > Tamaño - posicion) {

            throw new IllegalArgumentException(
                    "Acceso fuera de los limites del disco");
        }
    }

    public String getNombre() {
        return Nombre;
    }

    public long getTamaño() {
        return Tamaño;
    }

    @Override
    public void close() throws IOException {
        if (disco != null) {
            disco.close();
            disco = null;
        }
    }
}
