package dao;

import dados.Mensagem;
import dados.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class MensagemDAO {
    public void enviar(Mensagem mensagem) throws SQLException {
        int remetente = mensagem.getRemetente().getId();
        int destinatario = mensagem.getDestinatario().getId();
        if (remetente == destinatario) {
            throw new SQLException("Não é possível enviar uma mensagem para si mesmo.");
        }
        PostgresSupport.requireExists("usuarios", remetente, "Remetente");
        PostgresSupport.requireExists("usuarios", destinatario, "Destinatário");
        String sql = "INSERT INTO mensagens "
            + "(id_remetente, id_destinatario, conteudo, data_envio) VALUES (?, ?, ?, ?)";
        Timestamp timestamp = mensagem.getTimestamp();
        if (timestamp == null) {
            timestamp = new Timestamp(System.currentTimeMillis());
        }
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, remetente);
            statement.setInt(2, destinatario);
            statement.setString(3, mensagem.getConteudo());
            statement.setTimestamp(4, timestamp);
            statement.executeUpdate();
        }
    }

    public List<Mensagem> listarMensagensEntre(Usuario remetente, Usuario destinatario)
            throws SQLException {
        List<Mensagem> result = new ArrayList<>();
        int senderId = remetente.getId();
        int receiverId = destinatario.getId();
        String sql = "SELECT id, id_remetente, conteudo, data_envio FROM mensagens "
            + "WHERE (id_remetente = ? AND id_destinatario = ?) "
            + "OR (id_remetente = ? AND id_destinatario = ?) ORDER BY data_envio";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, senderId);
            statement.setInt(2, receiverId);
            statement.setInt(3, receiverId);
            statement.setInt(4, senderId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    boolean outgoing = rows.getInt("id_remetente") == senderId;
                    Timestamp timestamp = rows.getTimestamp("data_envio");
                    LocalDateTime date = timestamp == null ? null
                        : LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
                    result.add(new Mensagem(
                        rows.getInt("id"),
                        outgoing ? remetente : destinatario,
                        outgoing ? destinatario : remetente,
                        rows.getString("conteudo"),
                        date
                    ));
                }
            }
        }
        return result;
    }
}
