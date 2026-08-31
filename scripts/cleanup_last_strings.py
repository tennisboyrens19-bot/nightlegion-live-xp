#!/usr/bin/env python3
from pathlib import Path
pairs={
'Select um canal':'Select a channel',
' canal(is)':' channel(s)',
'Top 3 ainda sem registros':'Top 3 has no entries yet',
'Ou selecione nos menus abaixo':'Or select from the menus below',
'No PB sincronizado nesta categoria.':'No PB synced in this category.',
'Concedido após 30 dias in the clan.':'Granted after 30 days in the clan.',
'Solicitar ':'Request ',
}
root=Path('src/main/java/com/liveon')
for p in root.glob('*.java'):
 s=p.read_text(encoding='utf-8')
 for a,b in pairs.items(): s=s.replace('"'+a+'"','"'+b+'"')
 p.write_text(s,encoding='utf-8')
