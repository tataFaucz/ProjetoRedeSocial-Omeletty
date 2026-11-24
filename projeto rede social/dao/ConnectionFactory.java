package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConnectionFactory {
    private static final String URL = configuracao("POSTGRES_URL", "DB_URL",
        "jdbc:postgresql://" + configuracao("POSTGRES_HOST", "DB_HOST", "localhost")
            + ":" + configuracao("POSTGRES_PORT", "DB_PORT", "5432")
            + "/" + configuracao("POSTGRES_DATABASE", "DB_NAME", "rede_social"));
    private static final String USER = configuracao("POSTGRES_USER", "DB_USER", null);
    private static final String PASSWORD = configuracao("POSTGRES_PASSWORD", "DB_PASSWORD", null);

    private ConnectionFactory() {
    }

    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        if (USER != null) {
            properties.setProperty("user", USER);
        }
        if (PASSWORD != null) {
            properties.setProperty("password", PASSWORD);
        }
        return DriverManager.getConnection(URL, properties);
    }

    private static String configuracao(String principal, String alternativa, String padrao) {
        String valor = configuracao(principal);
        return valor == null || valor.isBlank()
            ? valorOuPadrao(configuracao(alternativa), padrao)
            : valor;
    }

    private static String configuracao(String nome) {
        String valor = System.getProperty(nome);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(nome);
        }
        return valor;
    }

    private static String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
