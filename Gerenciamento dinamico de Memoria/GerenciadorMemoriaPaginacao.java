import java.util.*;

public class GerenciadorMemoriaPaginacao {

    static class Variavel {
        int id;
        int tamanhoBytes;
        List<Integer> paginasAlocadas;

        Variavel(int id, int tamanhoBytes) {
            this.id = id;
            this.tamanhoBytes = tamanhoBytes;
            this.paginasAlocadas = new ArrayList<>();
        }
    }

    private final int tamanhoHeapKB;
    private final int tamanhoPaginaBytes;
    private final int[] heap;
    private final boolean[] paginasOcupadas;
    private final Map<Integer, Variavel> tabelaVariaveis = new HashMap<>();
    private final Queue<Integer> filaFIFO = new LinkedList<>();

    private int proximoId = 1;
    private int totalRequisicoesAtendidas = 0;
    private int totalTamanhoVariaveis = 0;
    private int totalVariaveisRemovidas = 0;
    private long fragmentacaoInternaTotal = 0;
    private long tempoExecucaoMs = 0;

    private int numeroRequisicoesExecutadas = 0;
    private int minTamanhoRequisicao;
    private int maxTamanhoRequisicao;

    public GerenciadorMemoriaPaginacao(int tamanhoHeapKB, int tamanhoPaginaBytes) {
        this.tamanhoHeapKB = tamanhoHeapKB;
        this.tamanhoPaginaBytes = tamanhoPaginaBytes;
        int totalPaginas = (tamanhoHeapKB * 1024) / tamanhoPaginaBytes;
        this.heap = new int[totalPaginas * (tamanhoPaginaBytes / 4)];
        this.paginasOcupadas = new boolean[totalPaginas];
    }

    public void executarSimulacao(int numeroRequisicoes, int minBytes, int maxBytes) {
        this.numeroRequisicoesExecutadas = numeroRequisicoes;
        this.minTamanhoRequisicao = minBytes;
        this.maxTamanhoRequisicao = maxBytes;

        Random random = new Random();
        long inicio = System.currentTimeMillis();

        for (int i = 0; i < numeroRequisicoes; i++) {
            int tamanho = random.nextInt(maxBytes - minBytes + 1) + minBytes;
            alocarVariavel(tamanho);
        }

        long fim = System.currentTimeMillis();
        tempoExecucaoMs = fim - inicio;
    }

    private void alocarVariavel(int tamanhoBytes) {
        int paginasNecessarias = (int) Math.ceil((double) tamanhoBytes / tamanhoPaginaBytes);
        List<Integer> paginasLivres = encontrarPaginasLivres(paginasNecessarias);

        if (paginasLivres.size() < paginasNecessarias) {
            liberarEspaco(paginasNecessarias);
            paginasLivres = encontrarPaginasLivres(paginasNecessarias);
        }

        if (paginasLivres.size() >= paginasNecessarias) {
            int id = proximoId++;
            Variavel var = new Variavel(id, tamanhoBytes);
            for (int pagina : paginasLivres.subList(0, paginasNecessarias)) {
                paginasOcupadas[pagina] = true;
                preencherPaginaComID(pagina, id);
                var.paginasAlocadas.add(pagina);
            }
            tabelaVariaveis.put(id, var);
            filaFIFO.add(id);

            totalRequisicoesAtendidas++;
            totalTamanhoVariaveis += tamanhoBytes;

            int desperdicio = paginasNecessarias * tamanhoPaginaBytes - tamanhoBytes;
            fragmentacaoInternaTotal += desperdicio;
        }
    }

    private List<Integer> encontrarPaginasLivres(int quantidade) {
        List<Integer> livres = new ArrayList<>();
        for (int i = 0; i < paginasOcupadas.length; i++) {
            if (!paginasOcupadas[i]) {
                livres.add(i);
                if (livres.size() == quantidade) break;
            }
        }
        return livres;
    }

    private void liberarEspaco(int paginasNecessarias) {
        int paginasParaLiberar = (int) Math.ceil(0.3 * paginasOcupadas.length);
        int paginasLiberadas = 0;

        while (!filaFIFO.isEmpty() && paginasLiberadas < paginasParaLiberar) {
            int id = filaFIFO.poll();
            Variavel var = tabelaVariaveis.remove(id);
            if (var != null) {
                for (int pagina : var.paginasAlocadas) {
                    paginasOcupadas[pagina] = false;
                    limparPagina(pagina);
                    paginasLiberadas++;
                }
                totalVariaveisRemovidas++;
            }
        }
    }

    private void preencherPaginaComID(int pagina, int id) {
        int inicio = pagina * (tamanhoPaginaBytes / 4);
        int fim = inicio + (tamanhoPaginaBytes / 4);
        for (int i = inicio; i < fim; i++) {
            heap[i] = id;
        }
    }

    private void limparPagina(int pagina) {
        int inicio = pagina * (tamanhoPaginaBytes / 4);
        int fim = inicio + (tamanhoPaginaBytes / 4);
        for (int i = inicio; i < fim; i++) {
            heap[i] = 0;
        }
    }

    public void exibirResumoCSV() {
        int tamanhoMedio = (totalRequisicoesAtendidas > 0) ? totalTamanhoVariaveis / totalRequisicoesAtendidas : 0;
        System.out.printf("%d;%d;%d;%d;%d;%d;%d\n",
                tamanhoHeapKB,
                tamanhoPaginaBytes,
                numeroRequisicoesExecutadas,
                tamanhoMedio,
                totalVariaveisRemovidas,
                fragmentacaoInternaTotal,
                tempoExecucaoMs);
    }

    public static void main(String[] args) {
    if (args.length != 5) {
        System.err.println("Uso: java GerenciadorMemoriaPaginacao <heapKB> <paginaB> <numRequisicoes> <minTamanho> <maxTamanho>");
        return;
    }

    int heapKB = Integer.parseInt(args[0]);
    int paginaB = Integer.parseInt(args[1]);
    int numRequisicoes = Integer.parseInt(args[2]);
    int minTamanho = Integer.parseInt(args[3]);
    int maxTamanho = Integer.parseInt(args[4]);

    GerenciadorMemoriaPaginacao gerenciador = new GerenciadorMemoriaPaginacao(heapKB, paginaB);
    gerenciador.executarSimulacao(numRequisicoes, minTamanho, maxTamanho);
    gerenciador.exibirResumoCSV();
}

}

