#!/bin/bash

# Verificação de número de argumentos
if [ "$#" -ne 6 ]; then
    echo "Uso: $0 <heapKB> <paginaB> <numRequisicoes> <minTamanho> <maxTamanho> <repeticoes>"
    exit 1
fi

# Atribuindo os argumentos a variáveis
HEAP_KB=$1
PAGINA_B=$2
REQUISICOES=$3
MIN_TAM=$4
MAX_TAM=$5
REPETICOES=$6

# Cálculo do tamanho médio estimado
TAM_MEDIO_ESTIMADO=$(( (MIN_TAM + MAX_TAM) / 2 ))

# Nome do arquivo Java (sem extensão)
ARQUIVO="GerenciadorMemoriaPaginacao"

# Gerar nome do arquivo de saída CSV
CSV_NOME="resultado_${HEAP_KB}_${PAGINA_B}_${REQUISICOES}_${TAM_MEDIO_ESTIMADO}_${REPETICOES}.csv"

# Compilar o programa
javac "$ARQUIVO.java"
if [ $? -ne 0 ]; then
    echo "Erro na compilação. Abortando."
    exit 1
fi

# Criar o arquivo CSV com cabeçalho
echo "TamanhoHeapKB;TamanhoPaginaBytes;NumeroRequisicoes;TamanhoMedioRequisicoes;VariaveisRemovidas;FragmentacaoInterna;TempoExecucaoMs" > "$CSV_NOME"

# Executar várias vezes e salvar os resultados
for i in $(seq 1 $REPETICOES); do
    java $ARQUIVO $HEAP_KB $PAGINA_B $REQUISICOES $MIN_TAM $MAX_TAM >> "$CSV_NOME"
done

echo "Testes finalizados. Resultados salvos em $CSV_NOME"

