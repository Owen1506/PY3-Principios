package Almacenamiento;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Usuarios {

    public static final int ROOT = 0;
    private static final int ITERACIONES = 600_000;
    private static final int BYTES_SAL = 16;
    private static final int BYTES_HASH = 32;

    public record Cuenta(int id, String nombre, int grupo, int home,
            byte[] sal, byte[] hash) {
        public boolean verificarContraseña(char[] contraseña) throws IOException {
            return MessageDigest.isEqual(hash, derivar(contraseña, sal));
        }
    }

    static byte[] crearRoot(char[] contraseña) throws IOException {
        byte[] sal = new byte[BYTES_SAL];
        new SecureRandom().nextBytes(sal);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream datos = new DataOutputStream(bytes)) {
            datos.writeInt(ROOT);
            datos.writeUTF("root");
            datos.writeInt(Grupos.ROOT);
            datos.writeInt(FCB.HOME_ROOT);
            datos.write(sal);
            datos.write(derivar(contraseña, sal));
        }
        return bytes.toByteArray();
    }

    public static Cuenta buscar(discoVirtual disco, String nombre) throws IOException {
        byte[] contenido = FCB.leer(disco, FCB.USUARIOS).leerContenido(disco);
        try (DataInputStream datos = new DataInputStream(new ByteArrayInputStream(contenido))) {
            while (datos.available() > 0) {
                int id = datos.readInt();
                String usuario = datos.readUTF();
                int grupo = datos.readInt();
                int home = datos.readInt();
                byte[] sal = new byte[BYTES_SAL];
                byte[] hash = new byte[BYTES_HASH];
                datos.readFully(sal);
                datos.readFully(hash);
                if (usuario.equals(nombre)) {
                    return new Cuenta(id, usuario, grupo, home, sal, hash);
                }
            }
        }
        throw new IOException("Usuario inexistente: " + nombre);
    }

    private static byte[] derivar(char[] contraseña, byte[] sal) throws IOException {
        PBEKeySpec parametros = new PBEKeySpec(contraseña, sal, ITERACIONES, BYTES_HASH * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(parametros).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IOException("No se pudo procesar la contraseña", e);
        } finally {
            parametros.clearPassword();
        }
    }
}
