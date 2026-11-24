package testes;

import dao.ComentarioDAO;
import dao.CompartilhamentoDAO;
import dao.CurtidaDAO;
import dao.FotoDAO;
import dao.GestaoDAO;
import dao.HashtagDAO;
import dao.MensagemDAO;
import dao.SeguidorDAO;
import dao.UsuarioDAO;
import dados.Comentario;
import dados.Foto;
import dados.Mensagem;
import dados.Usuario;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MongoDBEtapa2Test {
    public static void main(String[] args) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        UsuarioDAO usuarios = new UsuarioDAO();
        FotoDAO fotos = new FotoDAO();
        ComentarioDAO comentarios = new ComentarioDAO();
        MensagemDAO mensagens = new MensagemDAO();
        CurtidaDAO curtidas = new CurtidaDAO();
        SeguidorDAO seguidores = new SeguidorDAO();
        CompartilhamentoDAO compartilhamentos = new CompartilhamentoDAO();
        HashtagDAO hashtags = new HashtagDAO();
        GestaoDAO gestao = new GestaoDAO();
        Usuario autor = new Usuario("Autor de teste", "autor-" + suffix + "@example.com",
            "senha-segura", null, null);
        Usuario seguidor = new Usuario("Seguidor de teste", "seguidor-" + suffix + "@example.com",
            "senha-segura", null, null);
        Foto foto = new Foto("teste.jpg", "Publicação MongoDB", autor);
        int hashtagId = -1;

        try {
            usuarios.inserir(autor);
            usuarios.inserir(seguidor);
            fotos.inserir(foto, autor.getId());
            hashtags.associar(foto.getId(), "#etapa2_" + suffix);
            exigir(hashtags.listarPorFoto(foto.getId()).contains("etapa2_" + suffix),
                "hashtag deve ser associada à publicação");

            seguidores.seguir(seguidor.getId(), autor.getId());
            exigir(seguidores.listarSeguindo(seguidor.getId()).contains(autor.getId()),
                "relação de seguidores deve ser persistida");
            exigir(fotos.listarFeed(seguidor.getId()).stream()
                .anyMatch(publicacao -> publicacao.getId() == foto.getId()),
                "feed deve incluir publicações de quem o usuário segue");

            comentarios.inserir(new Comentario(autor.getNome(), "comentário MongoDB"),
                seguidor.getId(), foto.getId());
            exigir(comentarios.listarPorFoto(foto.getId()).size() == 1,
                "comentários devem ser consultados por publicação");
            curtidas.curtir(seguidor.getId(), foto.getId());
            curtidas.curtir(seguidor.getId(), foto.getId());
            exigir(curtidas.listarUsuarios(foto.getId()).size() == 1,
                "índice composto deve impedir curtidas duplicadas");
            compartilhamentos.compartilhar(seguidor.getId(), foto.getId());
            exigir(compartilhamentos.listarUsuariosQueCompartilharam(foto.getId())
                .contains(seguidor.getId()), "compartilhamentos devem ser persistidos");

            mensagens.enviar(new Mensagem(autor, seguidor, "mensagem MongoDB"));
            exigir(mensagens.listarMensagensEntre(autor, seguidor).size() == 1,
                "mensagens da conversa devem ser consultadas");

            GestaoDAO.QueryResult report = gestao.executarRelatorio("Comentários por publicação");
            exigir(contemId(report, foto.getId()), "relatório deve incluir a publicação");
            List<Object[]> hashtagRows = Arrays.asList(gestao.listar("hashtags").getRows());
            for (Object[] row : hashtagRows) {
                if (("etapa2_" + suffix).equals(row[1])) {
                    hashtagId = ((Number) row[0]).intValue();
                    break;
                }
            }
            exigir(hashtagId > 0, "hashtag deve aparecer na coleção gerenciada");

            gestao.executarAssociacao("hashtag", foto.getId(), hashtagId, false);
            exigir(!hashtags.listarPorFoto(foto.getId()).contains("etapa2_" + suffix),
                "associação de hashtag deve poder ser removida");
            gestao.executarAssociacao("hashtag", foto.getId(), hashtagId, true);

            fotos.excluir(foto.getId());
            exigir(comentarios.listarPorFoto(foto.getId()).isEmpty(),
                "excluir publicação deve remover seus comentários");
            exigir(hashtags.listarFotosPorHashtag("etapa2_" + suffix).isEmpty(),
                "excluir publicação deve remover a associação de hashtag");
            System.out.println("Testes de persistência MongoDB da fase 2 concluídos com sucesso.");
        } finally {
            usuarios.excluir(autor.getId());
            usuarios.excluir(seguidor.getId());
            if (hashtagId > 0) {
                gestao.excluir("hashtags", Map.of("id", Integer.toString(hashtagId)));
            }
        }
    }

    private static boolean contemId(GestaoDAO.QueryResult result, int id) {
        for (Object[] row : result.getRows()) {
            if (((Number) row[0]).intValue() == id) {
                return true;
            }
        }
        return false;
    }

    private static void exigir(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
