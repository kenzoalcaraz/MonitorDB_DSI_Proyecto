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

import java.net.ServerSocket;
import java.net.Socket;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;

public class ServidorSocket {

    private static final int PUERTO = 5000;

    // =================== AES CONFIG (MISMA QUE ClienteSocket) =====================
    private static String password = "contraseña";
    private static final byte[] SALT = new byte[]{
            (byte) 0x3a, (byte) 0x7f, (byte) 0x12, (byte) 0x98,
            (byte) 0xab, (byte) 0xcd, (byte) 0x55, (byte) 0x66
    };
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGO = "AES/CBC/PKCS5Padding";

    private static SecretKeySpec secretKeySpec = null;
    private static final SecureRandom secureRandom = new SecureRandom();
    // ==============================================================================

    public static void main(String[] args) {

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {

            System.out.println("Servidor escuchando en puerto " + PUERTO + "...");

            while (true) {
                Socket cliente = servidor.accept();
                System.out.println("Nuevo cliente conectado: " + cliente.getInetAddress());
                new Thread(() -> manejarCliente(cliente)).start();
            }

        } catch (Exception e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    // ========================== MANEJO DE CLIENTE ===============================

    private static void manejarCliente(Socket cliente) {

        try (
                BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true)
        ) {

            String linea;

            while ((linea = entrada.readLine()) != null) {

                // ------------------------ DESCIFRAR ------------------------
                String textoPlano = decryptBase64(linea);

                if (textoPlano == null) {
                    System.out.println("Error descifrando mensaje recibido: " + linea);
                    salida.println(encryptAndBase64("ERROR"));
                    continue;
                }

                // ------------------------ IDENTIFICAR TIPO ------------------------
                if (textoPlano.startsWith("QUERY:")) {
                    // Petición de consulta desde el cliente
                    System.out.println("Solicitud de consulta recibida: " + textoPlano);
                    String respuestaPlano = procesarConsulta(textoPlano);
                    if (respuestaPlano == null) respuestaPlano = "ERROR";
                    String cifrado = encryptAndBase64(respuestaPlano);
                    salida.println(cifrado);
                    System.out.println("Datos enviados al cliente (cifrados).");
                    continue;
                }

                // ------------------------ VALIDAR FORMATO DATOS ------------------------
                if (!textoPlano.matches("\\d+,\\d+,\\d+")) {
                    System.out.println("Dato inválido descifrado: " + textoPlano);
                    salida.println(encryptAndBase64("ERROR"));
                    continue;
                }

                String[] partes = textoPlano.split(",");
                int x = Integer.parseInt(partes[0]);
                int y = Integer.parseInt(partes[1]);
                int z = Integer.parseInt(partes[2]);

                // ------------------------ GUARDAR EN BD ------------------------
                ConexionBD.insertarDatos(x, y, z);

                System.out.println("Datos guardados: x=" + x + ", y=" + y + ", z=" + z);
                salida.println(encryptAndBase64("OK"));
            }

        } catch (Exception e) {
            System.out.println("Cliente desconectado.");
        } finally {
            try {
                cliente.close();
            } catch (Exception ignored) {
            }
        }
    }

    // ============================ PROCESAR CONSULTA ===============================
    // Devuelve un String plano con formato: "OK;13,62,61;65,79,45;57,79,83;..."
    private static String procesarConsulta(String consulta) {
        try {
            if (consulta.equals("QUERY:ALL")) {
                List<int[]> rows = ConexionBD.consultarTodos();
                return buildOkResponse(rows);
            } else if (consulta.startsWith("QUERY:DATE:")) {
                String fecha = consulta.substring("QUERY:DATE:".length()).trim(); // yyyy-MM-dd
                List<int[]> rows = ConexionBD.consultarPorFecha(fecha);
                return buildOkResponse(rows);
            } else if (consulta.startsWith("QUERY:DATETIME:")) {
                String fechaHora = consulta.substring("QUERY:DATETIME:".length()).trim(); // yyyy-MM-dd HH:mm:ss
                List<int[]> rows = ConexionBD.consultarDesdeFechaHora(fechaHora);
                return buildOkResponse(rows);
            } else {
                return "ERROR";
            }
        } catch (Exception e) {
            System.out.println("Error procesando consulta: " + e.getMessage());
            return "ERROR";
        }
    }

    private static String buildOkResponse(List<int[]> rows) {
        if (rows == null || rows.isEmpty()) return "OK;"; // OK con cero registros
        StringBuilder sb = new StringBuilder();
        sb.append("OK;");
        boolean first = true;
        for (int[] r : rows) {
            if (!first) sb.append(";");
            sb.append(r[0]).append(",").append(r[1]).append(",").append(r[2]);
            first = false;
        }
        return sb.toString();
    }

    // ============================ CIFRADO / DESCIFRADO ===============================

    private static SecretKeySpec deriveKey() throws Exception {
        if (secretKeySpec != null) return secretKeySpec;

        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGO);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        byte[] keyBytes = tmp.getEncoded();
        secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        return secretKeySpec;
    }

    // Cifra el texto plano y devuelve Base64( IV || CIPHERTEXT )
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
            System.out.println("Error cifrando datos en servidor: " + e.getMessage());
            return null;
        }
    }

    // Recibe Base64( IV || CIPHERTEXT ), lo descifra y devuelve el texto plano.
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
            System.out.println("Error al descifrar en servidor: " + e.getMessage());
            return null;
        }
    }
}
