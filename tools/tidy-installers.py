#!/usr/bin/env python3
"""Archive old generated APKs; keep exactly one install target."""
from pathlib import Path
import zipfile,hashlib
root=Path(__file__).resolve().parents[1]
old=[p for folder in ['.build-initial','.dist-initial'] for p in (root/folder).glob('*.apk')]
archive=root/'.checkpoints/earlier-generated-apks.zip'
if old:
 with zipfile.ZipFile(archive,'a',zipfile.ZIP_DEFLATED) as z:
  names=set(z.namelist())
  for p in old:
   name=str(p.relative_to(root))
   if name not in names:z.write(p,name)
 with zipfile.ZipFile(archive) as z:
  for p in old:
   if hashlib.sha256(z.read(str(p.relative_to(root)))).digest()!=hashlib.sha256(p.read_bytes()).digest():raise RuntimeError('Archive verification failed')
 for p in old:p.unlink()
print('Installable APKs:')
for p in root.rglob('*.apk'):print(p.relative_to(root))
