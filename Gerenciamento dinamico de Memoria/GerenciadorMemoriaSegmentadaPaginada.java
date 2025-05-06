// Importa utilitários essenciais como listas, mapas, filas, etc.
import java.util.*;

/**
 * Simulador de Gerenciador de Memória com Segmentação + Paginação.
 * Autores: Henrique de lima e Heduardo Witkoski (equipe 8).
 * Adaptado para usar 4 segmentos fixos: código, dados, heap e pilha.
 */
public class GerenciadorMemoriaSegmentadaPaginada {

    // =========================
    // CLASSES INTERNAS
    // =========================

    // Representa uma variável alocada na memória, com segmentos mapeados
    static class Variavel {
        int id; // Identificador único da variável
        int tamanhoBytes; // Tamanho total da variável em bytes
        Map<String, Segmento> segmentos; // Mapeia o nome de cada segmento aos seus dados (páginas)

        // Construtor da classe Variavel, inicializa o ID, tamanho e o mapa de segmentos
        Variavel(int id, int tamanhoBytes) {
            this.id = id;
            this.tamanhoBytes = tamanhoBytes;
            this.segmentos = new HashMap<>(); // Inicializa o mapa de segmentos
        }
    }

    // Representa um segmento da variável, contendo a lista de páginas usadas
    static class Segmento {
        List<Integer> paginas = new ArrayList<>(); // Lista de índices das páginas que compõem o segmento
    }

    // =========================
    // ATRIBUTOS DO GERENCIADOR
    // =========================

    private final int tamanhoHeapKB; // Tamanho da heap em kilobytes
    private final int tamanhoPaginaBytes; // Tamanho de cada página em bytes
    private final int[] heap; // Array que simula a memória (inteiros representam dados armazenados)
    private final boolean[] paginasOcupadas; // Marca quais páginas estão ocupadas
    private final Map<Integer, Variavel> tabelaVariaveis = new HashMap<>(); // Tabela com variáveis ativas na memória
    private final Queue<Integer> filaFIFO = new LinkedList<>(); // Fila FIFO para política de remoção

    // Variáveis auxiliares para coleta de estatísticas
    private int proximoId = 1; // ID da próxima variável a ser alocada
    private int totalRequisicoesAtendidas = 0; // Contador de requisições atendidas
    private int totalTamanhoVariaveis = 0; // Soma do tamanho de todas as variáveis alocadas
    private int totalVariaveisRemovidas = 0; // Contador de variáveis removidas pela política FIFO
    private long fragmentacaoInternaTotal = 0; // Fragmentação interna total gerada
    private long tempoExecucaoMs = 0; // Tempo total de execução da simulação

    private int numeroRequisicoesExecutadas = 0; // Número total de requisições executadas
    private int minTamanhoRequisicao; // Tamanho mínimo das variáveis alocadas
    private int maxTamanhoRequisicao; // Tamanho máximo das variáveis alocadas

    // Construtor do gerenciador de memória
    public GerenciadorMemoriaSegmentadaPaginada(int tamanhoHeapKB, int tamanhoPaginaBytes) {
        this.tamanhoHeapKB = tamanhoHeapKB; // Define o tamanho da heap
        this.tamanhoPaginaBytes = tamanhoPaginaBytes; // Define o tamanho de cada página
        int totalPaginas = (tamanhoHeapKB * 1024) / tamanhoPaginaBytes; // Cálculo do número total de páginas
        this.heap = new int[totalPaginas * (tamanhoPaginaBytes / 4)]; // Cada palavra = 4 bytes
        this.paginasOcupadas = new boolean[totalPaginas]; // Inicializa vetor de páginas livres/ocupadas
    }

    // Executa a simulação com N requisições aleatórias de alocação
    public void executarSimulacao(int numeroRequisicoes, int minBytes, int maxBytes) {
        this.numeroRequisicoesExecutadas = numeroRequisicoes; // Define o número de requisições
        this.minTamanhoRequisicao = minBytes; // Define o tamanho mínimo da variável
        this.maxTamanhoRequisicao = maxBytes; // Define o tamanho máximo da variável

        Random random = new Random(); // Instancia o gerador de números aleatórios
        long inicio = System.currentTimeMillis(); // Marca o tempo inicial

        // Loop para executar as requisições de alocação
        for (int i = 0; i < numeroRequisicoes; i++) {
            int tamanho = random.nextInt(maxBytes - minBytes + 1) + minBytes; // Gera um tamanho aleatório dentro do intervalo
            alocarVariavelCom4Segmentos(tamanho); // Aloca a variável na memória
        }

        long fim = System.currentTimeMillis(); // Marca o tempo final
        tempoExecucaoMs = fim - inicio; // Calcula tempo de execução total
    }

    // Aloca uma variável em 4 segmentos (código, dados, heap, pilha)
    private void alocarVariavelCom4Segmentos(int tamanhoBytes) {
        String[] nomesSegmentos = {"codigo", "dados", "heap", "pilha"}; // Nomes dos 4 segmentos
        int numSegmentos = nomesSegmentos.length; // Número de segmentos (4)

        // Divide o tamanho igualmente entre os segmentos
        int tamanhoPorSegmento = (int) Math.ceil((double) tamanhoBytes / numSegmentos);
        int paginasPorSegmento = (int) Math.ceil((double) tamanhoPorSegmento / tamanhoPaginaBytes);
        int totalPaginasNecessarias = paginasPorSegmento * numSegmentos;

        // Tenta encontrar páginas livres
        List<Integer> paginasLivres = encontrarPaginasLivres(totalPaginasNecessarias);

        // Se não houver páginas suficientes, libera espaço com política FIFO
        if (paginasLivres.size() < totalPaginasNecessarias) {
            liberarEspaco(totalPaginasNecessarias); // Libera espaço na memória
            paginasLivres = encontrarPaginasLivres(totalPaginasNecessarias); // Tenta novamente
        }

        // Se houver espaço suficiente, faz a alocação
        if (paginasLivres.size() >= totalPaginasNecessarias) {
            int id = proximoId++; // Atribui um novo ID para a variável
            Variavel var = new Variavel(id, tamanhoBytes); // Cria uma nova variável
            int indicePagina = 0;

            // Aloca as páginas para cada segmento
            for (String nome : nomesSegmentos) {
                Segmento seg = new Segmento();

                // Associa páginas ao segmento
                for (int i = 0; i < paginasPorSegmento; i++) {
                    int pagina = paginasLivres.get(indicePagina++); // Pega uma página livre
                    paginasOcupadas[pagina] = true; // Marca a página como ocupada
                    preencherPaginaComID(pagina, id); // Preenche a página com o ID da variável
                    seg.paginas.add(pagina); // Adiciona a página ao segmento
                }

                var.segmentos.put(nome, seg); // Adiciona o segmento à variável
            }

            tabelaVariaveis.put(id, var); // Registra a variável na tabela de variáveis
            filaFIFO.add(id); // Adiciona à fila FIFO para controle de remoção

            totalRequisicoesAtendidas++; // Atualiza a estatística de requisições atendidas
            totalTamanhoVariaveis += tamanhoBytes; // Acumula o tamanho das variáveis alocadas

            // Calcula a fragmentação interna (desperdício de espaço nas páginas)
            int desperdicio = totalPaginasNecessarias * tamanhoPaginaBytes - tamanhoBytes;
            fragmentacaoInternaTotal += desperdicio; // Atualiza a fragmentação interna total
        }
    }

    // Retorna uma lista com as N primeiras páginas livres encontradas
    private List<Integer> encontrarPaginasLivres(int quantidade) {
        List<Integer> livres = new ArrayList<>();
        for (int i = 0; i < paginasOcupadas.length && livres.size() < quantidade; i++) {
            if (!paginasOcupadas[i]) {
                livres.add(i); // Adiciona a página à lista se estiver livre
            }
        }
        return livres;
    }

    // Libera espaço na memória removendo variáveis antigas da FIFO
    private void liberarEspaco(int paginasNecessarias) {
        int paginasParaLiberar = (int) Math.ceil(0.3 * paginasOcupadas.length); // Libera 30% das páginas ocupadas
        int paginasLiberadas = 0;

        // Libera páginas ocupadas por variáveis removidas da fila FIFO
        while (!filaFIFO.isEmpty() && paginasLiberadas < paginasParaLiberar) {
            int id = filaFIFO.poll(); // Remove o mais antigo
            Variavel var = tabelaVariaveis.remove(id); // Remove a variável da tabela

            if (var != null) {
                for (Segmento seg : var.segmentos.values()) {
                    for (int pagina : seg.paginas) {
                        paginasOcupadas[pagina] = false; // Marca a página como livre
                        limparPagina(pagina); // Limpa o conteúdo da página
                        paginasLiberadas++; // Atualiza o contador de páginas liberadas
                    }
                }
                totalVariaveisRemovidas++; // Atualiza a estatística de variáveis removidas
            }
        }
    }

    // Preenche a página com o ID da variável (simulando conteúdo)
    private void preencherPaginaComID(int pagina, int id) {
        int inicio = pagina * (tamanhoPaginaBytes / 4); // Índice inicial no vetor
        int fim = inicio + (tamanhoPaginaBytes / 4); // Índice final
        for (int i = inicio; i < fim; i++) {
            heap[i] = id; // Preenche a página com o ID da variável
        }
    }

    // Limpa uma página, preenchendo com zeros
    private void limparPagina(int pagina) {
        int inicio = pagina * (tamanhoPaginaBytes / 4);
        int fim = inicio + (tamanhoPaginaBytes / 4);
        for (int i = inicio; i < fim; i++) {
            heap[i] = 0; // Limpa o conteúdo da página
        }
    }

    // Exibe um resumo da simulação em formato CSV
    public void exibirResumoCSV() {
        int tamanhoMedio = (totalRequisicoesAtendidas > 0)
                ? totalTamanhoVariaveis / totalRequisicoesAtendidas
                : 0;

        // Formato: heapKB;paginaB;numReq;tamMedio;removidas;fragmentacao;tempoMs
        System.out.printf("%d;%d;%d;%d;%d;%d;%d\n",
                tamanhoHeapKB, // Tamanho da heap em KB
                tamanhoPaginaBytes, // Tamanho da página em Bytes
                numeroRequisicoesExecutadas, // Número de requisições executadas
                tamanhoMedio, // Tamanho médio das variáveis
                totalVariaveisRemovidas, // Total de variáveis removidas
                fragmentacaoInternaTotal, // Fragmentação interna
                tempoExecucaoMs); // Tempo de execução em milissegundos
    }

    // Método principal que recebe parâmetros pela linha de comando
    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Uso: java GerenciadorMemoriaSegmentadaPaginada <heapKB> <paginaB> <numRequisicoes> <minTamanho> <maxTamanho>");
            return;
        }

        // Lê argumentos da linha de comando
        int heapKB = Integer.parseInt(args[0]);
        int paginaB = Integer.parseInt(args[1]);
        int numRequisicoes = Integer.parseInt(args[2]);
        int minTamanho = Integer.parseInt(args[3]);
        int maxTamanho = Integer.parseInt(args[4]);

        // Cria o gerenciador e executa a simulação
        GerenciadorMemoriaSegmentadaPaginada gerenciador = new GerenciadorMemoriaSegmentadaPaginada(heapKB, paginaB);
        gerenciador.executarSimulacao(numRequisicoes, minTamanho, maxTamanho);
        gerenciador.exibirResumoCSV(); // Exibe os resultados
    }
}

