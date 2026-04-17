package com.example.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int quantidadeTotalItens = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();

        int percentualQuantidade = calcularPercentualPorQuantidade(quantidadeTotalItens);

        int percentualCategoria = itens.stream()
                .mapToInt(this::calcularPercentualCategoriaPorItem)
                .sum();

        int percentualTotal = Math.min(25, percentualQuantidade + percentualCategoria);

        BigDecimal percentualDesconto = BigDecimal.valueOf(percentualTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorDesconto = subtotal
                .multiply(percentualDesconto)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(valorDesconto).setScale(2, RoundingMode.HALF_UP);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }

    private int calcularPercentualPorQuantidade(int quantidadeTotalItens) {
        if (quantidadeTotalItens >= 4) {
            return 10;
        }

        if (quantidadeTotalItens == 3) {
            return 7;
        }

        if (quantidadeTotalItens == 2) {
            return 5;
        }

        return 0;
    }

    private int calcularPercentualCategoriaPorItem(ItemCarrinho item) {
        int percentualCategoria = switch (item.getProduto().getCategoria()) {
            case CAPINHA, FONE -> 3;
            case CARREGADOR -> 5;
            case PELICULA, SUPORTE -> 2;
        };

        return percentualCategoria * item.getQuantidade();
    }
}
