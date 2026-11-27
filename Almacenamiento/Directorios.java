package Almacenamiento;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Directorios {

    static byte[] codificar(Map<String, Integer> entradas) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream datos = new DataOutputStream(bytes)) {
            for (var entrada : entradas.entrySet()) {
                datos.writeUTF(entrada.getKey());
                datos.writeInt(entrada.getValue());
            }
        }
        return bytes.toByteArray();
    }

    public static Map<String, Integer> leer(discoVirtual disco, int id) throws IOException {
        FCB directorio = FCB.leer(disco, id);
        if (directorio.tipo() != FCB.DIRECTORIO) {
            throw new IOException("El FCB indicado no es un directorio");
        }
        Map<String, Integer> entradas = new LinkedHashMap<>();
        try (DataInputStream datos = new DataInputStream(
                new ByteArrayInputStream(directorio.leerContenido(disco)))) {
            while (datos.available() > 0) {
                entradas.put(datos.readUTF(), datos.readInt());
            }
        }
        return entradas;
    }
}
