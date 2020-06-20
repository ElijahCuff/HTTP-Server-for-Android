package com.example.androidhttpserver;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;
import java.net.Inet4Address;
import java.net.NetworkInterface;  

public class ServerService extends Service {  
	/**
	 LOTS OF WORK HERE TO UPDATE ANDROID 6+ SUPPORT, ESPECIALLY
	 A SERVICE TO HANDLE THE SERVER FROM OTHER CLASSES USING PERIODIC LOGGING.
	 **/
	Integer portNumber;
	String rootPath;
	String indexPath;
	Server server;
	ServerSocket httpServerSocket;
	public static String periodicLog = "";

    @Nullable  
    @Override  
    public IBinder onBind(Intent intent) {  
        return null;  
	  }  

    @Override  
    public void onStart(Intent intent, int startid) { 
        Bundle params = intent.getExtras();
		String folder = params.getString("folder", getFilesDir() + "/resources");
		String index = params.getString("index", "index.html");
		int port = params.getInt("port", 8080);
		server = new Server(folder, index, port);
		server.start();
		log("Server Started");
		Toast.makeText(this, "Server Started", Toast.LENGTH_LONG).show();
	  }  
    @Override  
    public void onDestroy() {  
		if (httpServerSocket != null) {
			try {
				httpServerSocket.close();
			  } catch (IOException e) {
				e.printStackTrace();
			  }
		  }
		  log("Server Stopped");
        Toast.makeText(this, "Server Stopped", Toast.LENGTH_LONG).show();  
	  }
	  
	public void log(String toLog)
	{
	  periodicLog += toLog+"\n";
	}

	@Override
	public void onTrimMemory(int level) {
		// TODO: Implement this method
		super.onTrimMemory(0);
	  }  

	public class Server extends Thread {

		Server(String rootFolder, String indexFile, int httpPort) {
			rootPath = rootFolder;
			indexPath = indexFile;
			portNumber = httpPort;
		  }

		@Override
		public void run() {
			Socket socket = null;
			try {
				httpServerSocket = new ServerSocket(portNumber);
				while (true) {
					socket = httpServerSocket.accept();
					HttpResponseThread httpResponseThread =  new HttpResponseThread(socket);
					httpResponseThread.start();
				  }
			  } catch (IOException e) {
				// TODO Auto-generated catch block
			  }
		  }
		private class HttpResponseThread extends Thread {

			Socket socket;
			String h1;

			HttpResponseThread(Socket socket) {
				this.socket = socket;
			  }

			@Override
			public void run() {
				// we manage our particular client socketion
				BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
				String fileRequested = null;

				try {
					// we read characters from the client via input stream on the socket
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					// we get character output stream to client (for headers)
					out = new PrintWriter(socket.getOutputStream());

					// get binary output stream to client (for requested data)
					dataOut = new BufferedOutputStream(socket.getOutputStream());

					// get first line of the request from the client
					String input = in.readLine();

					// we parse the request with a string tokenizer
					StringTokenizer parse = new StringTokenizer(input);
					String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client

					// we get file requested
					fileRequested = parse.nextToken().toLowerCase();

					// we support only GET and HEAD methods, we check
					if (!method.equals("GET")  &&  !method.equals("HEAD")) {

						// we return the not supported file to the client
						File file = new File(rootPath, indexPath);
						int fileLength = (int) file.length();
						String contentMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length()));

						//read content to return to client
						byte[] fileData = readFileData(file, fileLength);

						// we send HTTP Headers with data to client
						out.println("HTTP/1.1 501 Not Implemented");
						out.println("Server: Android HTTP Server : 2.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + contentMimeType);
						out.println("Content-length: " + fileLength);

						// blank line between headers and content, very important !
						out.println(); 

						// flush character output stream buffer
						out.flush(); 

						// file
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();

					  } else {

						// GET or HEAD method
						if (fileRequested.endsWith("/")) {
							fileRequested += indexPath;
						  }

						
						 log(input
						 + "\n - " + socket.getInetAddress().toString());
						 

						File file = new File(rootPath, fileRequested);
						int fileLength = (int) file.length();
						String content = getContentType(fileRequested);
						String contentMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length()));
						if (contentMimeType == null) {
							contentMimeType = content;
						  }


						// GET method so we return content
						if (method.equals("GET")) { 
							byte[] fileData = readFileData(file, fileLength);

							// send HTTP Headers
							out.println("HTTP/1.1 200 OK");
							out.println("Server: Android HTTP Server : 2.0");
							out.println("Date: " + new Date());
							out.println("Content-type: " + contentMimeType);
							out.println("Content-length: " + fileLength);

							// blank line between headers and content, very important !
							out.println(); 

							// flush character output stream buffer
							out.flush(); 
							dataOut.write(fileData, 0, fileLength);
							dataOut.flush();
						  }
					  }

				  } catch (FileNotFoundException fnfe) {
					try {
						fileNotFound(out, dataOut, fileRequested);
					  } catch (IOException ioe) {
						//System.err.println("Error with file not found exception : " + ioe.getMessage());
					  }

				  } catch (IOException ioe) {
					//System.err.println("Server error : " + ioe);
				  } finally {
					try {
						in.close();
						out.close();
						dataOut.close();
						socket.close(); // we close socket socketion
					  } catch (Exception e) {
						//System.err.println("Error closing stream : " + e.getMessage());
					  } 
				  }
			  }
			private byte[] readFileData(File file, int fileLength) throws IOException {
				FileInputStream fileIn = null;
				byte[] fileData = new byte[fileLength];
				try {
					fileIn = new FileInputStream(file);
					fileIn.read(fileData);
				  } finally {
					if (fileIn != null) 
					  fileIn.close();
				  }
				return fileData;
			  }

			// return supported MIME Types
			private String getContentType(String fileRequested) {
				if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
				  return "text/html";
				else
				  return "text/plain";
			  }

			private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
				File file = new File(rootPath, indexPath);
				int fileLength = (int) file.length();
				String content = "text/html";
				byte[] fileData = readFileData(file, fileLength);
				out.println("HTTP/1.1 404 File Not Found");
				out.println("Server: Android HTTP Server : 2.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + content);
				out.println("Content-length: " + fileLength);
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			  }

		  }
	  }

  }  
