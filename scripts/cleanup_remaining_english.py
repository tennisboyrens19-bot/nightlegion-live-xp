#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'src/main/java/com/liveon'

REPL = [
    ('Entregar este broadcast também aos próximos players que entrarem', 'Also deliver this broadcast to players who join later'),
    ('Este aviso ficará visível<br>', 'This announcement will remain visible<br>'),
    ('Ao publicar um novo aviso,<br>', 'When you publish a new announcement,<br>'),
    ('o current será substituído.</div></html>', 'the current one will be replaced.</div></html>'),
    ('Texto que será exibido na seção Announcements da página Início', 'Text shown in the Announcements section on Home'),
    ('Digite o aviso antes de publicar', 'Enter the announcement before publishing'),
    ('Solicitações de rank', 'Rank requests'),
    ('Aviso painel', 'Home announcement'),
    ('Gerenciar aviso fixo do Home', 'Manage the pinned Home announcement'),
    ('<html><center>Ative <b>Conectar ao clan</b><br>nas configurações do plugin.</center></html>', '<html><center>Enable <b>Connect to clan server</b><br>in the plugin settings.</center></html>'),
    ('<html><center>A conexão continua desativada.<br>Ative a opção e tente novamente.</center></html>', '<html><center>The connection is still disabled.<br>Enable it and try again.</center></html>'),
    (' — requer 2300', ' — requires 2300'),
    ('— abra o Character Summary e sincronize', '— open Character Summary and sync'),
    ('— não obtida', '— not obtained'),
    (' pontos): ', ' points): '),
    ('Passo 1: equipe ou coloque as capas no inventário para detectar os itens.', 'Step 1: equip or place the required capes in your inventory so they can be detected.'),
    ('Passo 2: abra a aba Combat Achievements no jogo para load seus pontos.', 'Step 2: open Combat Achievements in-game to load your points.'),
    ('Passo 4: abra a página do Inferno no Collection Log para conferir o registro local.', 'Step 4: open the Inferno page in Collection Log to verify the local record.'),
    ('EHB não é considerado no cálculo dos ranks.', 'EHB is not used to calculate ranks.'),
    ('em análise', 'under review'),
    ('Promotion de rank available: ', 'Rank promotion available: '),
    ('[NightLegion] Promotion de rank available: ', '[NightLegion] Rank promotion available: '),
    ('membro', 'member'),
    ('General — somente via Discord', 'General — Discord only'),
    ('! Abra o banco uma vez<br>para verificar seus itens', '! Open your bank once<br>to verify your items'),
    ('✓ Max cape / 2376 total atendido', '✓ Max cape / 2376 total met'),
    ('/2376 — equivalente à Max cape', '/2376 — equivalent to Max cape'),
    ('! Combat Achievements ainda não carregado', '! Combat Achievements not loaded yet'),
    (' pontos)', ' points)'),
    ('! Promotion automática após 30 dias no clan', '! Automatic promotion after 30 days in the clan'),
    (': não verified — ', ': not verified — '),
    (' não encontrado — coloque no inventário ou equipe se possuir', ' not found — place it in your inventory or equip it if you own it'),
    (' ainda não verified — abra o banco', ' not verified yet — open your bank'),
    ('— abra a aba Combat Achievements e sincronize', '— open Combat Achievements and sync'),
    (' pontos ou equipe o ', ' points or equip the '),
    ('— abra a aba Combat Achievements ou equipe o ', '— open Combat Achievements or equip the '),
    (' — equipe ou coloque na bolsa', ' — equip it or place it in your inventory'),
    ('Maximum rank reached, parabéns!', 'Maximum rank reached, congratulations!'),
    ('Você pode solicitar seu novo rank via Discord no canal #ranks.', 'You can request your new rank on Discord in #ranks.'),
    ('Requirements faltantes: Quest cape', 'Missing requirements: Quest cape'),
    ('Indisponível', 'Unavailable'),
    ('Sem conexão', 'No connection'),
    ('Erro ', 'Error '),
    ('Failed ao publicar aviso', 'Failed to publish announcement'),
    ('Aviso publicado no Home', 'Announcement published on Home'),
    ('Failed ao remover aviso', 'Failed to remove announcement'),
    ('Aviso removido', 'Announcement removed'),
    ('Failed ao remover canal', 'Failed to remove channel'),
    ('Informe o nome do membro', 'Enter the member name'),
    ('Failed ao adicionar MVP', 'MVP badges are automatic'),
    ('Failed ao remover MVP', 'MVP badges are automatic'),
    ('Use de 1 a 5 letras ou números', 'Use 1 to 5 letters or numbers'),
    ('Membro adicionado', 'Member added'),
    ('Membro removido', 'Member removed'),
    ('Acesso staff necessário para publicar', 'Staff access required to publish'),
    ('Broadcast unavailable para este cargo', 'Broadcast unavailable for this rank'),
    ('Failed de autenticação WOM. Clique em Verify now e tente de novo', 'WOM authentication failed. Click Verify now and try again'),
    ('Failed ao remover', 'Failed to remove'),
    ('Somente broadcasts podem ser fixados', 'Only broadcasts can be pinned'),
    ('Acesso staff necessário', 'Staff access required'),
    ('Sincronize o rank antes de solicitar', 'Sync your rank before requesting'),
    ('Failed ao solicitar rank', 'Failed to request rank'),
    ('Solicita\\u00E7\\u00E3o enviada para a staff.', 'Request sent to staff.'),
    ('Você já possui uma request pending', 'You already have a pending request'),
    (' min para solicitar novamente', ' min before requesting again'),
    ('Player não available', 'Player unavailable'),
    ('Failed ao consultar o WOM (erro ', 'Failed to query WOM (error '),
    ('1 request de rank pending.', '1 pending rank request.'),
    (' requests de rank pendentes.', ' pending rank requests.'),
    ('Solicita\\u00E7\\u00E3o inv\\u00E1lida', 'Invalid request'),
    ('Failed ao publicar promoção', 'Failed to publish promotion'),
    ('Add membro', 'Add member'),
    ('Remove membro', 'Remove member'),
    ('Membro', 'Member'),
    ('Nome do membro', 'Member name'),
    ('Delete a etiqueta selecionada e suas associações', 'Delete the selected tag and its associations'),
    ('Select um membro', 'Select a member'),
    ('Remove o membro selecionado da etiqueta', 'Remove the selected member from the tag'),
    ('Somente Owner e Deputy Owner podem alterar', 'Only Owner and Deputy Owner can change this'),
    ('No aviso fixado.', 'No pinned announcement.'),
    ('Atividade registrada', 'Activity recorded'),
    ('Collapse atividade', 'Collapse activity'),
    ('Expand atividade', 'Expand activity'),
    ('Membro MVP', 'MVP member'),
    ('Novo best tempo', 'New best time'),
    ('novo best tempo', 'new best time'),
    ('do clan', 'in the clan'),
    ('do clã', 'in the clan'),
    ('no clan', 'in the clan'),
    ('no clã', 'in the clan'),
    ('para este cargo', 'for this rank'),
    ('de rank', 'rank'),
]

literal_re = re.compile(r'"(?:\\.|[^"\\])*"')

def transform(match):
    s = match.group(0)
    content = s[1:-1]
    for old, new in REPL:
        content = content.replace(old, new)
    return '"' + content + '"'

for path in SRC.glob('*.java'):
    text = path.read_text(encoding='utf-8')
    text = literal_re.sub(transform, text)
    # Promotion regex must parse the English messages we now publish.
    text = text.replace('(?:Promo\\u00E7\\u00E3o: )?', '(?:Promotion: )?')
    path.write_text(text, encoding='utf-8')

# New audit focuses on user-visible Portuguese remnants; technical regex escapes,
# icon glyphs and endpoint names are intentionally ignored.
markers = re.compile(r'\b(?:este|esta|ao|aos|próximos|entrarem|aviso|avisos|solicita(?:ção|ções)|requer|abra|obtida|pontos|passo|equipe|itens|não|somente|membro|membros|parabéns|você|novo|faltantes|indisponível|conexão|erro|publicado|removido|informe|letras|números|adicionado|acesso|necessário|cargo|autenticação|clique|sincronize|antes|já|possui|novamente|consultar|pendentes|inválida|etiqueta|selecionado|alterar|atividade|registrada)\b', re.I)
lines=['# Remaining visible Portuguese review','']
for path in sorted(SRC.glob('*.java')):
    for n,line in enumerate(path.read_text(encoding='utf-8').splitlines(),1):
        for m in literal_re.finditer(line):
            value=m.group(0)[1:-1]
            if markers.search(value):
                lines.append(f'- `{path.name}:{n}` — `{value}`')
(ROOT/'TRANSLATION_REVIEW_2.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('remaining',max(0,len(lines)-2))
