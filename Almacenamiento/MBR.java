package Almacenamiento;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MBR {

    public static final int TAMAÑO = 512;

    private Particion particion;

    public MBR(Particion particion) {
        this.particion = particion;
    }

    public Particion getParticion() {
        return particion;
    }

    public void escribir(RandomAccessFile disco)
            throws IOException {

        byte[] datos = new byte[TAMAÑO];

        // Primera entrada de la tabla de particiones.
        int posicion = 446;

        datos[posicion] = (byte)
                (particion.isActiva() ? 0x80 : 0x00);

        // Los campos CHS quedan sin utilizar.
        datos[posicion + 4] =
                (byte) particion.getTipo();

        ByteBuffer buffer = ByteBuffer.wrap(datos);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(posicion + 8,
                (int) particion.getInicioLBA());

        buffer.putInt(posicion + 12,
                (int) particion.getCantidadSectores());

        // Firma del MBR.
        datos[510] = 0x55;
        datos[511] = (byte) 0xAA;

        disco.seek(0);
        disco.write(datos);
    }

    public static MBR leer(RandomAccessFile disco)
            throws IOException {

        byte[] datos = new byte[TAMAÑO];

        disco.seek(0);
        disco.readFully(datos);

        if ((datos[510] & 0xFF) != 0x55 ||
            (datos[511] & 0xFF) != 0xAA) {

            throw new IOException("MBR invalido");
        }

        int posicion = 446;

        ByteBuffer buffer = ByteBuffer.wrap(datos);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        long inicio = Integer.toUnsignedLong(
                buffer.getInt(posicion + 8));

        long cantidad = Integer.toUnsignedLong(
                buffer.getInt(posicion + 12));

        int tipo = datos[posicion + 4] & 0xFF;
        boolean activa = datos[posicion] == (byte) 0x80;

        Particion particion = new Particion(
                inicio, cantidad, tipo, activa);

        return new MBR(particion);
    }
}
