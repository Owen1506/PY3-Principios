package Almacenamiento;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class VolumeControlBlock {

    private static final int MAGIC = 0x4D494653; // MIFS
    private static final int VERSION = 1;

    private String nombreFS;
    private int tamañoBloque;
    private int estrategiaAsignacion;

    private long totalBloques;
    private long bloquesLibres;

    private long inicioBitmap;
    private long cantidadBloquesBitmap;

    private long inicioFCB;
    private long cantidadBloquesFCB;

    private long inicioDatos;

    public VolumeControlBlock(
            String nombreFS,
            int tamañoBloque,
            int estrategiaAsignacion,
            long totalBloques,
            long bloquesLibres,
            long inicioBitmap,
            long cantidadBloquesBitmap,
            long inicioFCB,
            long cantidadBloquesFCB,
            long inicioDatos) {

        this.nombreFS = nombreFS;
        this.tamañoBloque = tamañoBloque;
        this.estrategiaAsignacion = estrategiaAsignacion;
        this.totalBloques = totalBloques;
        this.bloquesLibres = bloquesLibres;
        this.inicioBitmap = inicioBitmap;
        this.cantidadBloquesBitmap = cantidadBloquesBitmap;
        this.inicioFCB = inicioFCB;
        this.cantidadBloquesFCB = cantidadBloquesFCB;
        this.inicioDatos = inicioDatos;
    }

    public String getNombreFS() {
        return nombreFS;
    }

    public int getTamañoBloque() {
        return tamañoBloque;
    }

    public int getEstrategiaAsignacion() {
        return estrategiaAsignacion;
    }

    public long getTotalBloques() {
        return totalBloques;
    }

    public long getBloquesLibres() {
        return bloquesLibres;
    }

    public long getInicioBitmap() {
        return inicioBitmap;
    }

    public long getCantidadBloquesBitmap() {
        return cantidadBloquesBitmap;
    }

    public long getInicioFCB() {
        return inicioFCB;
    }

    public long getCantidadBloquesFCB() {
        return cantidadBloquesFCB;
    }

    public long getInicioDatos() {
        return inicioDatos;
    }

    public void escribir(RandomAccessFile disco,
            long posicion)
            throws IOException {

        // byte[] datos = new byte[tamañoBloque];
        byte[] datos = new byte[512];
        ByteBuffer buffer = ByteBuffer.wrap(datos);

        buffer.putInt(MAGIC);
        buffer.putInt(VERSION);

        buffer.putInt(tamañoBloque);
        buffer.putInt(estrategiaAsignacion);

        buffer.putLong(totalBloques);
        buffer.putLong(bloquesLibres);

        buffer.putLong(inicioBitmap);
        buffer.putLong(cantidadBloquesBitmap);

        buffer.putLong(inicioFCB);
        buffer.putLong(cantidadBloquesFCB);

        buffer.putLong(inicioDatos);

        // Nombre fijo de 64 bytes.
        byte[] nombre = nombreFS.getBytes(
                StandardCharsets.UTF_8);

        if (nombre.length > 64) {
            throw new IOException(
                    "El nombre del File System es demasiado largo");
        }

        byte[] campoNombre = new byte[64];

        System.arraycopy(nombre, 0, campoNombre,
                0, nombre.length);

        buffer.put(campoNombre);

        disco.seek(posicion);
        disco.write(datos);
    }

    public static VolumeControlBlock leer(
            RandomAccessFile disco,
            long posicion) throws IOException {

        byte[] datos = new byte[512];

        disco.seek(posicion);
        disco.readFully(datos);

        ByteBuffer buffer = ByteBuffer.wrap(datos);

        if (buffer.getInt() != MAGIC) {
            throw new IOException("VCB invalido");
        }

        if (buffer.getInt() != VERSION) {
            throw new IOException(
                    "Version del File System incompatible");
        }

        int tamañoBloque = buffer.getInt();
        int estrategia = buffer.getInt();

        long total = buffer.getLong();
        long libres = buffer.getLong();

        long bitmap = buffer.getLong();
        long bloquesBitmap = buffer.getLong();

        long fcb = buffer.getLong();
        long bloquesFCB = buffer.getLong();

        long datosInicio = buffer.getLong();

        byte[] nombreBytes = new byte[64];
        buffer.get(nombreBytes);

        int longitud = 0;

        while (longitud < nombreBytes.length &&
                nombreBytes[longitud] != 0) {
            longitud++;
        }

        String nombreFS = new String(
                nombreBytes, 0, longitud,
                StandardCharsets.UTF_8);

        return new VolumeControlBlock(
                nombreFS,
                tamañoBloque,
                estrategia,
                total,
                libres,
                bitmap,
                bloquesBitmap,
                fcb,
                bloquesFCB,
                datosInicio);
    }
}
