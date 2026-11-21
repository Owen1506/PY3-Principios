package Almacenamiento;

public class Main {

    public static void main(String[] args) {

        try (discoVirtual disco = new discoVirtual()) {

            disco.formatear(
                    "prueba.fs",
                    10,//representa el tamaño del disco en MB
                    512,//representa el tamaño del bloque en bytes
                    3);//representa el número de particiones

            System.out.println("Disco creado correctamente");

            VolumeControlBlock vcb = disco.leerVCB();

            System.out.println("Nombre: " +
                    vcb.getNombreFS());

            System.out.println("Tamaño de bloque: " +
                    vcb.getTamañoBloque());

            System.out.println("Total de bloques: " +
                    vcb.getTotalBloques());

            System.out.println("Bloques libres: " +
                    vcb.getBloquesLibres());
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
