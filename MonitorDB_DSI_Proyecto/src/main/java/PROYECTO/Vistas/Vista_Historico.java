package PROYECTO.Vistas;

import PROYECTO.Estilos.RoundedBorder;
import PROYECTO.Estilos.TemaUnison;

import javax.swing.*;
import java.awt.*;
import java.awt.RenderingHints;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Vista_Historico {

    private JPanel JPanelHistorico;
    private JLabel JLabelHistorico;
    private JTextField textFieldFecha;
    private JButton BuscarButton;
    private JPanel JPanelGrafica;
    private JButton ActualizarButton;
    private JLabel CargandoLabel;
    private JButton regresarButton;
    private JTextField textFieldHora;
    private JLabel FechaLabel;
    private JLabel HoraLabel;

    private XYSeries seriesX = new XYSeries("X");
    private XYSeries seriesY = new XYSeries("Y");
    private XYSeries seriesZ = new XYSeries("Z");

    public Vista_Historico() {

        aplicarTema();
        inicializarGrafica();

        BuscarButton.addActionListener(e -> cargarDatosFiltrados());
        ActualizarButton.addActionListener(e -> cargarTodosLosDatos());

        regresarButton.addActionListener(e -> {
            Container parent = JPanelHistorico.getParent();
            CardLayout cl = (CardLayout) parent.getLayout();
            cl.show(parent, "Inicio");
        });
    }

    // ============================================================
    //                   APLICAR TEMA UNISON
    // ============================================================
    private void aplicarTema() {

        JPanelHistorico.setBackground(TemaUnison.AZUL_OSCURO);

        JLabelHistorico.setFont(TemaUnison.FUENTE_TITULO);
        JLabelHistorico.setForeground(Color.WHITE);

        FechaLabel.setFont(TemaUnison.FUENTE_TEXTO);
        FechaLabel.setForeground(Color.WHITE);

        HoraLabel.setFont(TemaUnison.FUENTE_TEXTO);
        HoraLabel.setForeground(Color.WHITE);

        CargandoLabel.setFont(TemaUnison.FUENTE_TEXTO);
        CargandoLabel.setForeground(Color.WHITE);

        textFieldFecha.setFont(TemaUnison.FUENTE_TEXTO);
        textFieldFecha.setBorder(new RoundedBorder(4));
        textFieldFecha.setBackground(Color.WHITE);

        textFieldHora.setFont(TemaUnison.FUENTE_TEXTO);
        textFieldHora.setBorder(new RoundedBorder(4));
        textFieldHora.setBackground(Color.WHITE);

        BuscarButton.setFont(TemaUnison.FUENTE_TEXTO);
        BuscarButton.setBackground(TemaUnison.DORADO);
        BuscarButton.setForeground(Color.BLACK);
        BuscarButton.setBorder(new RoundedBorder(4));

        ActualizarButton.setFont(TemaUnison.FUENTE_TEXTO);
        ActualizarButton.setBackground(TemaUnison.DORADO_OSCURO);
        ActualizarButton.setForeground(Color.BLACK);
        ActualizarButton.setBorder(new RoundedBorder(4));

        regresarButton.setFont(TemaUnison.FUENTE_TEXTO);
        regresarButton.setBackground(TemaUnison.DORADO);
        regresarButton.setForeground(Color.BLACK);
        regresarButton.setBorder(new RoundedBorder(4));

        JPanelGrafica.setBackground(Color.WHITE);
    }

    // ============================================================
    //                   CONFIGURAR GRÁFICA
    // ============================================================
    private void inicializarGrafica() {

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(seriesX);
        dataset.addSeries(seriesY);
        dataset.addSeries(seriesZ);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Histórico de datos",
                "Tiempo",
                "Valor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // ===== SUAVIZADO / ANTI-ALIASING JFREECHART =====
        chart.setAntiAlias(true);
        chart.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        chart.getRenderingHints().put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        chart.getRenderingHints().put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        chart.getRenderingHints().put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // ===== FONDO BLANCO PROFESIONAL =====
        chart.setBackgroundPaint(Color.WHITE);
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.getPlot().setOutlineVisible(false);
        chart.getXYPlot().setRangeGridlinePaint(new Color(220, 220, 220));
        chart.getXYPlot().setDomainGridlinePaint(new Color(220, 220, 220));

        // ===== ESTILO =====
        chart.getTitle().setFont(TemaUnison.FUENTE_TITULO);
        chart.getLegend().setItemFont(TemaUnison.FUENTE_TEXTO);
        chart.getXYPlot().getDomainAxis().setLabelFont(TemaUnison.FUENTE_TEXTO);
        chart.getXYPlot().getRangeAxis().setLabelFont(TemaUnison.FUENTE_TEXTO);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));

        JPanelGrafica.setLayout(new BorderLayout());
        JPanelGrafica.add(chartPanel, BorderLayout.CENTER);
    }

    // ============================================================
//               CONSULTA FILTRADA (FECHA + HORA) - via servidor
// ============================================================
    private void cargarDatosFiltrados() {

        String fecha = textFieldFecha.getText().trim();
        String hora = textFieldHora.getText().trim();

        if (!hora.isEmpty() && fecha.isEmpty()) {
            JOptionPane.showMessageDialog(JPanelHistorico,
                    "Debe introducir una fecha si va a usar una hora.");
            return;
        }

        if (fecha.isEmpty()) {
            JOptionPane.showMessageDialog(JPanelHistorico,
                    "Introduzca al menos la fecha.");
            return;
        }

        String filtroFechaHora = fecha + (hora.isEmpty() ? "" : " " + hora);

        mostrarMensajeCarga(true);

        new Thread(() -> {

            limpiarSeries();

            // Preparar mensaje QUERY
            String consulta;
            if (hora.isEmpty()) {
                // Solo fecha
                consulta = "QUERY:DATE:" + fecha;
            } else {
                // Fecha + hora
                consulta = "QUERY:DATETIME:" + filtroFechaHora;
            }

            String respuestaPlano = ClienteSocket.enviarConsulta(consulta);

            if (respuestaPlano == null) {
                SwingUtilities.invokeLater(() -> {
                    mostrarMensajeCarga(false);
                    JOptionPane.showMessageDialog(JPanelHistorico,
                            "Error al solicitar datos al servidor.");
                });
                return;
            }

            // Respuesta esperada: "OK;13,62,61;65,79,45;..."
            if (!respuestaPlano.startsWith("OK")) {
                SwingUtilities.invokeLater(() -> {
                    mostrarMensajeCarga(false);
                    JOptionPane.showMessageDialog(JPanelHistorico,
                            "Respuesta inválida del servidor.");
                });
                return;
            }

            // Extraer registros
            String payload = respuestaPlano.length() > 3 ? respuestaPlano.substring(3) : ""; // after "OK;"
            if (payload.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    mostrarMensajeCarga(false);
                    JOptionPane.showMessageDialog(JPanelHistorico, "No se encontraron datos.");
                });
                return;
            }

            String[] registros = payload.split(";");
            int t = 0;
            for (String reg : registros) {
                if (reg.trim().isEmpty()) continue;
                String[] vals = reg.split(",");
                if (vals.length != 3) continue;
                try {
                    final int x = Integer.parseInt(vals[0]);
                    final int y = Integer.parseInt(vals[1]);
                    final int z = Integer.parseInt(vals[2]);
                    final int tiempo = t++;
                    SwingUtilities.invokeLater(() -> {
                        seriesX.add(tiempo, x);
                        seriesY.add(tiempo, y);
                        seriesZ.add(tiempo, z);
                    });
                } catch (NumberFormatException ignored) {
                }
            }

            SwingUtilities.invokeLater(() -> mostrarMensajeCarga(false));

        }).start();
    }

    // ============================================================
//               CONSULTA GENERAL (TODOS LOS DATOS) - via servidor
// ============================================================
    private void cargarTodosLosDatos() {

        mostrarMensajeCarga(true);

        new Thread(() -> {

            limpiarSeries();

            String consulta = "QUERY:ALL";
            String respuestaPlano = ClienteSocket.enviarConsulta(consulta);

            if (respuestaPlano == null) {
                SwingUtilities.invokeLater(() -> {
                    mostrarMensajeCarga(false);
                    JOptionPane.showMessageDialog(JPanelHistorico,
                            "Error al solicitar datos al servidor.");
                });
                return;
            }

            if (!respuestaPlano.startsWith("OK")) {
                SwingUtilities.invokeLater(() -> {
                    mostrarMensajeCarga(false);
                    JOptionPane.showMessageDialog(JPanelHistorico,
                            "Respuesta inválida del servidor.");
                });
                return;
            }

            String payload = respuestaPlano.length() > 3 ? respuestaPlano.substring(3) : "";
            if (payload.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    mostrarMensajeCarga(false);
                    JOptionPane.showMessageDialog(JPanelHistorico, "No se encontraron datos.");
                });
                return;
            }

            String[] registros = payload.split(";");
            int t = 0;
            for (String reg : registros) {
                if (reg.trim().isEmpty()) continue;
                String[] vals = reg.split(",");
                if (vals.length != 3) continue;
                try {
                    final int x = Integer.parseInt(vals[0]);
                    final int y = Integer.parseInt(vals[1]);
                    final int z = Integer.parseInt(vals[2]);
                    final int tiempo = t++;
                    SwingUtilities.invokeLater(() -> {
                        seriesX.add(tiempo, x);
                        seriesY.add(tiempo, y);
                        seriesZ.add(tiempo, z);
                    });
                } catch (NumberFormatException ignored) {
                }
            }

            SwingUtilities.invokeLater(() -> mostrarMensajeCarga(false));

        }).start();
    }
    // ============================================================
    //                     UTILIDADES
    // ============================================================
    private void limpiarSeries() {
        SwingUtilities.invokeLater(() -> {
            seriesX.clear();
            seriesY.clear();
            seriesZ.clear();
        });
    }

    private void mostrarMensajeCarga(boolean cargando) {
        CargandoLabel.setText(cargando ? "Cargando datos..." : "");
    }

    public JPanel getPanelHistorico() {
        return JPanelHistorico;
    }
}
