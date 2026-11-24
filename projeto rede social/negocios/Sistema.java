package negocios;

import dao.ComentarioDAO;
import dao.CurtidaDAO;
import dao.FotoDAO;
import dao.HashtagDAO;
import dao.MensagemDAO;
import dao.SeguidorDAO;
import dao.CompartilhamentoDAO;
import dao.UsuarioDAO;
import dados.Comentario;
import dados.Foto;
import dados.Mensagem;
import dados.Usuario;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Sistema {
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final FotoDAO fotoDAO = new FotoDAO();
    private final ComentarioDAO comentarioDAO = new ComentarioDAO();
    private final MensagemDAO mensagemDAO = new MensagemDAO();
    private final CurtidaDAO curtidaDAO = new CurtidaDAO();
    private final SeguidorDAO seguidorDAO = new SeguidorDAO();
    private final CompartilhamentoDAO compartilhamentoDAO = new CompartilhamentoDAO();
    private final HashtagDAO hashtagDAO = new HashtagDAO();

    private List<Usuario> usuarios = new ArrayList<>();
    private Usuario usuarioLogado;

    public Sistema() {
        carregarUsuarios();
    }

    private void carregarUsuarios() {
        try {
            usuarios = usuarioDAO.listarTodos();
        } catch (SQLException e) {
            usuarios = new ArrayList<>();
            e.printStackTrace();
        }
    }

    private void carregarDadosUsuario(Usuario usuario) {
        if (usuario == null) {
            return;
        }

        usuario.getPublicacoes().clear();
        usuario.getSeguindo().clear();
        usuario.getSeguidores().clear();
        usuario.getMensagensEnviadas().clear();
        usuario.getMensagensRecebidas().clear();

        try {
            for (Foto foto : fotoDAO.listarPorUsuario(usuario.getId())) {
                foto.setAutor(usuario);
                usuario.adicionarPublicacao(foto);
            }

            for (int idSeguido : seguidorDAO.listarSeguindo(usuario.getId())) {
                Usuario seguido = buscarUsuarioPorId(idSeguido);
                if (seguido != null) {
                    usuario.getSeguindo().add(seguido);
                }
            }

            for (int idSeguidor : seguidorDAO.listarSeguidores(usuario.getId())) {
                Usuario seguidor = buscarUsuarioPorId(idSeguidor);
                if (seguidor != null) {
                    usuario.getSeguidores().add(seguidor);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Usuario> listarUsuariosAtualizados() throws SQLException {
        usuarios = usuarioDAO.listarTodos();
        if (usuarioLogado != null) {
            for (int i = 0; i < usuarios.size(); i++) {
                if (usuarios.get(i).getId() == usuarioLogado.getId()) {
                    usuarios.set(i, usuarioLogado);
                    break;
                }
            }
        }
        return new ArrayList<>(usuarios);
    }

    public void carregarDadosPerfil(Usuario usuario) throws SQLException {
        if (usuario == null) {
            return;
        }

        usuario.getPublicacoes().clear();
        usuario.getSeguidores().clear();
        usuario.getSeguindo().clear();

        for (Foto foto : fotoDAO.listarPorUsuario(usuario.getId())) {
            foto.setAutor(usuario);
            carregarInteracoesFoto(foto);
            usuario.adicionarPublicacao(foto);
        }

        List<Usuario> todos = listarUsuariosAtualizados();
        for (int idSeguido : seguidorDAO.listarSeguindo(usuario.getId())) {
            Usuario seguido = todos.stream().filter(candidate -> candidate.getId() == idSeguido)
                .findFirst().orElse(null);
            if (seguido != null) {
                usuario.getSeguindo().add(seguido);
            }
        }

        for (int idSeguidor : seguidorDAO.listarSeguidores(usuario.getId())) {
            Usuario seguidor = todos.stream().filter(candidate -> candidate.getId() == idSeguidor)
                .findFirst().orElse(null);
            if (seguidor != null) {
                usuario.getSeguidores().add(seguidor);
            }
        }
    }

    public boolean estaSeguindo(Usuario seguido) throws SQLException {
        if (usuarioLogado == null || seguido == null) {
            return false;
        }
        return seguidorDAO.listarSeguindo(usuarioLogado.getId()).contains(seguido.getId());
    }

    public boolean cadastrarUsuario(String nome, String email, String senha,
                                   String fotoPerfil, String biografia) throws SQLException {
        if (ValidacaoConta.validar(nome, email, biografia, fotoPerfil, senha, true) != null ||
            usuarioDAO.existeLogin(email)) {
            return false;
        }

        Usuario usuario = new Usuario(nome.trim(),
            email.trim().toLowerCase(Locale.ROOT), senha, vazioNulo(fotoPerfil), vazioNulo(biografia));
        try {
            usuarioDAO.inserir(usuario);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                return false;
            }
            throw e;
        }

        if (usuarios == null) {
            usuarios = new ArrayList<>();
        }
        usuarios.add(usuario);
        return true;
    }

    public boolean login(String email, String senha) throws SQLException {
        Usuario usuario = usuarioDAO.buscarPorLogin(email == null ? "" : email.trim(), senha);
        if (usuario != null) {
            usuarioLogado = usuario;
            carregarDadosUsuario(usuario);
            if (!usuarios.contains(usuario)) {
                usuarios.add(usuario);
            }
            return true;
        }
        return false;
    }

    public void logout() {
        usuarioLogado = null;
    }

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public boolean editarConta(String nome, String email, String biografia,
                               String fotoPerfil, String senhaAtual, String novaSenha) {
        if (usuarioLogado == null ||
            ValidacaoConta.validar(nome, email, biografia, fotoPerfil, novaSenha, false) != null) {
            return false;
        }

        boolean trocarSenha = novaSenha != null && !novaSenha.isEmpty();
        try {
            if (trocarSenha && (senhaAtual == null || !usuarioDAO.senhaCorreta(usuarioLogado.getId(), senhaAtual))) {
                return false;
            }
            String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
            if (usuarioDAO.existeEmailEmOutroUsuario(emailNormalizado, usuarioLogado.getId())) {
                return false;
            }

            Usuario atualizado = new Usuario(usuarioLogado.getId(), nome.trim(),
                emailNormalizado, null, vazioNulo(fotoPerfil), vazioNulo(biografia));
            usuarioDAO.atualizarConta(atualizado, trocarSenha ? novaSenha : null);
            usuarioLogado.setNome(atualizado.getNome());
            usuarioLogado.setEmail(atualizado.getEmail());
            usuarioLogado.setBiografia(atualizado.getBiografia());
            usuarioLogado.setFotoPerfil(atualizado.getFotoPerfil());
            return true;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean excluirConta(String senha) {
        if (usuarioLogado == null || senha == null || senha.isEmpty()) {
            return false;
        }
        try {
            if (!usuarioDAO.senhaCorreta(usuarioLogado.getId(), senha)) {
                return false;
            }
            int idExcluido = usuarioLogado.getId();
            usuarioDAO.excluir(idExcluido);
            usuarios.removeIf(usuario -> usuario.getId() == idExcluido);
            usuarioLogado = null;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String vazioNulo(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }

    public List<Usuario> getUsuarios() {
        if (usuarios == null || usuarios.isEmpty()) {
            carregarUsuarios();
        }
        return usuarios;
    }

    public Usuario buscarUsuario(String emailOuNome) {
        carregarUsuarios();
        for (Usuario u : getUsuarios()) {
            if (u.getEmail().equalsIgnoreCase(emailOuNome) || u.getNome().equalsIgnoreCase(emailOuNome)) {
                return u;
            }
        }
        return null;
    }

    public Usuario buscarUsuarioPorId(int id) {
        for (Usuario u : getUsuarios()) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public boolean publicarFoto(String caminho, String legenda) {
        return publicarFoto(caminho, legenda, "");
    }

    public boolean publicarFoto(String caminho, String legenda, String hashtags) {
        if (usuarioLogado == null) {
            return false;
        }

        try {
            Foto foto = new Foto(caminho, legenda, usuarioLogado);
            fotoDAO.inserir(foto, usuarioLogado.getId());
            try {
                hashtagDAO.associar(foto.getId(), hashtags);
            } catch (SQLException ex) {
                fotoDAO.excluir(foto.getId());
                throw ex;
            }
            foto.setAutor(usuarioLogado);
            carregarInteracoesFoto(foto);
            usuarioLogado.adicionarPublicacao(foto);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Foto> listarPublicacoesDoFeed() throws SQLException {
        if (usuarioLogado == null) {
            return new ArrayList<>();
        }
        List<Foto> fotos = fotoDAO.listarFeed(usuarioLogado.getId());
        for (Foto foto : fotos) {
            carregarInteracoesFoto(foto);
        }
        return fotos;
    }

    private void carregarInteracoesFoto(Foto foto) throws SQLException {
        foto.getCurtidas().clear();
        foto.getCurtidas().addAll(curtidaDAO.listarUsuarios(foto.getId()));
        foto.getCompartilhamentos().clear();
        foto.getCompartilhamentos().addAll(compartilhamentoDAO.listarUsuarios(foto.getId()));
        foto.getComentarios().clear();
        foto.getComentarios().addAll(comentarioDAO.listarPorFoto(foto.getId()));
        foto.getHashtags().clear();
        for (String hashtag : hashtagDAO.listarPorFoto(foto.getId())) {
            foto.adicionarHashtag(hashtag);
        }
    }

    public void atualizarComentarios(Foto foto) throws SQLException {
        foto.getComentarios().clear();
        foto.getComentarios().addAll(comentarioDAO.listarPorFoto(foto.getId()));
    }

    public boolean curtirFoto(Foto foto) {
        if (usuarioLogado == null || foto == null) {
            return false;
        }

        try {
            boolean jaCurtiu = foto.getCurtidas().stream()
                .anyMatch(usuario -> usuario.getId() == usuarioLogado.getId());
            if (jaCurtiu) {
                curtidaDAO.descurtir(usuarioLogado.getId(), foto.getId());
                foto.getCurtidas().removeIf(usuario -> usuario.getId() == usuarioLogado.getId());
            } else {
                curtidaDAO.curtir(usuarioLogado.getId(), foto.getId());
                foto.adicionarCurtida(usuarioLogado);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean compartilharFoto(Foto foto) {
        if (usuarioLogado == null || foto == null) {
            return false;
        }

        try {
            boolean jaCompartilhou = foto.getCompartilhamentos().stream()
                .anyMatch(usuario -> usuario.getId() == usuarioLogado.getId());
            if (jaCompartilhou) {
                compartilhamentoDAO.removerCompartilhamento(usuarioLogado.getId(), foto.getId());
                foto.getCompartilhamentos().removeIf(usuario -> usuario.getId() == usuarioLogado.getId());
            } else {
                compartilhamentoDAO.compartilhar(usuarioLogado.getId(), foto.getId());
                foto.adicionarCompartilhamento(usuarioLogado);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean comentarFoto(Foto foto, String texto) {
        if (usuarioLogado == null || foto == null || texto == null || texto.trim().isEmpty()) {
            return false;
        }

        try {
            Comentario comentario = new Comentario(usuarioLogado.getNome(), texto.trim());
            comentarioDAO.inserir(comentario, usuarioLogado.getId(), foto.getId());
            foto.adicionarComentario(comentario);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void enviarMensagem(String emailDestino, String conteudo) {
        Usuario destino = buscarUsuario(emailDestino);
        if (usuarioLogado == null || destino == null || conteudo == null || conteudo.trim().isEmpty()) {
            return;
        }

        try {
            Mensagem mensagem = new Mensagem(usuarioLogado, destino, conteudo.trim());
            mensagemDAO.enviar(mensagem);
            usuarioLogado.enviarMensagem(destino, conteudo.trim());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Mensagem> visualizarMensagens(String emailOutroUsuario) {
        Usuario outro = buscarUsuario(emailOutroUsuario);
        if (usuarioLogado == null || outro == null) {
            return null;
        }

        try {
            return mensagemDAO.listarMensagensEntre(usuarioLogado, outro);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean seguirUsuario(Usuario usuario) {
        if (usuarioLogado == null || usuario == null || usuario.getId() == usuarioLogado.getId()) {
            return false;
        }

        try {
            seguidorDAO.seguir(usuarioLogado.getId(), usuario.getId());
            usuarioLogado.seguir(usuario);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deixarDeSeguirUsuario(Usuario usuario) {
        if (usuarioLogado == null || usuario == null || usuario.getId() == usuarioLogado.getId()) {
            return false;
        }

        try {
            seguidorDAO.deixarDeSeguir(usuarioLogado.getId(), usuario.getId());
            usuarioLogado.deixarDeSeguir(usuario);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}