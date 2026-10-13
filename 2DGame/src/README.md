# 2D Game (K for Khaidir)

Game 2D top-down RPG berbasis Java Swing. Status: masih dikembangkan (sekitar 50%).

## Cara menjalankan

Butuh JDK (dikembangkan dengan JDK 22, sudah dites compile dengan JDK 21).

**Eclipse:** file proyek (`.project`, `.classpath`) sudah ada. Pilih File > Import > Existing Projects into Workspace, pilih folder `2DGame`, lalu jalankan class `Main.Main`.

**Command line** (dari dalam folder `2DGame`):

```
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin:src Main.Main
```

Di Windows, ganti `:` dengan `;` pada perintah `java`, dan untuk `javac` buat daftar file dulu:

```
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
java -cp bin;src Main.Main
```

Folder `src` harus ada di classpath karena gambar dan audio dimuat dari sana.

## Kontrol

| Tombol | Fungsi |
|---|---|
| W A S D | Bergerak |
| Shift | Lari (butuh stamina) |
| Spasi | Lompat |
| Klik kiri | Memukul |
| E | Berinteraksi / bicara dengan NPC |
| 1 / 2 / 3 | Memilih opsi dialog |
| I | Perintah untuk NPC (stay / follow / cancel) |
| Y | Buka/tutup backpack |
| TAB | Buka/tutup statistik pemain |
| M | Buka/tutup mob index |
| Z / X / C | Skill |
| T | Memanggil minion dari Death Totem |
| F5 | Simpan ke slot 1 (muat lewat menu Load Game) |
| F3 | Tampilkan/sembunyikan koordinat player (nyala secara default) |
| F4 | Nyalakan/matikan kotak collision dan log debug di konsol |
| ESC | Menutup panel / keluar dari rumah atau domain |

## Catatan

- File save disimpan di folder `saves/` (tidak ikut git).
- Kredit dan lisensi aset (sprite dan musik): belum dicatat, isi sebelum game dipublikasikan.
