package PROYECTO.Vistas;

import PROYECTO.Estilos.RoundedBorder;
import PROYECTO.Estilos.TemaUnison;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class Vista_Inicio {

    // Componentes generados por el .form
    private JPanel JPanel_Inicio;
    private JButton monitorButton;
    private JButton historicoButton;
    private JLabel JLabelSM;
    private JLabel JLabelName;

    public Vista_Inicio() {

        aplicarTema();  // ← Estilo global

        // Borde interno opcional
        JPanel_Inicio.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    // ============================================================
    //                  APLICAR ESTILO UNISON
    // ============================================================
    private void aplicarTema() {

        // Fondo general azul oscuro UNISON
        JPanel_Inicio.setBackground(TemaUnison.AZUL_OSCURO);

        // ----- Estilo del Título -----
        JLabelSM.setFont(TemaUnison.FUENTE_TITULO);
        JLabelSM.setForeground(Color.WHITE);

        // ----- Estilo del autor / subtítulo -----
        JLabelName.setFont(TemaUnison.FUENTE_TEXTO);
        JLabelName.setForeground(Color.WHITE);

        // ====== Botón Monitor ======
        monitorButton.setFont(TemaUnison.FUENTE_TEXTO);
        monitorButton.setBackground(TemaUnison.DORADO);
        monitorButton.setForeground(Color.BLACK);
        monitorButton.setBorder(new RoundedBorder(4));

        // ====== Botón Histórico ======
        historicoButton.setFont(TemaUnison.FUENTE_TEXTO);
        historicoButton.setBackground(TemaUnison.DORADO);
        historicoButton.setForeground(Color.BLACK);
        historicoButton.setBorder(new RoundedBorder(4));
    }

    // ============================================================
    //                      GETTERS
    // ============================================================
    public JPanel getPanelInicio() {
        return JPanel_Inicio;
    }

    public JButton getMonitorButton() {
        return monitorButton;
    }

    public JButton gethistoricoButton() {
        return historicoButton;
    }

}
