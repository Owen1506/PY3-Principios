package Almacenamiento;
import java.io.RandomAccessFile;
import java.io.IOException;

public class discoVirtual {

    private String Nombre;
    private long Tamaño;
    private RandomAccessFile disco;

public void crearDisco(String Nombre, long TamañoMB)
        throws IOException {

    this.Nombre = Nombre;
    this.Tamaño = TamañoMB * 1024L * 1024;

    this.disco = new RandomAccessFile(Nombre, "rw");
    this.disco.setLength(this.Tamaño);
}
}
