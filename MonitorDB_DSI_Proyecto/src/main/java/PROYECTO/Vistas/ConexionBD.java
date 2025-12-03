package PROYECTO.Vistas;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConexionBD {

    // ===============================================================
    //              RUTA PORTABLE DE LA BASE DE DATOS
    // ===============================================================

    // Directorio donde está ejecutándose el proyecto
    private static final String PROJECT_DIR = System.getProperty("user.dir");

    // Carpeta que contiene la BD
    private static final String DB_FOLDER = PROJECT_DIR + File.separator + "database";

    // Archivo SQLite final
    private static final String DB_PATH = DB_FOLDER + File.separator + "monitorBD.db";

    // URL de conexión
    private static final String URL = "jdbc:sqlite:" + DB_PATH;


    // ===============================================================
    //                  CREAR TABLA AUTOMÁTICAMENTE
    // ===============================================================
    static {
        try {
            // Crear carpeta si no existe
            File carpeta = new File(DB_FOLDER);
            if (!carpeta.exists()) carpeta.mkdirs();

            Class.forName("org.sqlite.JDBC");

            try (Connection conn = DriverManager.getConnection(URL)) {

                // ==========================================================
                //    TABLA REQUERIDA POR EL PDF "datos_sensor"
                // ==========================================================
                String sql = "CREATE TABLE IF NOT EXISTS datos_sensor (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "x INTEGER NOT NULL," +
                        "y INTEGER NOT NULL," +
                        "z INTEGER NOT NULL," +
                        "fecha_de_captura TEXT NOT NULL DEFAULT (date('now','localtime'))," +
                        "hora_de_captura TEXT NOT NULL DEFAULT (time('now','localtime'))" +
                        ")";

                conn.createStatement().execute(sql);
            }

            System.out.println("Base de datos lista en: " + DB_PATH);

        } catch (Exception e) {
            System.out.println("Error inicializando BD: " + e.getMessage());
        }
    }


    // ===============================================================
    //                      INSERTAR NUEVOS DATOS
    // ===============================================================
    public static void insertarDatos(int x, int y, int z) {

        // En opción A NO pasamos fecha/hora: SQLite las genera
        String sql = "INSERT INTO datos_sensor (x, y, z) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error guardando datos en BD: " + e.getMessage());
        }
    }


    // ===============================================================
    //                 CONSULTAR TODOS LOS DATOS
    // ===============================================================
    public static List<int[]> consultarTodos() {

        List<int[]> lista = new ArrayList<>();

        String sql = "SELECT x, y, z FROM datos_sensor ORDER BY id ASC";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                int[] valores = new int[3];
                valores[0] = rs.getInt("x");
                valores[1] = rs.getInt("y");
                valores[2] = rs.getInt("z");

                lista.add(valores);
            }

        } catch (Exception e) {
            System.out.println("Error consultando todos los datos: " + e.getMessage());
        }

        return lista;
    }


    // ===============================================================
    //         CONSULTAR SOLO POR FECHA (yyyy-MM-dd)
    // ===============================================================
    public static List<int[]> consultarPorFecha(String fecha) {

        List<int[]> lista = new ArrayList<>();

        String sql = "SELECT x, y, z FROM datos_sensor " +
                "WHERE fecha_de_captura = ? " +
                "ORDER BY hora_de_captura ASC";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fecha); // EXACT match a la fecha

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int[] valores = new int[3];
                    valores[0] = rs.getInt("x");
                    valores[1] = rs.getInt("y");
                    valores[2] = rs.getInt("z");

                    lista.add(valores);
                }
            }

        } catch (Exception e) {
            System.out.println("Error consultando por fecha: " + e.getMessage());
        }

        return lista;
    }


    // ===============================================================
    //     CONSULTAR DESDE FECHA Y HORA (yyyy-MM-dd HH:mm:ss)
    // ===============================================================
    public static List<int[]> consultarDesdeFechaHora(String fechaHora) {

        List<int[]> lista = new ArrayList<>();

        // Separamos fecha y hora del filtro
        // Ejemplo de entrada: "2025-01-15 14:30:00"
        String fecha = fechaHora.substring(0, 10);
        String hora = fechaHora.substring(11);

        String sql =
                "SELECT x, y, z FROM datos_sensor " +
                        "WHERE fecha_de_captura > ? " +               // días posteriores
                        "   OR (fecha_de_captura = ? AND hora_de_captura >= ?) " +
                        "ORDER BY fecha_de_captura ASC, hora_de_captura ASC";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fecha); // fecha >
            ps.setString(2, fecha); // fecha =
            ps.setString(3, hora);  // hora >=

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int[] valores = new int[3];
                    valores[0] = rs.getInt("x");
                    valores[1] = rs.getInt("y");
                    valores[2] = rs.getInt("z");

                    lista.add(valores);
                }
            }

        } catch (Exception e) {
            System.out.println("Error consultando desde fecha y hora: " + e.getMessage());
        }

        return lista;
    }
}
