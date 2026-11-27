package Almacenamiento;

import comandos.format;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    private static final Path DISCO = Path.of("disco.fs");

    public static void main(String[] args) {
        Scanner entrada = new Scanner(System.in);
        try {
            if (Files.notExists(DISCO)) {
                System.out.println("No existe configuración de disco.");
                System.out.println("El primer paso es ejecutar el comando format.");
                System.out.print("Escribe format para crear disco.fs. : ");
                if (entrada.hasNextLine() && entrada.nextLine().trim().equalsIgnoreCase("format")) {
                    format.ejecutar(entrada, DISCO, false);
                }
                return;
            }

            System.out.println("Se va a cargar la información de disco.fs.");
            try (discoVirtual disco = new discoVirtual()) {
                disco.abrirDisco(DISCO.toString());
                mostrarConfiguracion(disco);
                if (!solicitarCambio(entrada)) {
                    return;
                }
            }
            format.ejecutar(entrada, DISCO, true);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static boolean solicitarCambio(Scanner entrada) {
        System.out.println("1. Continuar");
        System.out.println("2. Cambiar Configuración");
        while (entrada.hasNextLine()) {
            switch (entrada.nextLine().trim()) {
                case "1":
                    System.out.println("disco.fs cargado correctamente.");
                    return false;
                case "2":
                    if (confirmarFormato(entrada)) {
                        return true;
                    }
                    System.out.println("Operación cancelada. Se conservó disco.fs.");
                    return false;
                default:
                    System.out.println("Opción inválida. Escribe 1 o 2.");
            }
        }
        return false;
    }

    private static void mostrarConfiguracion(discoVirtual disco) throws IOException {
        VolumeControlBlock vcb = disco.leerVCB();
        String estrategia = switch (vcb.getEstrategiaAsignacion()) {
            case 1 -> "Secuencial";
            case 2 -> "Enlazada";
            case 3 -> "Indexada";
            default -> "Desconocida (" + vcb.getEstrategiaAsignacion() + ")";
        };
        System.out.println("Nombre del volumen: " + vcb.getNombreFS());
        System.out.println("Capacidad del disco: " + disco.getTamaño() / (1024 * 1024) + " MiB");
        System.out.println("Tamaño de bloque: " + vcb.getTamañoBloque() + " bytes");
        System.out.println("Total de bloques: " + vcb.getTotalBloques());
        System.out.println("Estrategia de asignación: " + estrategia);
    }

    private static boolean confirmarFormato(Scanner entrada) {
        System.out.println("Esta opción ejecutará format y borrará el contenido de disco.fs.");
        System.out.println("¿Deseas continuar? (s/n)");
        while (entrada.hasNextLine()) {
            switch (entrada.nextLine().trim().toLowerCase(java.util.Locale.ROOT)) {
                case "s", "sí", "si":
                    return true;
                case "n", "no":
                    return false;
                default:
                    System.out.println("Escribe s para confirmar o n para cancelar.");
            }
        }
        return false;
    }
}
