package Object;

import java.io.IOException;

import javax.imageio.ImageIO;

public class ObjKey extends Barang{
	public ObjKey() {
		try {
		image = ImageIO.read(getClass().getResourceAsStream("/Aset/4.png"));
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
