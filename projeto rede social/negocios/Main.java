package negocios;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        final Sistema sistema;
        try {
            sistema = new Sistema();
        } catch (Exception exception) {
            exception.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                null,
                "Não foi possível conectar ao PostgreSQL. Confira se o serviço está ativo e se "
                    + "POSTGRES_URL/DB_URL e as credenciais estão configurados.",
                "Erro de banco de dados",
                JOptionPane.ERROR_MESSAGE
            ));
            return;
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            new MainFrame(sistema).setVisible(true);
        });
    }
}