package Almacenamiento;

import java.io.IOException;
import java.nio.ByteBuffer;

public record FCB(int tipo, int propietario, int grupo, int permisos,
        long primerBloque, int cantidadBloques, int longitud) {

    public static final int RAIZ = 0;
    public static final int HOME_ROOT = 1;
    public static final int USUARIOS = 2;
    public static final int GRUPOS = 3;
    public static final int DIRECTORIO = 1;
    public static final int REGISTRO_INTERNO = 2;
    private static final int FIRMA = 0x46434231;
    private static final int TAM_REGISTRO = 64;

    void escribir(discoVirtual disco, VolumeControlBlock volumen, long particion, int id)
            throws IOException {
        ByteBuffer datos = ByteBuffer.allocate(TAM_REGISTRO);
        datos.putInt(FIRMA).putInt(tipo).putInt(propietario).putInt(grupo)
                .putInt(permisos).putLong(primerBloque).putInt(cantidadBloques).putInt(longitud);
        disco.escribir(posicion(volumen, particion, id), datos.array());
    }

    public static FCB leer(discoVirtual disco, int id) throws IOException {
        VolumeControlBlock volumen = disco.leerVCB();
        long particion = disco.leerMBR().getParticion().getInicioBytes();
        ByteBuffer datos = ByteBuffer.wrap(disco.leer(posicion(volumen, particion, id), TAM_REGISTRO));
        if (datos.getInt() != FIRMA) {
            throw new IOException("FCB inexistente o inválido: " + id);
        }
        return new FCB(datos.getInt(), datos.getInt(), datos.getInt(), datos.getInt(),
                datos.getLong(), datos.getInt(), datos.getInt());
    }

    public byte[] leerContenido(discoVirtual disco) throws IOException {
        if (cantidadBloques == 0 && longitud == 0) {
            return new byte[0];
        }
        VolumeControlBlock volumen = disco.leerVCB();
        long particion = disco.leerMBR().getParticion().getInicioBytes();
        if (cantidadBloques <= 0 || longitud < 0 || primerBloque < volumen.getInicioDatos()
                || primerBloque > volumen.getTotalBloques() - cantidadBloques
                || longitud > (long) cantidadBloques * volumen.getTamañoBloque()) {
            throw new IOException("El FCB contiene un rango de datos inválido");
        }
        return disco.leer(particion + primerBloque * volumen.getTamañoBloque(), longitud);
    }

    private static long posicion(VolumeControlBlock volumen, long particion, int id) {
        long desplazamiento = (long) id * TAM_REGISTRO;
        if (id < 0 || desplazamiento >= volumen.getCantidadBloquesFCB() * volumen.getTamañoBloque()) {
            throw new IllegalArgumentException("Identificador de FCB fuera de rango");
        }
        return particion + volumen.getInicioFCB() * volumen.getTamañoBloque() + desplazamiento;
    }
}
