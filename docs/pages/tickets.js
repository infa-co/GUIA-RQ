// ================================================
// CONFIGURAÇÃO DA API
// ================================================
const API_URL = "https://api.guiaranchoqueimado.com.br/api";


// ================================================
// FUNÇÃO – BUSCAR TICKETS POR E-MAIL
// ================================================
async function buscarTickets() {
    const email = document.getElementById("emailBusca").value.trim();

    if (!email) {
        alert("Digite um e-mail válido.");
        return;
    }

    document.getElementById("loading").classList.remove("hidden");
    document.getElementById("ticketsLista").innerHTML = "";

    try {
        const resp = await fetch(`${API_URL}/tickets/por-email?email=${email}`);
        const dados = await resp.json();

        document.getElementById("loading").classList.add("hidden");

        if (!dados.length) {
            document.getElementById("ticketsLista").innerHTML =
                `<p class="text-center text-gray-600 mt-4">Nenhum ticket encontrado para este e-mail.</p>`;
            return;
        }

        renderTicketsLista(dados);

    } catch (err) {
        document.getElementById("loading").classList.add("hidden");
        alert("Erro ao buscar tickets. Verifique mais tarde.");
        console.error("Erro:", err);
    }
}


// ================================================
// RENDERIZA LISTA DE TICKETS
// ================================================
function renderTicketsLista(tickets) {
    const container = document.getElementById("ticketsLista");
    container.innerHTML = "";

    tickets.forEach(t => {
        const statusClass = t.usado
            ? "bg-red-100 text-red-700"
            : "bg-green-100 text-green-700";

        const dataFormatada = t.dataCompra
            ? new Date(t.dataCompra).toLocaleDateString("pt-BR")
            : "—";

        container.innerHTML += `
            <div class="bg-white rounded-xl shadow p-4 flex justify-between items-center">
                <div>
                    <p class="font-semibold text-gray-900">${t.nome}</p>
                    <p class="text-sm text-gray-600">Compra: ${dataFormatada}</p>
                    <span class="inline-block mt-1 px-3 py-1 rounded-full text-xs font-semibold ${statusClass}">
                        ${t.usado ? "Usado" : "Válido"}
                    </span>
                </div>

                <button onclick="abrirTicket('${t.idPublico}')"
                        class="bg-blue-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-blue-700">
                    Abrir
                </button>
            </div>
        `;
    });
}


// ================================================
// ABRIR TICKET INDIVIDUAL
// ================================================
function abrirTicket(idPublico) {
    window.location.href = `ticket.html?id=${idPublico}`;
}


// ================================================
// CARREGAR TICKET INDIVIDUAL
// ================================================
async function carregarTicketIndividual() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");

    if (!id) {
        mostrarErro("ID do ticket não encontrado na URL.");
        return;
    }

    try {
        const resp = await fetch(`${API_URL}/tickets/ver/${id}`);

        if (!resp.ok) {
            mostrarErro("Ticket não encontrado.");
            return;
        }

        const ticket = await resp.json();
        preencherTicket(ticket);

    } catch (err) {
        console.error(err);
        mostrarErro("Erro ao carregar ticket.");
    }
}


// ================================================
// PREENCHER TELA DO TICKET
// ================================================
function preencherTicket(t) {
    document.getElementById("loading").classList.add("hidden");
    document.getElementById("ticketContainer").classList.remove("hidden");

    document.getElementById("ticketNome").innerText = t.nome;
    document.getElementById("ticketDescricao").innerText = t.descricao || "";

    const dataFormatada = t.dataCompra
        ? new Date(t.dataCompra).toLocaleString("pt-BR")
        : "—";

    document.getElementById("ticketData").innerText = dataFormatada;

    document.getElementById("ticketExperiencia").innerText = t.experiencia || "—";

    // STATUS
    const status = document.getElementById("ticketStatus");
    status.innerText = t.usado ? "Usado" : "Válido";
    status.className =
        "inline-block px-3 py-1 rounded-full text-xs font-semibold " +
        (t.usado ? "bg-red-100 text-red-700" : "bg-green-100 text-green-700");

    // QR CODE
    document.getElementById("ticketQr").src = `${API_URL}/qr/${t.idPublico}`;
}


// ================================================
// MOSTRAR ERRO
// ================================================
function mostrarErro(msg) {
    document.getElementById("loading").classList.add("hidden");
    const erro = document.getElementById("erro");
    erro.innerText = msg;
    erro.classList.remove("hidden");
}
