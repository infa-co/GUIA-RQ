## **Guia Web RQ - Rancho Queimado | Motor de Reservas**

Um sistema web desenvolvido para ser o **Guia Oficial de Rancho Queimado (SC)**. O objetivo principal do projeto é centralizar informações turísticas, históricas, gastronômicas e, crucialmente, gerar receita através de um motor de **reservas e venda de tickets** robusto.

A plataforma oferece uma solução completa para gestão de inventário e transações financeiras para parceiros locais.

### **Arquitetura & Stack Tecnológica**

O projeto adota a arquitetura **Model-View-Controller (MVC)**, garantindo uma base sólida, escalável e de fácil manutenção para lidar com a complexidade das transações financeiras e gestão de disponibilidade.

| Componente | Tecnologia | Papel no Sistema |
| :---: | :--- | :--- |
| **Backend** | **Java (MVC)** | Lógica de negócio, gestão de disponibilidade (*slots* e calendário), segurança e API REST. |
| **Frontend** | **HTML5, CSS3 & JavaScript** | Interface *Mobile-First* para clientes e painéis de gestão, focada em alta usabilidade. |
| **Monetização** | **Stripe Connect** | Módulo para **Split Payment** (divisão automática de comissões e repasse de valores aos parceiros). |
| **Comunicação** | **WhatsApp/IA** | Automação do relacionamento com parceiros e clientes (notificações e lembretes). |
| **Banco de Dados** | **Supabase** | Armazenamento de produtos, reservas e controle de inventário (`CalendarioDisponibilidade`). |

---

### **Funcionalidades Centrais (Entregáveis)**

O sistema cobre todo o ciclo de vida do turismo e gestão de eventos na região:

* **Motor de Reservas Integrado:** Gerenciamento de disponibilidade baseado em **dias (Hospedagens)** e **slots de horário (Passeios/Restaurantes)**.
* **Venda de Tickets Avulsos:** *Checkout* seguro, geração automática de **QR Codes** e sistema de validação instantânea.
* **Gestão Financeira Avançada:** Implementação do **Stripe Connect** para repasse automático, controle de comissões e relatórios de fluxo de caixa para o Guia.
* **Módulos de Conversão:** Lógica de **Upsells** no *checkout* e sistema de **Reviews** automatizado (após a conclusão do serviço).

---

### **Como Contribuir**

Toda contribuição para a evolução do Guia RQ é bem-vinda.

**Status:** Em Desenvolvimento | **Desenvolvido em:** Java/MVC | **Contato:** [Seu Nome ou E-mail]
