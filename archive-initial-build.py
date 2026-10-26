#!/usr/bin/env python3
"""Keep the initial build and source reversible before the UI/companion update."""
from pathlib import Path
import zipfile,shutil
root=Path(__file__).resolve().parents[1]
archive=root/'.checkpoints';archive.mkdir(exist_ok=True)
out=archive/'FluidKeys-0.1.0-source-and-apk.zip'
if not out.exists():
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  for folder in ['app','tools','tests','dist']:
   for p in (root/folder).rglob('*'):
    if p.is_file():z.write(p,p.relative_to(root))
  z.write(root/'README.md','README.md')
 print('Preserved initial source and APK:',out)
for folder in ['build','dist']:
 source=root/folder
 target=root/('.'+folder+'-initial')
 if source.exists() and not target.exists():source.rename(target)
install=root/'Install';install.mkdir(exist_ok=True)
if not (install/'FluidKeys.apk').exists():
 shutil.copyfile(root/'.dist-initial/FluidKeys-0.1.0-arm64.apk',install/'FluidKeys.apk')
print('Current install APK:',install/'FluidKeys.apk')
