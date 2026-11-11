package br.com.fiap.meusdireitos.util;

/**
 * Ferramenta de linha de comando para gerar hashes de senha dos dados iniciais (script .sql).
 * <p>
 * Uso, na pasta backend:
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass=br.com.fiap.meusdireitos.util.GeradorHashSenha -Dexec.args="Senha@123"
 * </pre>
 * Cada execução gera um hash diferente (salt aleatório), e todos validam a mesma senha.
 */
public final class GeradorHashSenha {

    private GeradorHashSenha() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Informe uma ou mais senhas. Ex.: -Dexec.args=\"Senha@123\"");
            return;
        }
        for (String senha : args) {
            System.out.println(SenhaUtil.gerarHash(senha));
        }
    }
}
