package Almacenamiento;

import java.io.IOException;
import java.io.RandomAccessFile;
/* Boot Control Block */
public class BootControlBlock {

    private boolean arrancable;

    public BootControlBlock(boolean arrancable) {
        this.arrancable = arrancable;
    }

    public boolean isArrancable() {
        return arrancable;
    }

    public void escribir(RandomAccessFile disco,
            Particion particion)
            throws IOException {

        byte[] datos = new byte[512];

        // Identificador propio del proyecto.
        datos[0] = 'B';
        datos[1] = 'C';
        datos[2] = 'B';
        datos[3] = '1';

        datos[4] = (byte) (arrancable ? 1 : 0);

        // Firma del sector.
        datos[510] = 0x55;
        datos[511] = (byte) 0xAA;

        disco.seek(particion.getInicioBytes());
        disco.write(datos);
    }

    public static BootControlBlock leer(
            RandomAccessFile disco,
            Particion particion) throws IOException {

        byte[] datos = new byte[512];

        disco.seek(particion.getInicioBytes());
        disco.readFully(datos);

        if (datos[0] != 'B' || datos[1] != 'C' ||
                datos[2] != 'B' || datos[3] != '1') {

            throw new IOException(
                    "Boot Control Block invalido");
        }

        if ((datos[510] & 0xFF) != 0x55 ||
                (datos[511] & 0xFF) != 0xAA) {

            throw new IOException(
                    "Firma de arranque invalida");
        }

        return new BootControlBlock(datos[4] == 1);
    }
}
