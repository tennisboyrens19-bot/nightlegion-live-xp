#!/usr/bin/env python3
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'src/main/java/com/liveon'
PAIRS=[
('Fixar ou desfixar broadcast selecionado','Pin or unpin the selected broadcast'),
('Passo 3: abra a categoria/tier desejada e clique novamente em Sincronizar.','Step 3: open the desired Combat Achievement tier and click Sync again.'),
('Combat Achievements: sincronize com a aba correspondente aberta.','Combat Achievements: sync while the corresponding tab is open.'),
('abra o Character Summary','open Character Summary'),
(': abra o menu Combat Achievements',': open the Combat Achievements menu'),
(' pontos',' points'),
('Requirements faltantes: ','Missing requirements: '),
('Quest points pendentes','Quest points pending'),
('Informe RSN e canal da Twitch','Enter the RSN and Twitch channel'),
('Failed ao associar canal','Failed to link channel'),
('Channel removido','Channel removed'),
('Informe o nome do member','Enter the member name'),
('MVP adicionado','MVP is automatic'),
('MVP removido','MVP is automatic'),
('Apenas Deputy Owner pode alterar','Only Deputy Owner can change this'),
('Etiqueta criada','Tag created'),
('Etiqueta removida','Tag removed'),
('Failed ao salvar','Failed to save'),
('Failed ao publicar','Failed to publish'),
('Publicado','Published'),
('Failed ao alterar message fixada','Failed to change pinned message'),
('Failed ao limpar messages','Failed to clear messages'),
('Acesso staff ausente','Staff access missing'),
('Criar etiqueta','Create tag'),
('Delete etiqueta','Delete tag'),
('Etiqueta','Tag'),
('Crie ou selecione uma etiqueta','Create or select a tag'),
('Select uma etiqueta','Select a tag'),
('Remove o member selecionado da etiqueta','Remove the selected member from the tag'),
(' etiqueta(s)',' tag(s)'),
('Remove canal selecionado','Remove selected channel'),
('AO VIVO','LIVE'),
('Cargo MVP','MVP rank'),
('Remove member MVP selecionado','Remove selected MVP member'),
('1. Abra o <b>Adventure Log</b> da sua POH para importar todos os seus tempos.<br><br>','1. Open the <b>Adventure Log</b> in your POH to import all your times.<br><br>'),
('2. Nos <b>Combat Achievements</b>, abra a página do boss que quiser registrar.<br><br>','2. In <b>Combat Achievements</b>, open the boss page you want to register.<br><br>'),
("<html><div style='text-align:center'>Abra o Adventure Log<br>para importar seus PBs</div></html>","<html><div style='text-align:center'>Open the Adventure Log<br>to import your PBs</div></html>"),
('Ainda não existem PBs nesta categoria.','There are no PBs in this category yet.'),
('Você ainda não possui PB<br>nesta categoria','You do not have a PB<br>in this category yet'),
('Soldier é concedido após 30 dias in the clan. No é necessário solicitar.','Soldier is granted automatically after 30 days in the clan. No request is required.'),
('Next cargo','Next rank'),
('General • somente via Discord','General • Discord only'),
('No rank novo available','No new rank available'),
('Conclua as pendências abaixo e verifique novamente.','Complete the missing requirements below and check again.'),
('Complete ou verifique os requirements destacados antes de solicitar','Complete or verify the highlighted requirements before requesting'),
('Necessário: 200 Quest points e Fire cape.','Required: 200 Quest points and Fire cape.'),
('Necessário: 250 Quest points, Fire cape e Easy Combat Achievements.','Required: 250 Quest points, Fire cape and Easy Combat Achievements.'),
('Necessário: 300 Quest points, Fire cape e Medium Combat Achievements.','Required: 300 Quest points, Fire cape and Medium Combat Achievements.'),
('Necessário: Quest cape, Fire cape e Hard Combat Achievements.','Required: Quest cape, Fire cape and Hard Combat Achievements.'),
("Necessário: Quest cape, Dizana's quiver ou Infernal cape e Elite Combat Achievements.","Required: Quest cape, Dizana's quiver or Infernal cape and Elite Combat Achievements."),
("Necessário: Diary cape, Dizana's quiver, Infernal cape e Master Combat Achievements.","Required: Diary cape, Dizana's quiver, Infernal cape and Master Combat Achievements."),
('Necessário: requirements de Captain e 2300 total level.','Required: Captain requirements and 2300 total level.'),
('Necessário: Diary cape, Max cape e Grandmaster Combat Achievements.','Required: Diary cape, Max cape and Grandmaster Combat Achievements.'),
('Check os requirements pendentes antes de solicitar.','Check the pending requirements before requesting.'),
('! abra o banco','! open your bank'),
]
lit=re.compile(r'"(?:\\.|[^"\\])*"')
def x(m):
 s=m.group(0); c=s[1:-1]
 for a,b in PAIRS:c=c.replace(a,b)
 return '"'+c+'"'
for p in SRC.glob('*.java'):
 s=p.read_text(encoding='utf-8'); s=lit.sub(x,s); p.write_text(s,encoding='utf-8')
# Audit only obvious Portuguese UI phrases; technical regex strings can remain.
marks=re.compile(r'\b(?:fixar|desfixar|passo|abra|sincronize|faltantes|pendentes|informe|canal|removido|apenas|alterar|etiqueta|crie|selecione|selecionado|ainda|existem|possui|categoria|concedido|após|necessário|necessaria|necessário|conclua|verifique|antes|solicitar|banco|ao vivo)\b',re.I)
out=['# Final visible-language audit','']
for p in sorted(SRC.glob('*.java')):
 for n,line in enumerate(p.read_text(encoding='utf-8').splitlines(),1):
  for m in lit.finditer(line):
   v=m.group(0)[1:-1]
   if marks.search(v): out.append(f'- `{p.name}:{n}` — `{v}`')
(ROOT/'TRANSLATION_FINAL_AUDIT.md').write_text('\n'.join(out)+'\n',encoding='utf-8')
print('remaining visible markers',max(0,len(out)-2))
