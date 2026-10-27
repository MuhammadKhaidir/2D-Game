"""Bikin sprite biome es (pixel art, dibuat dengan kode) ke folder Aset/ice/.
Jalankan:  python3 make_ice_assets.py <folder_tujuan>
Perlu: Pillow + numpy. Tiap sprite digambar kecil (resolusi seni) lalu diperbesar 4x tanpa blur."""
import sys, os
from pines import make_pine
from others import make_dead, make_rock, make_crystal, make_glow, make_drift, make_pebbles, make_shard, make_tuft

out = sys.argv[1] if len(sys.argv) > 1 else 'ice'
os.makedirs(out, exist_ok=True)
S = 4
pine_cfg = [(1, 4, 0.55, 1.0), (2, 3, 0.62, 1.1), (4, 5, 0.50, 0.85)]
for i, (seed, tiers, snow, slim) in enumerate(pine_cfg):
    make_pine(seed, tiers, snow, slim).save(f'{out}/pine_{i}.png', S)
for i in range(2):
    make_dead(10 + i).save(f'{out}/dead_{i}.png', S)
for i in range(3):
    make_rock(20 + i, i).save(f'{out}/rock_{i}.png', S)
for i in range(3):
    make_crystal(30 + i, i).save(f'{out}/crystal_{i}.png', S)
for i in range(2):
    make_drift(40 + i).save(f'{out}/drift_{i}.png', S)
    make_pebbles(50 + i).save(f'{out}/pebble_{i}.png', S)
    make_shard(60 + i).save(f'{out}/shard_{i}.png', S)
    make_tuft(70 + i).save(f'{out}/tuft_{i}.png', S)
make_glow().save(f'{out}/glow.png')
print('selesai ->', out)
