#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/liveon"

# Phrase-first translations applied ONLY inside Java string literals, so Java
# identifiers/API method names are never rewritten.
TRANSLATIONS = [
    ("Promoção: ", "Promotion: "),
    ("foi promoted to", "was promoted to"),
    ("foi promovido para", "was promoted to"),
    ("solicitou um rank", "requested a rank"),
    ("Aguardando aprovação", "Waiting for approval"),
    ("Solicitação enviada para a staff.", "Request sent to staff."),
    ("Current rank • carregando…", "Current rank • loading…"),
    ("Current rank • carregando...", "Current rank • loading..."),
    ("Rank disponível", "Available rank"),
    ("Ainda não verificado", "Not verified yet"),
    ("Requisitos", "Requirements"),
    ("Equipe os itens exigidos e abra os menus necessários antes de verificar.", "Equip the required items and open the necessary menus before checking."),
    ("Verificar", "Check"),
    ("Refresh itens, pontos e requisitos do rank", "Refresh items, points and rank requirements"),
    ("Próximo objetivo", "Next objective"),
    ("Solicitar novo rank", "Request new rank"),
    ("Não identificado", "Not identified"),
    ("não identificado", "not identified"),
    ("não sincronizado", "not synchronized"),
    ("carregando", "loading"),
    ("Melhor rank disponível", "Best available rank"),
    ("Como começar", "How to start"),
    ("Clique em Check. Se algum item não for detectado, abra o banco ou equipe-o e verifique novamente.", "Click Check. If an item is not detected, open your bank or equip it and check again."),
    ("Clique em Verificar. Se algum item não for detectado, abra o banco ou equipe-o e verifique novamente.", "Click Check. If an item is not detected, open your bank or equip it and check again."),
    ("Current rank indisponível", "Current rank unavailable"),
    ("Aguarde o Clan Chat carregar", "Wait for Clan Chat to load"),
    ("Informação", "Information"),
    ("A request será liberada quando o cargo atual for confirmado pelo Clan Chat.", "The request will be enabled when your current rank is confirmed by Clan Chat."),
    ("Aguardando Clan Chat", "Waiting for Clan Chat"),
    ("Rank máximo atingido", "Maximum rank reached"),
    ("Parabéns! Você chegou ao rank máximo do clã. Não há novos ranks para solicitar.", "Congratulations! You reached the clan's maximum rank. There are no further ranks to request."),
    ("You currently have a special rank, então a troca de rank não pode ser feita by aqui. Se quiser mudar, é só pedir para a staff pelo Discord.", "You currently have a special rank, so automatic rank changes are disabled. Ask staff on Discord if you want it changed."),
    ("Soldier • promoção automática", "Soldier • automatic promotion"),
    ("Dados verificados", "Verified data"),
    ("Confira abaixo os dados usados para calcular seu rank.", "Below are the values used to calculate your rank."),
    ("Próximo rank", "Next rank"),
    ("Faltando", "Missing"),
    ("Concluído", "Completed"),
    ("Concluída", "Completed"),
    ("Sim", "Yes"),
    ("Não", "No"),
    ("Pendente", "Pending"),
    ("Atingido", "Reached"),
    ("Não atingido", "Not reached"),
    ("Pontos de missão", "Quest points"),
    ("Capa de fogo", "Fire cape"),
    ("Capa de inferno", "Infernal cape"),
    ("Aljava", "Quiver"),
    ("Capa de diário", "Diary cape"),
    ("Capa de máximo", "Max cape"),
    ("Conquistas de combate", "Combat achievements"),
    ("fácil", "Easy"),
    ("médio", "Medium"),
    ("difícil", "Hard"),
    ("elite", "Elite"),
    ("mestre", "Master"),
    ("grão-mestre", "Grandmaster"),
    ("Abrir Discord do NightLegion", "Open NightLegion Discord"),
    ("Abrir grupo no Wise Old Man", "Open Wise Old Man group"),
    ("URL inválida", "Invalid URL"),
    ("Nenhuma mensagem", "No messages"),
    ("Nenhuma mensagem enviada", "No sent messages"),
    ("Nenhuma solicitação", "No requests"),
    ("Nenhuma solicitação pendente", "No pending requests"),
    ("Nenhum resultado", "No results"),
    ("Nenhum dado", "No data"),
    ("Nenhum PB", "No PB"),
    ("Nenhum drop", "No drops"),
    ("Nenhuma atividade recente", "No recent activity"),
    ("Nenhuma live ativa no momento.", "No active streams right now."),
    ("Atualizando...", "Refreshing..."),
    ("Carregando...", "Loading..."),
    ("Falha ao carregar", "Failed to load"),
    ("Falha ao atualizar", "Failed to refresh"),
    ("Erro ao carregar", "Failed to load"),
    ("Erro ao atualizar", "Failed to refresh"),
    ("Tente novamente", "Try again"),
    ("Atualizar", "Refresh"),
    ("Pesquisar", "Search"),
    ("Selecione", "Select"),
    ("Todos", "All"),
    ("Todas", "All"),
    ("Bosses", "Bosses"),
    ("Raids", "Raids"),
    ("Recordes pessoais do clã", "Clan personal bests"),
    ("Mensagem para o clã", "Message to clan"),
    ("Expandir", "Expand"),
    ("Recolher", "Collapse"),
    ("Canal do clã", "Clan channel"),
    ("Fixar broadcast", "Pin broadcast"),
    ("Enviar mensagem", "Send message"),
    ("Mensagens enviadas", "Sent messages"),
    ("mensagem(ns)", "message(s)"),
    ("Segurança da staff", "Staff security"),
    ("Salvar chave", "Save key"),
    ("Chave de acesso", "Access key"),
    ("Aviso fixo no painel", "Pinned Home announcement"),
    ("Publicar aviso fixado", "Publish pinned announcement"),
    ("Remover aviso atual", "Remove current announcement"),
    ("Gerenciar lives", "Manage streams"),
    ("Adicionar live", "Add stream"),
    ("Remover", "Remove"),
    ("Jogador", "Player"),
    ("Canal", "Channel"),
    ("Ao vivo", "Live"),
    ("Offline", "Offline"),
    ("Mês", "Month"),
    ("Ranking mensal", "Monthly ranking"),
    ("Líder do mês", "Month leader"),
    ("Sua posição", "Your position"),
    ("SUA POSIÇÃO", "YOUR POSITION"),
    ("Classificação", "Ranking"),
    ("CLASSIFICAÇÃO", "RANKING"),
    ("Drop do mês", "Drop of the month"),
    ("valor total", "total value"),
    ("Valor total", "Total value"),
    ("Como registrar seus PBs?", "How to register your PBs?"),
    ("Melhores tempos do clã", "Clan best times"),
    ("MELHORES TEMPOS DO CLÃ", "CLAN BEST TIMES"),
    ("Seu PB", "Your PB"),
    ("lugar", "place"),
    ("Modo", "Mode"),
    ("Tamanho do time", "Team size"),
    ("Tipo de tempo", "Time type"),
    ("Tempo", "Time"),
    ("Posição", "Position"),
    ("ANNOUNCEMENTS", "ANNOUNCEMENTS"),
    ("ATIVIDADE RECENTE DO CLÃ", "RECENT CLAN ACTIVITY"),
    ("Novo melhor tempo do clã", "New clan best time"),
    ("Autenticado como", "Authenticated as"),
    ("Desconectado", "Disconnected"),
    ("Conectado", "Connected"),
    ("Verificando...", "Checking..."),
    ("Falha na verificação", "Verification failed"),
    ("Não é membro do clã", "Not a clan member"),
    ("Membro não encontrado", "Member not found"),
    ("Acesso restrito", "Restricted access"),
    ("Somente staff", "Staff only"),
    ("Cargo especial", "Special rank"),
    ("Staff", "Staff"),
]

# Generic single-word cleanup is deliberately limited to accented/common UI
# terms after phrase substitutions, again only inside quoted literals.
WORDS = [
    ("Aguardando", "Waiting"), ("aprovação", "approval"), ("solicitação", "request"),
    ("Solicitação", "Request"), ("disponível", "available"), ("indisponível", "unavailable"),
    ("objetivo", "objective"), ("Objetivo", "Objective"), ("requisitos", "requirements"),
    ("Requisitos", "Requirements"), ("verificado", "verified"), ("Verificado", "Verified"),
    ("atual", "current"), ("Atual", "Current"), ("melhor", "best"), ("Melhor", "Best"),
    ("máximo", "maximum"), ("Máximo", "Maximum"), ("próximo", "next"), ("Próximo", "Next"),
    ("mensagem", "message"), ("Mensagem", "Message"), ("mensagens", "messages"), ("Mensagens", "Messages"),
    ("clã", "clan"), ("Clã", "Clan"), ("jogador", "player"), ("Jogador", "Player"),
    ("Nenhum", "No"), ("Nenhuma", "No"), ("Adicionar", "Add"), ("Excluir", "Delete"),
    ("Aceitar", "Accept"), ("Recusar", "Decline"), ("Confirmar", "Confirm"),
    ("enviado", "sent"), ("Enviado", "Sent"), ("pendente", "pending"), ("Pendente", "Pending"),
    ("carregar", "load"), ("Carregar", "Load"), ("carregando", "loading"), ("Carregando", "Loading"),
    ("falha", "failed"), ("Falha", "Failed"), ("dados", "data"), ("Dados", "Data"),
]

literal_re = re.compile(r'"(?:\\.|[^"\\])*"')

def translate_literal(match: re.Match) -> str:
    literal = match.group(0)
    content = literal[1:-1]
    original = content
    for old, new in TRANSLATIONS:
        content = content.replace(old, new)
    for old, new in WORDS:
        content = re.sub(rf'(?<![A-Za-zÀ-ÿ]){re.escape(old)}(?![A-Za-zÀ-ÿ])', new, content)
    return '"' + content + '"'

for path in SRC.glob("*.java"):
    text = path.read_text(encoding="utf-8")
    text = literal_re.sub(translate_literal, text)
    # Known malformed translation from the first adaptation pass.
    text = text.replace('Pattern.compile("(?:Promo\\u00E7\\u00E3o: )?(?<player>.+?) foi promoted to (?<rank>.+)!")',
                        'Pattern.compile("(?:Promotion: )?(?<player>.+?) was promoted to (?<rank>.+)!")')
    path.write_text(text, encoding="utf-8")

# Produce an audit report so remaining Portuguese UI strings can be reviewed.
portuguese_markers = re.compile(
    r'\b(?:não|nao|para|como|quando|cargo|rank|clã|cla|jogador|jogadores|mensagem|mensagens|solicita|'
    r'verificar|verificação|atualizar|carregar|nenhum|nenhuma|adicionar|remover|excluir|aceitar|recusar|'
    r'aguardando|disponível|requisitos|objetivo|melhor|próximo|dados|falha|erro|aviso|atividade|posição|'
    r'classificação|tempo|equipe|itens|pontos|missão|capa|conquista|mês|membro|membros|somente|segurança)\b',
    re.I)
review = ["# Remaining Portuguese string-literal review", ""]
for path in sorted(SRC.glob("*.java")):
    for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        for m in literal_re.finditer(line):
            value = m.group(0)[1:-1]
            if portuguese_markers.search(value) or any(ord(ch) > 127 for ch in value):
                review.append(f"- `{path.name}:{lineno}` — `{value}`")
(ROOT / "TRANSLATION_REVIEW.md").write_text("\n".join(review) + "\n", encoding="utf-8")
print(f"Translated exact Live On UI strings; review entries: {max(0, len(review)-2)}")
