package PROYECTO.Vistas;

import PROYECTO.Estilos.RoundedBorder;
import PROYECTO.Estilos.TemaUnison;

import javax.swing.*;
import java.awt.*;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.fazecast.jSerialComm.SerialPort;

public class Vista_Monitor {

    private JPanel JPanelMonitor;
    private JLabel TextMonitorEnTiempoReal;
    private JComboBox<String> ComboBoxCOM;
    private JPanel JPanelGrafica;
    private JButton IniDetButton;
    private JButton regresarButton;

    private XYSeries seriesX = new XYSeries("X");
    private XYSeries seriesY = new XYSeries("Y");
    private XYSeries seriesZ = new XYSeries("Z");

    private volatile Integer ultimoX = null;
    private volatile Integer ultimoY = null;
    private volatile Integer ultimoZ = null;

    private SerialPort puertoActivo;
    private AtomicBoolean leyendo = new AtomicBoolean(false);
    private Thread hiloLectura;
    private Thread hiloGrafica;

    private int tiempo = 0;
    private static final int LIMITE_PUNTOS = 2000;
    private static final int TIMEOUT_MS = 2000;

    public Vista_Monitor() {
        aplicarEstilo();
        inicializarGrafica();
        configurarEventos();
        iniciarHiloGrafica();
    }

    private void aplicarEstilo() {
        JPanelMonitor.setBackground(TemaUnison.AZUL_OSCURO);

        TextMonitorEnTiempoReal.setFont(TemaUnison.FUENTE_TEXTO);
        TextMonitorEnTiempoReal.setForeground(Color.WHITE);

        ComboBoxCOM.setFont(TemaUnison.FUENTE_TEXTO);
        ComboBoxCOM.setBorder(new RoundedBorder(4));
        ComboBoxCOM.setBackground(Color.WHITE);

        IniDetButton.setFont(TemaUnison.FUENTE_TEXTO);
        IniDetButton.setBackground(TemaUnison.DORADO);
        IniDetButton.setForeground(Color.BLACK);
        IniDetButton.setBorder(new RoundedBorder(4));

        regresarButton.setFont(TemaUnison.FUENTE_TEXTO);
        regresarButton.setBackground(TemaUnison.DORADO_OSCURO);
        regresarButton.setForeground(Color.BLACK);
        regresarButton.setBorder(new RoundedBorder(4));

        JPanelGrafica.setBackground(Color.WHITE);
    }

    private void inicializarGrafica() {

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(seriesX);
        dataset.addSeries(seriesY);
        dataset.addSeries(seriesZ);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Monitor en tiempo real",
                "Tiempo (s)",
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
        chart.getXYPlot().setRangeGridlinePaint(new Color(220, 220, 220)); // gris claro
        chart.getXYPlot().setDomainGridlinePaint(new Color(220, 220, 220));

        // ===== ESTILO DE TÍTULOS, EJES Y LEYENDA =====
        chart.getTitle().setFont(TemaUnison.FUENTE_TITULO);
        chart.getLegend().setItemFont(TemaUnison.FUENTE_TEXTO);
        chart.getXYPlot().getDomainAxis().setLabelFont(TemaUnison.FUENTE_TEXTO);
        chart.getXYPlot().getRangeAxis().setLabelFont(TemaUnison.FUENTE_TEXTO);

        ChartPanel chartPanel = new ChartPanel(chart);
        JPanelGrafica.setLayout(new BorderLayout());
        JPanelGrafica.add(chartPanel, BorderLayout.CENTER);
    }

    private void configurarEventos() {

        IniDetButton.addActionListener(e -> {
            if (!leyendo.get()) {
                if (ComboBoxCOM.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(JPanelMonitor,
                            "Seleccione un puerto COM antes de iniciar.");
                    return;
                }
                iniciarLectura();
                IniDetButton.setText("Detener");
            } else {
                detenerLectura();
                IniDetButton.setText("Iniciar");
            }
        });

        regresarButton.addActionListener(e -> {
            detenerLectura();
            ((CardLayout) JPanelMonitor.getParent().getLayout())
                    .show(JPanelMonitor.getParent(), "Inicio");
        });
    }

    private void iniciarLectura() {

        limpiarSeries();
        tiempo = 0;

        String nombrePuerto = (String) ComboBoxCOM.getSelectedItem();
        puertoActivo = SerialPort.getCommPort(nombrePuerto);
        puertoActivo.setBaudRate(9600);

        puertoActivo.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                TIMEOUT_MS,
                0
        );

        if (!puertoActivo.openPort()) {
            JOptionPane.showMessageDialog(JPanelMonitor, "No se pudo abrir el puerto.");
            return;
        }

        leyendo.set(true);

        hiloLectura = new Thread(() -> {
            try { Thread.sleep(1200); } catch (Exception ignored) {}

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(puertoActivo.getInputStream()))) {

                while (leyendo.get()) {

                    String linea;

                    try {
                        linea = br.readLine();
                    } catch (Exception ex) {
                        if (ex.getMessage() != null && ex.getMessage().contains("timed out")) {
                            System.out.println("Timeout lectura (sin datos nuevos).");
                            continue;
                        }
                        System.out.println("Error lectura: " + ex.getMessage());
                        continue;
                    }

                    if (linea == null) continue;
                    linea = linea.trim();
                    if (!linea.matches("x:\\d+,y:\\d+,z:\\d+")) continue;

                    String[] partes = linea.split(",");
                    int x = Integer.parseInt(partes[0].split(":")[1]);
                    int y = Integer.parseInt(partes[1].split(":")[1]);
                    int z = Integer.parseInt(partes[2].split(":")[1]);

                    ultimoX = x;
                    ultimoY = y;
                    ultimoZ = z;

                    SwingUtilities.invokeLater(() ->
                            TextMonitorEnTiempoReal.setText("x=" + x + "  y=" + y + "  z=" + z));

                    ClienteSocket.enviarDatos(x, y, z);
                }

            } catch (Exception ex) {
                System.out.println("Error general lectura: " + ex.getMessage());
            }

            puertoActivo.closePort();
        });

        hiloLectura.start();
    }

    private void iniciarHiloGrafica() {
        hiloGrafica = new Thread(() -> {
            while (true) {
                try { Thread.sleep(200); } catch (Exception ignored) {}

                if (ultimoX == null) continue;

                tiempo++;

                SwingUtilities.invokeLater(() -> {
                    seriesX.add(tiempo, ultimoX);
                    seriesY.add(tiempo, ultimoY);
                    seriesZ.add(tiempo, ultimoZ);

                    limitarPuntos(seriesX);
                    limitarPuntos(seriesY);
                    limitarPuntos(seriesZ);
                });
            }
        });

        hiloGrafica.setDaemon(true);
        hiloGrafica.start();
    }

    private void limitarPuntos(XYSeries serie) {
        if (serie.getItemCount() > LIMITE_PUNTOS)
            serie.remove(0);
    }

    private void detenerLectura() {
        leyendo.set(false);
        try {
            if (puertoActivo != null && puertoActivo.isOpen())
                puertoActivo.closePort();
        } catch (Exception ignored) {}
    }

    private void limpiarSeries() {
        SwingUtilities.invokeLater(() -> {
            seriesX.clear();
            seriesY.clear();
            seriesZ.clear();
        });
    }

    public JPanel getPanelMonitor() {
        cargarPuertos();
        return JPanelMonitor;
    }

    public void cargarPuertos() {
        SerialPort[] puertos = SerialPort.getCommPorts();
        ComboBoxCOM.removeAllItems();
        for (SerialPort p : puertos)
            ComboBoxCOM.addItem(p.getSystemPortName());
    }
}
