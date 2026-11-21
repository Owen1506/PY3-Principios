package Almacenamiento;

public class Particion {

    private long inicioLBA;// LBA de inicio de la particion lba es 
    private long cantidadSectores;
    private int tipo;
    private boolean activa;

    public Particion(long inicioLBA, long cantidadSectores,
            int tipo, boolean activa) {

        if (inicioLBA < 1 || cantidadSectores <= 0) {
            throw new IllegalArgumentException(
                    "La particion tiene posiciones invalidas");
        }

        this.inicioLBA = inicioLBA;
        this.cantidadSectores = cantidadSectores;
        this.tipo = tipo;
        this.activa = activa;
    }

    public long getInicioLBA() {
        return inicioLBA;
    }

    public long getCantidadSectores() {
        return cantidadSectores;
    }

    public int getTipo() {
        return tipo;
    }

    public boolean isActiva() {
        return activa;
    }

    public long getInicioBytes() {
        return inicioLBA * 512L;
    }

    public long getTamañoBytes() {
        return cantidadSectores * 512L;
    }

    public long getFinBytes() {
        return getInicioBytes() + getTamañoBytes();
    }
}
