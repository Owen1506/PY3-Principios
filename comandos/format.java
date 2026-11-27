package comandos;

import Almacenamiento.discoVirtual;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

public class format {

    public static void ejecutar(Scanner entrada, Path archivo, boolean reemplazar)
            throws IOException {
        System.out.println("Tamaño del disco en MiB:");
        if (!entrada.hasNextLine()) {
            return;
        }
        long tamaño = Long.parseLong(entrada.nextLine().trim());

        System.out.println("Tamaño de bloque en bytes (512, 1024, 2048 o 4096):");
        if (!entrada.hasNextLine()) {
            return;
        }
        int bloque = Integer.parseInt(entrada.nextLine().trim());

        if (!elegirEstrategia(entrada)) {
            return;
        }
        char[] contraseña = leerContraseña(entrada, "Contraseña de root");
        if (contraseña == null) {
            return;
        }
        try {
            if (contraseña.length == 0) {
                throw new IllegalArgumentException("La contraseña de root es obligatoria");
            }
            char[] confirmación = leerContraseña(entrada, "Confirma la contraseña de root");
            if (confirmación == null) {
                return;
            }
            try {
                if (!Arrays.equals(contraseña, confirmación)) {
                    throw new IllegalArgumentException("Las contraseñas no coinciden");
                }
                discoVirtual.formatear(archivo, tamaño, bloque, contraseña, reemplazar);
                System.out.println(archivo + " formateado correctamente. HOME de root: /root.");
            } finally {
                Arrays.fill(confirmación, '\0');
            }
        } finally {
            Arrays.fill(contraseña, '\0');
        }
    }

    private static boolean elegirEstrategia(Scanner entrada) {
        System.out.println("Estrategia de asignación");
        System.out.println("1. Secuencial");
        System.out.println("2. Enlazada (pendiente)");
        System.out.println("3. Indexada (pendiente)");
        while (entrada.hasNextLine()) {
            switch (entrada.nextLine().trim()) {
                case "1":
                    return true;
                case "2", "3":
                    System.out.println("Esa estrategia aún no está implementada. Elige 1.");
                    break;
                default:
                    System.out.println("Opción inválida. Escribe 1, 2 o 3.");
            }
        }
        return false;
    }

    private static char[] leerContraseña(Scanner entrada, String mensaje) {
        Console consola = System.console();
        if (consola != null && consola.isTerminal()) {
            return consola.readPassword("%s: ", mensaje);
        }
        System.out.println(mensaje + ":");
        return entrada.hasNextLine() ? entrada.nextLine().toCharArray() : null;
    }
}
