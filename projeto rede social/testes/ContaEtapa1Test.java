package testes;

import negocios.SegurancaSenha;
import negocios.Sistema;
import negocios.ValidacaoConta;
import java.util.UUID;

public class ContaEtapa1Test {
    public static void main(String[] args) throws Exception {
        testarValidacaoESenha();
        if (args.length > 0 && "--unit".equals(args[0])) {
            System.out.println("Testes unitários da etapa 1 concluídos com sucesso.");
            return;
        }
        testarFluxosDeConta();
        System.out.println("Testes da etapa 1 concluídos com sucesso.");
    }

    private static void testarValidacaoESenha() {
        exigir(ValidacaoConta.validar("Pessoa", "pessoa@example.com", "", "",
            "senha-segura", true) == null, "dados válidos devem ser aceitos");
        exigir(ValidacaoConta.validar("", "pessoa@example.com", "", "",
            "senha-segura", true) != null, "nome vazio deve ser rejeitado");
        exigir(ValidacaoConta.validar("Pessoa", "email-invalido", "", "",
            "senha-segura", true) != null, "e-mail inválido deve ser rejeitado");
        exigir(ValidacaoConta.validar("Pessoa", "pessoa@example.com", "", "",
            "curta", true) != null, "senha curta deve ser rejeitada");

        String hash = SegurancaSenha.gerarHash("senha-segura");
        exigir(!hash.equals("senha-segura"), "senha persistida não pode ser texto puro");
        exigir(SegurancaSenha.verificar("senha-segura", hash), "hash deve autenticar a senha correta");
        exigir(!SegurancaSenha.verificar("senha-errada", hash), "hash deve rejeitar senha incorreta");
        exigir(SegurancaSenha.verificar("legada", "legada"), "login legado deve ser migrável");
    }

    private static void testarFluxosDeConta() throws Exception {
        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String emailOriginal = sufixo + "@example.com";
        String emailNovo = "novo_" + sufixo + "@example.com";
        String outroEmail = "outro_" + sufixo + "@example.com";
        String senhaOriginal = "Senha-inicial-42";
        String senhaNova = "Senha-atualizada-42";
        Sistema sistema = new Sistema();
        boolean contaPrincipalCriada = false;
        boolean outraContaCriada = false;

        try {
            exigir(sistema.cadastrarUsuario("Conta Teste", emailOriginal,
                senhaOriginal, "", "bio inicial"), "cadastro deve funcionar");
            contaPrincipalCriada = true;
            exigir(sistema.getUsuarios().stream().filter(usuario -> emailOriginal.equals(usuario.getEmail()))
                .findFirst().get().getSenha() == null, "senha não deve permanecer no objeto de usuário");
            exigir(sistema.cadastrarUsuario("Outra Conta", outroEmail,
                senhaOriginal, "", ""), "cadastro da segunda conta deve funcionar");
            outraContaCriada = true;
            exigir(!sistema.cadastrarUsuario("Duplicada", emailOriginal,
                senhaOriginal, "", ""), "e-mail duplicado deve ser rejeitado");
            exigir(!sistema.cadastrarUsuario("Inválida", "email-invalido",
                senhaOriginal, "", ""), "cadastro inválido deve ser rejeitado");

            exigir(sistema.login(outroEmail, senhaOriginal), "segunda conta deve autenticar pelo e-mail");
            exigir(!sistema.editarConta("Conta indevida", emailOriginal, "", "",
                senhaOriginal, ""), "uma conta não deve poder assumir o e-mail de outra");
            exigir(outroEmail.equals(sistema.getUsuarioLogado().getEmail()),
                "tentativa de edição não deve alterar a conta autenticada");
            sistema.logout();

            exigir(!sistema.login(emailOriginal, "senha-errada"), "senha incorreta não autentica");
            exigir(sistema.login(emailOriginal, senhaOriginal), "login por e-mail deve funcionar");
            sistema.logout();
            exigir(sistema.getUsuarioLogado() == null, "logout deve encerrar a sessão");
            exigir(!sistema.editarConta("Sem sessão", emailOriginal, "", "", "", ""),
                "edição deve exigir sessão autenticada");
            exigir(!sistema.excluirConta(senhaOriginal), "exclusão deve exigir sessão autenticada");
            exigir(sistema.login(emailOriginal, senhaOriginal), "login por e-mail deve funcionar");

            exigir(!sistema.editarConta("Conta Teste", emailOriginal, "", "",
                "incorreta", senhaNova), "troca deve exigir senha atual correta");
            exigir(!sistema.editarConta("Conta Teste", outroEmail, "", "",
                senhaOriginal, ""), "e-mail de outra conta deve ser rejeitado");
            exigir(sistema.editarConta("Conta Atualizada", emailNovo, "bio nova", "",
                senhaOriginal, senhaNova), "edição e troca de senha devem funcionar");
            exigir("Conta Atualizada".equals(sistema.getUsuarioLogado().getNome()), "nome deve atualizar na sessão");
            exigir(emailNovo.equals(sistema.getUsuarioLogado().getEmail()), "e-mail deve atualizar na sessão");

            sistema.logout();
            exigir(!sistema.login(emailOriginal, senhaOriginal), "credenciais antigas devem deixar de funcionar");
            exigir(sistema.login(emailNovo, senhaNova), "novas credenciais devem funcionar");
            exigir(!sistema.excluirConta("senha-errada"), "exclusão deve exigir senha correta");
            exigir(sistema.getUsuarioLogado() != null, "falha na exclusão não deve encerrar sessão");
            exigir(sistema.excluirConta(senhaNova), "exclusão autenticada deve funcionar");
            contaPrincipalCriada = false;
            exigir(sistema.getUsuarioLogado() == null, "excluir conta deve encerrar sessão");
            exigir(!sistema.login(emailNovo, senhaNova), "conta excluída não deve autenticar");
        } finally {
            sistema.logout();
            if (contaPrincipalCriada) {
                excluirSeAutenticavel(sistema, emailOriginal, senhaOriginal);
                excluirSeAutenticavel(sistema, emailNovo, senhaNova);
            }
            if (outraContaCriada) {
                excluirSeAutenticavel(sistema, outroEmail, senhaOriginal);
            }
        }
    }

    private static void excluirSeAutenticavel(Sistema sistema, String email, String senha) throws Exception {
        if (sistema.login(email, senha) && sistema.getUsuarioLogado() != null) {
            sistema.excluirConta(senha);
        }
        sistema.logout();
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
