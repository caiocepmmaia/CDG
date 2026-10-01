# 💰 Sistema de Gestão Financeira (JavaFX)

Um sistema desktop moderno de gestão financeira pessoal desenvolvido em **Java** utilizando **JavaFX**, **SQLite** e arquitetura baseada em repositórios. O projeto foca no controlo rigoroso de despesas, receitas, investimentos, faturas de cartão de crédito com ciclos de fecho/vencimento personalizados, metas de poupança e relatórios em PDF.

---

## 🚀 Funcionalidades Principais

* **Dashboard Mensal Interativo:** Navegação fluida entre meses com cálculo automático de sobras, déficits e despesas reembolsáveis.
* **Controlo de Cartões de Crédito:** Gestão de múltiplos cartões com configuração de dias de vencimento e fechamento, calculando automaticamente a fatura correta com base na data da compra.
* **Gestão de Transações:** Registo de receitas, despesas e investimentos, com suporte completo para parcelamentos (evitando perda de cêntimos entre parcelas) e marcação de despesas corporativas/reembolsáveis.
* **Metas e Objetivos:** Criação de metas financeiras com acompanhamento visual de progresso (barras de progresso e indicadores circulares) e funcionalidade rápida de **"✨ Investir Sobra"**.
* **Categorias Personalizadas:** Organização e agrupamento visual dos gastos por categoria.
* **Relatórios em PDF:** Geração automática de relatórios detalhados de despesas reembolsáveis para o mês corrente utilizando OpenPDF.

---

## 🛠️ Tecnologias e Bibliotecas

* **Java 17+**
* **JavaFX** (Interface Gráfica)
* **SQLite JDBC** (Base de dados local)
* **Lombok** (Redução de boilerplate de código)
* **OpenPDF** (Geração de relatórios PDF)
* **JUnit 5** (Testes unitários)

---

## 📂 Estrutura do Projeto

```text
src/
├── main/
│   ├── java/org/cdg/
│   │   ├── App.java                 # Ponto de entrada da aplicação JavaFX
│   │   ├── Launcher.java            # Launcher auxiliar de compatibilidade (Linux/Windows)
│   │   ├── AtualizarBanco.java      # Script utilitário para resetar/limpar o banco de dados
│   │   ├── controller/              # Controladores das telas (Dashboard, Formulários)
│   │   ├── model/                   # Classes de domínio e DTOs
│   │   ├── repository/              # Camada de persistência e comunicação com SQLite
│   │   ├──service/                  # Regras de negocios
│   │   └──util/                     # Funcoes auxiliares
│   └── resources/
│       ├── database/                # Ficheiro da base de dados SQLite (financas.db)
│       └── view/                    # Ficheiros FXML e estilos CSS (Tema Cosmic)
└── test/                            # Testes unitários de regras de negócio e repositórios