package com.example.androidhttpserver;
import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzip {
    private String outputFolder;
    private String inputZipFile;
    Context me;

    public Unzip(Context c, String zipFile, String location) {
        this.inputZipFile = zipFile;
        this.outputFolder = location;
        this.me = c;
        checkFolder(location);
	  }

    public void unzip() {
        ZipInputStream zin = null;
        AssetManager assetManager = this.me.getAssets();
        try {
            zin = new ZipInputStream(assetManager.open(this.inputZipFile));
		  } catch (IOException e2) {
            e2.printStackTrace();
		  }
        while (true) {
            try {
				ZipEntry ze = zin.getNextEntry();

				if (ze == null) {
					zin.close();
					try {
						Files.chmod(new File(this.outputFolder + ze.getName()), 493);
					  } catch (Exception e) {}

					return;
				  }

				try {
					if (ze.isDirectory()) {
						checkFolder(ze.getName());
					  } else {
						FileOutputStream fout = new FileOutputStream(this.outputFolder + ze.getName());
						byte[] buffer = new byte[4096];
						while (true) {
							int length = zin.read(buffer);
							if (length <= 0) {
								break;
							  }
							fout.write(buffer, 0, length);
						  }
						zin.closeEntry();
						fout.close();
					  }
				  } catch (Exception e) {
					return;
				  }
			  } catch (IOException e) {}
		  }
	  }

    private void checkFolder(String dir) {
        File f = new File(this.outputFolder + dir);
        if (!f.isDirectory()) {
            f.mkdirs();
            try {
                Files.chmod(f.getParentFile(), 493);
			  } catch (Exception e) {
                e.printStackTrace();
			  }
		  }
	  }
  }

