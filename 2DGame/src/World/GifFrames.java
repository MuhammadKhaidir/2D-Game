package World;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.Node;

/**
 * Decoder GIF yang hasilnya berupa frame-frame utuh (sudah di-composite) + lama tiap frame.
 *
 * Kenapa gak pakai ImageIcon kayak GIF lain di proyek ini? ImageIcon selalu looping tanpa henti,
 * sedangkan animasi peti harus diputar SEKALI lalu berhenti di frame terakhir (peti terbuka tetap
 * terbuka sampai ditutup). Dengan frame manual, GamePanel yang pegang kendali kapan mulai/berhenti.
 */
public final class GifFrames {

    /** Frame utuh (ukuran = canvas GIF), urutan sesuai animasi. */
    public final BufferedImage[] frames;
    /** Lama tiap frame dalam satuan tick game (minimal 1). */
    public final int[] delayTicks;

    private GifFrames(BufferedImage[] frames, int[] delayTicks) {
        this.frames = frames;
        this.delayTicks = delayTicks;
    }

    public int size() { return frames.length; }

    /** Total tick dari awal sampai akhir animasi. */
    public int totalTicks() {
        int t = 0;
        for (int d : delayTicks) t += d;
        return t;
    }

    /**
     * Baca GIF dari classpath (mis. "/Aset/OpenChest.gif").
     * @param fps FPS game, buat ngubah delay GIF (1/100 detik) jadi jumlah tick
     * @return null kalau file gak ada / gak bisa dibaca (pemanggil harus siap pakai fallback)
     */
    public static GifFrames load(Class<?> loader, String resourcePath, int fps) {
        URL url = loader.getResource(resourcePath);
        if (url == null) {
            System.err.println("[GifFrames] WARNING: " + resourcePath + " tidak ditemukan.");
            return null;
        }
        try (InputStream is = url.openStream();
             ImageInputStream in = ImageIO.createImageInputStream(is)) {

            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, false, false);
                int count = reader.getNumImages(true);
                if (count <= 0) return null;

                // ukuran canvas = logical screen (kalau gak ada, pakai ukuran frame pertama)
                int canvasW = 0, canvasH = 0;
                IIOMetadata streamMeta = reader.getStreamMetadata();
                if (streamMeta != null) {
                    Node lsd = findChild(streamMeta.getAsTree("javax_imageio_gif_stream_1.0"),
                                         "LogicalScreenDescriptor");
                    if (lsd != null) {
                        canvasW = intAttr(lsd, "logicalScreenWidth", 0);
                        canvasH = intAttr(lsd, "logicalScreenHeight", 0);
                    }
                }
                BufferedImage first = reader.read(0);
                if (canvasW <= 0) canvasW = first.getWidth();
                if (canvasH <= 0) canvasH = first.getHeight();

                BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
                List<BufferedImage> out = new ArrayList<>();
                int[] ticks = new int[count];

                for (int i = 0; i < count; i++) {
                    BufferedImage fr = (i == 0) ? first : reader.read(i);
                    Node tree = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    Node desc = findChild(tree, "ImageDescriptor");
                    Node gce  = findChild(tree, "GraphicControlExtension");

                    int left = desc != null ? intAttr(desc, "imageLeftPosition", 0) : 0;
                    int top  = desc != null ? intAttr(desc, "imageTopPosition", 0) : 0;
                    String disposal = gce != null ? strAttr(gce, "disposalMethod", "none") : "none";
                    int delayCs = gce != null ? intAttr(gce, "delayTime", 10) : 10;
                    if (delayCs <= 1) delayCs = 10; // GIF dengan delay 0/1 diputar ~10cs sama browser

                    // simpan canvas kalau frame ini harus "dibatalin" setelah ditampilkan
                    BufferedImage backup = null;
                    if ("restoreToPrevious".equals(disposal)) backup = copy(canvas);

                    Graphics2D g = canvas.createGraphics();
                    g.drawImage(fr, left, top, null);
                    g.dispose();
                    out.add(copy(canvas)); // snapshot frame utuh

                    // disposal: apa yang terjadi di canvas SETELAH frame ini
                    if ("restoreToBackgroundColor".equals(disposal)) {
                        Graphics2D cg = canvas.createGraphics();
                        cg.setComposite(java.awt.AlphaComposite.Clear);
                        cg.fillRect(left, top, fr.getWidth(), fr.getHeight());
                        cg.dispose();
                    } else if (backup != null) {
                        canvas = backup;
                    }

                    ticks[i] = Math.max(1, (int) Math.round(delayCs * fps / 100.0));
                }
                return new GifFrames(out.toArray(new BufferedImage[0]), ticks);
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            System.err.println("[GifFrames] Gagal membaca " + resourcePath + ": " + e);
            return null;
        }
    }

    // ── helper ──

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return c;
    }

    private static Node findChild(Node parent, String name) {
        if (parent == null) return null;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (name.equals(n.getNodeName())) return n;
        }
        return null;
    }

    private static String strAttr(Node n, String attr, String def) {
        Node a = n.getAttributes() != null ? n.getAttributes().getNamedItem(attr) : null;
        return a != null ? a.getNodeValue() : def;
    }

    private static int intAttr(Node n, String attr, int def) {
        try {
            return Integer.parseInt(strAttr(n, attr, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
