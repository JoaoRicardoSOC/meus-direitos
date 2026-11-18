# Diagrama de sequência · confirmar que foi resolvido

Gerado a partir do código: `ResolucaoComunidade.jsx` (front), `DenunciaResource.confirmarResolucao`, `ConfirmacaoResolucaoService.confirmar` e os DAOs JDBC (back-end).
O GitHub desenha o diagrama abaixo direto nesta página. A versão em imagem está em [`diagrama-sequencia-confirmacao.svg`](diagrama-sequencia-confirmacao.svg), nesta mesma pasta.

É a regra mais característica do Meus Direitos: o autor não decide sozinho que o problema acabou. Quando **3 moradores diferentes da mesma cidade** confirmam no detalhe da denúncia que "já foi resolvido", ela passa para `RESOLVIDA` sozinha, e a linha do tempo registra que a mudança veio da comunidade.

## Diagrama

```mermaid
sequenceDiagram
    autonumber
    actor M as Morador
    participant App as App React<br/>(ResolucaoComunidade)
    participant R as DenunciaResource
    participant A as AutenticacaoService
    participant S as ConfirmacaoResolucaoService
    participant T as Transacao
    participant D as DenunciaDaoJdbc
    participant U as UsuarioDaoJdbc
    participant C as ConfirmacaoResolucaoDaoJdbc
    participant H as HistoricoStatusDaoJdbc
    participant DB as Oracle

    M->>App: toca em "Já foi resolvido"
    App->>R: PUT /api/denuncias/{id}/confirmacao<br/>Authorization: Bearer token
    R->>A: idDoUsuarioLogado(authorization)
    A-->>R: idUsuario (ou 401)
    R->>S: confirmar(idUsuario, idDenuncia)
    S->>T: executar(con -> ...)
    T->>DB: empresta conexão do pool (autoCommit = false)

    S->>D: buscarParaAtualizar(idDenuncia)
    D->>DB: SELECT ... FOR UPDATE OF d.st_denuncia
    Note over D,DB: Trava a linha da denúncia até o fim da transação.<br/>Confirmações simultâneas esperam na fila<br/>e cada uma conta as que já foram gravadas.
    DB-->>D: denúncia (ou nenhuma linha → 404)
    D-->>S: Denuncia

    alt quem confirma é o autor
        S-->>R: RegraNegocioException (422)
    else a denúncia já está resolvida
        S-->>R: RegraNegocioException (422)
    else
        S->>U: buscarPorId(idUsuario)
        U-->>S: Usuario (cidade e UF)
        alt morador de outra cidade
            S-->>R: AcessoNegadoException (403)
        else mesma cidade
            S->>C: confirmar(idUsuario, idDenuncia)
            C->>DB: INSERT INTO tb_gs26s2_confirmacao_resolucao
            Note over C,DB: Chave primária (usuário, denúncia):<br/>confirmar de novo não conta duas vezes.
            S->>C: contar(idDenuncia)
            C->>DB: SELECT COUNT(*) ...
            C-->>S: total de confirmações
            opt total >= 3 (Denuncia.confirmacoesSuficientes)
                S->>D: atualizarStatus(idDenuncia, RESOLVIDA)
                D->>DB: UPDATE tb_gs26s2_denuncia SET st_denuncia = 'RESOLVIDA'
                S->>H: registrar(idDenuncia, anterior, RESOLVIDA, COMUNIDADE)
                H->>DB: INSERT INTO tb_gs26s2_historico_status
            end
            S->>D: buscarDetalhada(idDenuncia, idUsuario)
            D-->>S: denúncia com placar e progresso da confirmação
        end
    end

    alt sem exceção
        T->>DB: COMMIT (libera a trava)
        S-->>R: DenunciaResponse
        R-->>App: 200 + denúncia (status, resolucao.confirmacoes, resolucao.euConfirmei)
        App-->>M: barra de progresso "N de 3" e, com 3, selo "Resolvida"
        Note over App: A linha do tempo da tela é buscada de novo<br/>(GET /denuncias/{id}/historico).
    else exceção de regra ou de banco
        T->>DB: ROLLBACK (nada fica gravado)
        R-->>App: JSON de erro {status, erro, mensagem, caminho, timestamp}
        App-->>M: mensagem do erro, sem mudar a tela
    end
    T->>DB: devolve a conexão ao pool
```

## Como ler

- **Tudo numa transação.** `Transacao.executar` abre a conexão, roda o bloco e faz `COMMIT` se nada falhar ou `ROLLBACK` se qualquer exceção subir. Gravar a confirmação, mudar a situação e registrar o histórico acontecem juntos ou não acontecem.
- **Por que a trava.** Sem o `FOR UPDATE`, três moradores confirmando no mesmo instante contavam só a própria confirmação (1, 1, 1) e a denúncia nunca chegava a 3. Com a trava, a segunda transação espera a primeira terminar e já enxerga a confirmação gravada. Isso foi conferido disparando três confirmações em paralelo contra a API e o Oracle.
- **Quem pode confirmar.** Moradores logados, da mesma cidade da denúncia (403 para outra cidade), que não sejam o autor (422: ele usa "Marcar como resolvida" em Minhas denúncias, que registra no histórico com origem `AUTOR`).
- **Retirar a confirmação.** `DELETE` na mesma rota segue o mesmo caminho (com a mesma trava), apaga a linha da pessoa e não muda a situação; depois de resolvida, a confirmação não pode mais ser retirada (422).
- **Erros.** As exceções viram o JSON padrão nos mappers de `exception/mapper` (ver a parte 3 do [diagrama de classes](diagrama-classes.md#3-erros-exception)).
