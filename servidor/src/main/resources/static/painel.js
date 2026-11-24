const seletor = document.getElementById('seletor');
const botaoSelecionar = document.getElementById('selecionar');
let seletorEditado = false;
let salvandoSelecao = false;
let lotesCarregados = false;
let amostraIdDasCapturas = null;
let versaoSelecao = 0;
const linhasCapturas = document.getElementById('capturas');

function mostrarAmostra(amostra) {
  document.getElementById('selecionada').textContent = `${amostra.lote_id} / ${amostra.amostra_id}`;
  if (amostraIdDasCapturas !== amostra.amostra_id) {
    amostraIdDasCapturas = amostra.amostra_id;
    linhasCapturas.replaceChildren();
    document.getElementById('capturas-amostra').textContent = `Capturas de ${amostraIdDasCapturas}`;
    document.getElementById('capturas-estado').textContent = 'Consultando capturas…';
  }
}

function criarLinhaCaptura(captura) {
  const linha = document.createElement('tr');
  const horario = document.createElement('td');
  horario.textContent = new Date(captura.recebida_em).toLocaleString('pt-BR');
  const lux = document.createElement('td');
  lux.textContent = captura.lux.toLocaleString('pt-BR');
  const celula = document.createElement('td');
  const link = document.createElement('a');
  link.href = `/api/capturas/${encodeURIComponent(captura.id)}/imagem`;
  link.target = '_blank';
  link.rel = 'noopener';
  const imagem = document.createElement('img');
  imagem.src = link.href;
  imagem.alt = `Abrir imagem completa da captura ${captura.id}`;
  imagem.width = 160;
  imagem.height = 120;
  link.append(imagem);
  celula.append(link);
  linha.append(horario, lux, celula);
  return linha;
}

async function atualizarCapturas() {
  const amostraId = amostraIdDasCapturas;
  const versaoConsultada = versaoSelecao;
  try {
    const capturas = await consultarJson(`/api/amostras/${encodeURIComponent(amostraId)}/capturas`);
    // A resposta pode chegar depois de o operador trocar a amostra.
    if (amostraId !== amostraIdDasCapturas || versaoConsultada !== versaoSelecao) {
      return;
    }
    const linhas = [];
    for (const captura of capturas) {
      linhas.push(criarLinhaCaptura(captura));
    }
    linhasCapturas.replaceChildren(...linhas);
    document.getElementById('capturas-estado').textContent = capturas.length
      ? `${capturas.length} captura(s). Clique na miniatura para abrir a imagem completa.` : 'Nenhuma captura nesta amostra.';
  } catch {
    if (amostraId === amostraIdDasCapturas && versaoConsultada === versaoSelecao) {
      document.getElementById('capturas-estado').textContent = 'Não foi possível consultar capturas.';
    }
  }
}
seletor.addEventListener('change', () => {
  seletorEditado = true;
});

async function consultarJson(caminho, opcoes = {}) {
  const resposta = await fetch(caminho, { cache: 'no-store', ...opcoes });
  if (!resposta.ok) {
    throw new Error('Não foi possível atender a requisição.');
  }
  return resposta.json();
}

// Usa o nome de cabeçalho fornecido pelo servidor para que o servidor valide o token CSRF nas alterações.
function enviarJsonComCsrf(caminho, metodo, csrf, dados) {
  return consultarJson(caminho, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', [csrf.header_name]: csrf.token },
    body: JSON.stringify(dados)
  });
}

async function salvarSelecao(evento) {
  evento.preventDefault();
  salvandoSelecao = true;
  versaoSelecao++;
  botaoSelecionar.disabled = true;
  seletor.disabled = true;
  const mensagem = document.getElementById('selecao-resultado');
  try {
    const csrf = await consultarJson('/api/painel/csrf');
    const amostra = await enviarJsonComCsrf('/api/painel/amostra-selecionada', 'PUT', csrf, {
      amostra_id: seletor.value
    });
    versaoSelecao++;
    mostrarAmostra(amostra);
    await atualizarCapturas();
    seletorEditado = false;
    mensagem.textContent = 'Seleção salva. Aguarde a próxima sincronização e confira a amostra informada.';
  } catch {
    mensagem.textContent = 'Não foi possível salvar a seleção. Tente novamente.';
  } finally {
    salvandoSelecao = false;
    botaoSelecionar.disabled = false;
    seletor.disabled = false;
  }
}

document.getElementById('selecao').addEventListener('submit', salvarSelecao);

const controlesComandos = document.getElementById('comandos-controles');
const nomesComandos = {
  capturar: 'Capturar',
  status: 'Atualizar estado',
  ajustar_brilho: 'Ajustar brilho'
};

async function enviarComando(pedido) {
  controlesComandos.disabled = true;
  const mensagem = document.getElementById('comandos-resultado');
  mensagem.textContent = 'Enviando comando…';
  try {
    const csrf = await consultarJson('/api/painel/csrf');
    const comando = await enviarJsonComCsrf('/api/painel/comandos', 'POST', csrf, pedido);
    mensagem.textContent = `Comando ${comando.id} registrado.`;
  } catch {
    mensagem.textContent = 'Não foi possível confirmar o registro. Confira o histórico antes de tentar novamente.';
  } finally {
    controlesComandos.disabled = false;
  }
}

for (const botaoComando of controlesComandos.querySelectorAll('button[type="button"]')) {
  botaoComando.addEventListener('click', () => enviarComando({ tipo: botaoComando.value }));
}

document.getElementById('comandos-form').addEventListener('submit', evento => {
  evento.preventDefault();
  const valor = document.getElementById('novo-brilho').valueAsNumber;
  enviarComando({ tipo: 'ajustar_brilho', valor });
});

async function atualizarComandos() {
  const mensagem = document.getElementById('comandos-estado');
  try {
    const comandos = await consultarJson('/api/painel/comandos');
    const linhas = comandos.reverse().map(comando => {
      const linha = document.createElement('tr');
      const nome = nomesComandos[comando.tipo];
      const descricao = comando.valor === null ? nome : `${nome} (${comando.valor})`;
      for (const texto of [comando.id, descricao, comando.estado]) {
        const celula = document.createElement('td');
        celula.textContent = texto;
        linha.append(celula);
      }
      return linha;
    });
    document.getElementById('comandos').replaceChildren(...linhas);
    mensagem.textContent = comandos.length
      ? 'Entregue indica que o servidor incluiu o comando na resposta à estação.'
      : 'Nenhum comando registrado.';
  } catch {
    mensagem.textContent = 'Não foi possível atualizar o histórico de comandos.';
  }
}

function preencherSeletor(lotes) {
  for (const lote of lotes) {
    const grupo = document.createElement('optgroup');
    grupo.label = lote.lote_id;
    for (const amostra of lote.amostras) {
      grupo.append(new Option(amostra.amostra_id, amostra.amostra_id));
    }
    seletor.append(grupo);
  }
  lotesCarregados = true;
  seletor.disabled = false;
  botaoSelecionar.disabled = false;
}

function mostrarEstadoEstacao(dadosPainel) {
  const conexao = document.getElementById('conexao');
  conexao.textContent = `${dadosPainel.estacao_id}: ${dadosPainel.online ? 'online' : 'offline'}`;
  conexao.dataset.online = dadosPainel.online;
  document.getElementById('ultima').textContent = dadosPainel.ultima_sincronizacao
    ? new Date(dadosPainel.ultima_sincronizacao).toLocaleString('pt-BR') : 'nenhuma';
  const estado = dadosPainel.estado;
  document.getElementById('estado').textContent = estado
    ? 'Último estado informado pela estação.' : 'Aguardando sincronização da estação.';
  const aviso = document.getElementById('captura-recusa');
  const recusa = estado?.motivo_recusa === 'lux_fora_da_faixa'
    ? 'Captura recusada: luz fora da faixa ideal.' : '';
  if (aviso.textContent !== recusa) {
    aviso.textContent = recusa;
  }
  document.getElementById('presenca').textContent = estado ? (estado.presenca ? 'Presente' : 'Ausente') : '—';
  document.getElementById('lux').textContent = estado ? `${estado.lux.toLocaleString('pt-BR')} lx` : '—';
  document.getElementById('brilho').textContent = estado ? `${estado.brilho} de 10 segmentos` : '—';
  document.getElementById('amostra').textContent = estado ? (estado.amostra_id ?? 'Nenhuma') : '—';
  document.getElementById('rssi').textContent = estado ? `${estado.rssi} dBm` : '—';
  document.getElementById('uptime').textContent = estado ? `${estado.uptime_s} s` : '—';
}

async function atualizarPainel() {
  const consultaComandos = atualizarComandos();
  try {
    if (!lotesCarregados) {
      const lotes = await consultarJson('/api/lotes');
      preencherSeletor(lotes);
    }
    const versaoConsultada = versaoSelecao;
    const dados = await consultarJson('/api/painel/estado');
    if (versaoConsultada !== versaoSelecao) {
      return;
    }
    const amostra = dados.amostra_selecionada;
    if (!salvandoSelecao) {
      mostrarAmostra(amostra);
      if (!seletorEditado && document.activeElement !== seletor) {
        seletor.value = amostra.amostra_id;
      }
    }
    mostrarEstadoEstacao(dados);
    if (!salvandoSelecao && amostraIdDasCapturas) {
      await atualizarCapturas();
    }
  } catch {
    const conexao = document.getElementById('conexao');
    conexao.textContent = 'Servidor indisponível';
    conexao.dataset.online = false;
  } finally {
    await consultaComandos;
    // Espera 2 s após o ciclo terminar para evitar sobrepor consultas periódicas.
    setTimeout(atualizarPainel, 2000);
  }
}
atualizarPainel();
