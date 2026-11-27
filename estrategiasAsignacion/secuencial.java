package estrategiasAsignacion;

import java.io.IOException;

public class secuencial {

    public static long reservar(byte[] mapa, long inicioDatos, long total, int cantidad)
            throws IOException {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La reserva debe contener al menos un bloque");
        }
        int seguidos = 0;
        for (long bloque = inicioDatos; bloque < total; bloque++) {
            seguidos = ocupado(mapa, bloque) ? 0 : seguidos + 1;
            if (seguidos == cantidad) {
                long inicio = bloque - cantidad + 1;
                marcar(mapa, inicio, cantidad, true);
                return inicio;
            }
        }
        throw new IOException("No hay suficientes bloques contiguos");
    }

    public static void liberar(byte[] mapa, long inicioDatos, long total,
            long inicio, int cantidad) {
        if (inicio < inicioDatos || cantidad <= 0 || inicio > total - cantidad) {
            throw new IllegalArgumentException("La liberación está fuera del área de datos");
        }
        for (long bloque = inicio; bloque < inicio + cantidad; bloque++) {
            if (!ocupado(mapa, bloque)) {
                throw new IllegalArgumentException("El rango contiene bloques libres");
            }
        }
        marcar(mapa, inicio, cantidad, false);
    }

    public static boolean ocupado(byte[] mapa, long bloque) {
        return (mapa[(int) (bloque / 8)] & mascara(bloque)) != 0;
    }

    public static void marcar(byte[] mapa, long inicio, long cantidad, boolean ocupado) {
        for (long bloque = inicio; bloque < inicio + cantidad; bloque++) {
            int indice = (int) (bloque / 8);
            if (ocupado) {
                mapa[indice] |= mascara(bloque);
            } else {
                mapa[indice] &= ~mascara(bloque);
            }
        }
    }

    private static int mascara(long bloque) {
        return 0x80 >>> (bloque % 8);
    }
}
