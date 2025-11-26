const API_URL = "https://guia-rq-backend.onrender.com";

async function carregarTicketIndividual() {
    const params = new URLSearchParams(window.location.search);
    const idPublico = params.get("qr");

    const erroEl = document.getElementById("erro");
    const loadingEl = document.getElementById("loading");
    const containerEl = document.getElementById("ticketContainer");

    if (!idPublico) {
        erroEl.textContent = "Ticket inválido.";
        erroEl.classList.remove("hidden");
        loadingEl.classList.add("hidden");
        return;
    }

    try {
        const response = await fetch(`${API_URL}/ticket/${idPublico}`);

        if (!response.ok) {
            erroEl.textContent = "Ticket não encontrado.";
            erroEl.classList.remove("hidden");
            loadingEl.classList.add("hidden");
            return;
        }

        const t = await response.json();

        document.getElementById("ticketNome").textContent = t.nomeTicket || t.nome;
        document.getElementById("ticketDescricao").textContent = t.descricao || "";

        document.getElementById("ticketData").textContent =
            t.dataCompra ? new Date(t.dataCompra).toLocaleDateString("pt-BR") : "—";

        document.getElementById("ticketExperiencia").textContent = t.experiencia || "—";

        // Status
        const stat = document.getElementById("ticketStatus");
        if (t.usado === true) {
            stat.textContent = "Usado";
            stat.className = "inline-block px-3 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-700";
        } else {
            stat.textContent = "Válido";
            stat.className = "inline-block px-3 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-700";
        }

        // QR CODE
        const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${t.qrToken}`;
        document.getElementById("ticketQr").src = qrUrl;

        loadingEl.classList.add("hidden");
        containerEl.classList.remove("hidden");

    } catch (err) {
        erroEl.textContent = "Erro ao carregar ticket.";
        erroEl.classList.remove("hidden");
        loadingEl.classList.add("hidden");
        console.error(err);
    }
}
