package com.example.androidhttpserver;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class Files {
    private Files() {
	  }

    public static boolean externalStorageMounted() {
        String state = Environment.getExternalStorageState();
        return "mounted".equals(state) || "mounted_ro".equals(state);
	  }

    public static int chmod(File path, int mode) throws Exception {
        return ((Integer) Class.forName("android.os.FileUtils").getMethod("setPermissions", new Class[]{String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE}).invoke(null, new Object[]{path.getAbsolutePath(), Integer.valueOf(mode), Integer.valueOf(-1), Integer.valueOf(-1)})).intValue();
	  }

    public static boolean recursiveChmod(File root, int mode) throws Exception {
        boolean success;
        if (chmod(root, mode) == 0) {
            success = true;
		  } else {
            success = false;
		  }
        for (File path : root.listFiles()) {
            int i;
            if (path.isDirectory()) {
                success = recursiveChmod(path, mode);
			  }
            if (chmod(path, mode) == 0) {
                i = 1;
			  } else {
                i = 0;
			  }
            success = (i == 1);
		  }
        return success;
	  }

    public static boolean delete(File path) {
        int i = 0;
        boolean result = true;
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] listFiles = path.listFiles();
                while (i < listFiles.length) {
                    result &= delete(listFiles[i]);
                    i++;
				  }
                result &= path.delete();
			  }
            if (path.isFile()) {
                result &= path.delete();
			  }
            if (!result) {
                Log.e("", "Delete failed;");
			  }
            return result;
		  }
        Log.e("", "File does not exist.");
        return false;
	  }

    public static File copyFromStream(String name, InputStream input) {
        if (name == null || name.length() == 0) {
            Log.e("", "No script name specified.");
            return null;
		  }
        File file = new File(name);
        if (!makeDirectories(file.getParentFile(), 493)) {
            return null;
		  }
        try {
            copy(input, new FileOutputStream(file));
            return file;
		  } catch (Exception e) {
            Log.e("", "", e);
            return null;
		  }
	  }

    public static boolean makeDirectories(File directory, int mode) {
        File parent = directory;
        while (parent.getParentFile() != null && !parent.exists()) {
            parent = parent.getParentFile();
		  }
        if (!directory.exists()) {
            Log.v("", "Creating directory: " + directory.getName());
            if (!directory.mkdirs()) {
                Log.e("", "Failed to create directory.");
                return false;
			  }
		  }
        try {
            recursiveChmod(parent, mode);
            return true;
		  } catch (Exception e) {
            Log.e("", "", e);
            return false;
		  }
	  }

    public static boolean rename(File file, String name) {
        return file.renameTo(new File(file.getParent(), name));
	  }

    public static String readToString(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
		  }
        FileReader reader = new FileReader(file);
        StringBuilder out = new StringBuilder();
        char[] buffer = new char[4096];
        while (true) {
            int numRead = reader.read(buffer);
            if (numRead <= -1) {
                reader.close();
                return out.toString();
			  }
            out.append(String.valueOf(buffer, 0, numRead));
		  }
	  }

    public static String readFromAssetsFile(Context context, String name) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(name)));
        StringBuilder builder = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                return builder.toString();
			  }
            builder.append(line);
		  }
	  }

	public boolean copyFileFromAssets(Context ctx, String fileName, String outFileName) {
        Exception e;
        OutputStream outputStream;
        try {
            InputStream in = ctx.getAssets().open(fileName);
            OutputStream out = new FileOutputStream(outFileName);
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    int read = in.read(buffer);
                    if (read == -1) {
                        try {
                            in.close();
                            out.flush();
                            out.close();
                            return true;
						  } catch (Exception e2) {
                            e = e2;
                            outputStream = out;
                            e.printStackTrace();
                            return false;
						  }
					  }
                    out.write(buffer, 0, read);
				  }
			  } catch (IOException e3) {
                e3.printStackTrace();
                outputStream = out;
                return false;
			  }
		  } catch (Exception e4) {
            e = e4;
            e.printStackTrace();
            return false;
		  }
	  }

    public boolean copyFile(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            while (true) {
                int len = in.read(buf);
                if (len <= 0) {
                    in.close();
                    out.close();
                    return true;
				  }
                out.write(buf, 0, len);
			  }
		  } catch (IOException e) {
            return false;
		  }
	  }

    public boolean saveTextFile(String fileName, String text) {
        try {
            FileWriter fWriter = new FileWriter(fileName);
            fWriter.write(text);
            fWriter.flush();
            fWriter.close();
            return true;
		  } catch (Exception e) {
            e.printStackTrace();
            return false;
		  }
	  }

    public String readTextFileFromAssets(Context ctx, String fileName) {
        try {
            InputStream stream = ctx.getAssets().open(fileName);
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);
            stream.close();
            String text = new String(buffer);
            return text;
		  } catch (IOException e) {
            return null;
		  }
	  }

    public static String readTextFile(String fileName) {
        File file = new File(fileName);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    return text.toString();
				  }
                text.append(line);
                text.append('\n');
			  }
		  } catch (IOException e) {
            return null;
		  }
	  }

    public static boolean deleteFolder(String path) {
        File dir = new File(path);
        Log.d("DeleteRecursive", "DELETEPREVIOUS TOP" + dir.getPath());
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String file : children) {
                File temp = new File(dir, file);
                if (temp.isDirectory()) {
                    Log.d("DeleteRecursive", "Recursive Call" + temp.getPath());
                    deleteFolder(temp.getPath());
				  } else {
                    Log.d("DeleteRecursive", "Delete File" + temp.getPath());
                    if (!temp.delete()) {
                        Log.d("DeleteRecursive", "DELETE FAIL");
                        return false;
					  }
				  }
			  }
            dir.delete();
		  }
        return true;
	  }


	private static final int BUFFER_SIZE = 8192;

	public static int copy(InputStream input, OutputStream output) throws Exception, IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
		BufferedOutputStream out = new BufferedOutputStream(output, BUFFER_SIZE);
		int count = 0;
		while (true) {
			try {
				int n = in.read(buffer, 0, BUFFER_SIZE);
				if (n == -1) {
					break;
				  }
				out.write(buffer, 0, n);
				count += n;
			  } finally {
				try {
					out.close();
				  } catch (IOException e) {
					Log.e("", e.getMessage(), e);
				  }
				try {
					in.close();
				  } catch (IOException e2) {
					Log.e("", e2.getMessage(), e2);
				  }
			  }
		  }
		out.flush();
		try {
			in.close();
		  } catch (IOException e22) {
			Log.e("", e22.getMessage(), e22);
		  }
		return count;
	  }

  }

