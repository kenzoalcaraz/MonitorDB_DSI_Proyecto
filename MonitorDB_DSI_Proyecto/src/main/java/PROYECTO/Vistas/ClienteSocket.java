package PROYECTO.Vistas;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class ClienteSocket {

    // NOTA: este ClienteSocket mantiene el método enviarDatos (usa conexión persistente)
    // para el envío en tiempo real. Para consultas (histórico) usaremos enviarConsulta
    // que abre una conexión temporal y no interfiere con la conexión persistente.

    private static Socket socket;
    private static PrintWriter salida;

    // Host y puerto del servidor
    private static final String HOST = "127.0.0.1";
    private static final int PUERTO = 5000;

    // ---------------- AES CONFIG (modifiable) ----------------
    private static String password = "contraseña";

    private static final byte[] SALT = new byte[] {
            (byte)0x3a, (byte)0x7f, (byte)0x12, (byte)0x98,
            (byte)0xab, (byte)0xcd, (byte)0x55, (byte)0x66
    };

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits
    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGO = "AES/CBC/PKCS5Padding";

    private static SecretKeySpec secretKeySpec = null;

    private static final SecureRandom secureRandom = new SecureRandom();
    // -------------------------------------------------------

    public static void setPassword(String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) return;
        password = newPassword;
        secretKeySpec = null;
    }

    private static void conectar() {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket(HOST, PUERTO);
                salida = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("Cliente conectado al servidor.");
            }
        } catch (Exception e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    public static void enviarDatos(int x, int y, int z) {
        try {
            if (socket == null || socket.isClosed()) {
                conectar();
                if (socket == null) return;
            }

            String mensajePlano = x + "," + y + "," + z;
            String cifradoBase64 = encryptAndBase64(mensajePlano);

            if (cifradoBase64 != null) {
                salida.println(cifradoBase64);
            } else {
                System.out.println("Error: no se pudo cifrar el mensaje.");
            }

        } catch (Exception e) {
            System.out.println("Error enviando datos: " + e.getMessage());
            cerrar();
        }
    }

    // Enviar una consulta al servidor y devolver la respuesta (texto plano).
    // Esta función abre una conexión temporal, envía la consulta (cifrada),
    // lee una única línea de respuesta (cifrada), la descifra y la devuelve.
    public static String enviarConsulta(String mensajePlano) {
        try (Socket s = new Socket(HOST, PUERTO);
             PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))
        ) {
            String cifrado = encryptAndBase64(mensajePlano);
            if (cifrado == null) return null;

            pw.println(cifrado);

            // Esperar respuesta (una línea)
            String respuestaCifrada = br.readLine();
            if (respuestaCifrada == null) return null;

            String respuestaPlano = decryptBase64(respuestaCifrada);
            return respuestaPlano;

        } catch (Exception e) {
            System.out.println("Error en enviarConsulta: " + e.getMessage());
            return null;
        }
    }

    public static void cerrar() {
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        socket = null;
        salida = null;
    }

    // ------------------ CIFRADO AUXILIAR --------------------

    private static SecretKeySpec deriveKey() throws Exception {
        if (secretKeySpec != null) return secretKeySpec;

        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGO);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        byte[] keyBytes = tmp.getEncoded();
        secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        return secretKeySpec;
    }

    private static String encryptAndBase64(String plain) {
        try {
            SecretKeySpec key = deriveKey();

            // Generar IV de 16 bytes
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] encrypted = cipher.doFinal(plain.getBytes("UTF-8"));

            // Prepend IV
            byte[] ivAndCipher = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
            System.arraycopy(encrypted, 0, ivAndCipher, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(ivAndCipher);

        } catch (Exception e) {
            System.out.println("Error cifrando datos: " + e.getMessage());
            return null;
        }
    }

    // Descifra Base64( IV || CIPHERTEXT ) y devuelve el texto plano
    private static String decryptBase64(String base64) {
        try {
            byte[] ivAndCipher = Base64.getDecoder().decode(base64);

            // IV = primeros 16 bytes
            byte[] iv = new byte[16];
            System.arraycopy(ivAndCipher, 0, iv, 0, 16);

            // Ciphertext = resto
            byte[] cipherText = new byte[ivAndCipher.length - 16];
            System.arraycopy(ivAndCipher, 16, cipherText, 0, cipherText.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec key = deriveKey();

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            byte[] decrypted = cipher.doFinal(cipherText);

            return new String(decrypted, "UTF-8");

        } catch (Exception e) {
            System.out.println("Error al descifrar datos: " + e.getMessage());
            return null;
        }
    }
}
