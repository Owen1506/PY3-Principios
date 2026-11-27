package Almacenamiento;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Grupos {

    public static final int ROOT = 0;

    static byte[] crearRoot() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream datos = new DataOutputStream(bytes)) {
            datos.writeInt(ROOT);
            datos.writeUTF("root");
        }
        return bytes.toByteArray();
    }

    public static String nombre(discoVirtual disco, int id) throws IOException {
        byte[] contenido = FCB.leer(disco, FCB.GRUPOS).leerContenido(disco);
        try (DataInputStream datos = new DataInputStream(new ByteArrayInputStream(contenido))) {
            while (datos.available() > 0) {
                int grupo = datos.readInt();
                String nombre = datos.readUTF();
                if (grupo == id) {
                    return nombre;
                }
            }
        }
        throw new IOException("Grupo inexistente: " + id);
    }
}
