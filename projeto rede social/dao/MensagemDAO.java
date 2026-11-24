package dao;

import com.mongodb.client.MongoCollection;
import dados.Mensagem;
import dados.Usuario;
import org.bson.Document;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.or;

public class MensagemDAO {
    private static final String COLLECTION = "mensagens";

    public void enviar(Mensagem mensagem) throws SQLException {
        int senderId = mensagem.getRemetente().getId();
        int recipientId = mensagem.getDestinatario().getId();
        if (senderId == recipientId) {
            throw new SQLException("Não é possível enviar uma mensagem para si mesmo.");
        }
        MongoSupport.requireExists("usuarios", senderId, "Remetente");
        MongoSupport.requireExists("usuarios", recipientId, "Destinatário");
        Date date = mensagem.getTimestamp() == null
            ? new Date() : new Date(mensagem.getTimestamp().getTime());
        MongoSupport.inserir(COLLECTION, new Document("id_remetente", senderId)
            .append("id_destinatario", recipientId)
            .append("conteudo", mensagem.getConteudo())
            .append("data_envio", date));
    }

    public List<Mensagem> listarMensagensEntre(Usuario remetente, Usuario destinatario)
            throws SQLException {
        List<Mensagem> result = new ArrayList<>();
        int senderId = remetente.getId();
        int receiverId = destinatario.getId();
        for (Document document : collection().find(or(
                new Document("id_remetente", senderId).append("id_destinatario", receiverId),
                new Document("id_remetente", receiverId).append("id_destinatario", senderId)))
                .sort(new Document("data_envio", 1))) {
            boolean outgoing = MongoSupport.inteiro(document, "id_remetente") == senderId;
            Date timestamp = document.getDate("data_envio");
            LocalDateTime date = timestamp == null ? null
                : LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
            result.add(new Mensagem(
                MongoSupport.inteiro(document, "id"),
                outgoing ? remetente : destinatario,
                outgoing ? destinatario : remetente,
                document.getString("conteudo"),
                date));
        }
        return result;
    }

    private MongoCollection<Document> collection() {
        return ConnectionFactory.getCollection(COLLECTION);
    }
}
