package com.example.androidhttpserver;
import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.example.androidhttpserver.R;
import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class MainActivity extends Activity {

	TextView infoIp;
	TextView infoMsg;
	String msgLog = "";
	Intent server;
	LinearLayout main;
	ToggleButton serverStatus;
	Button select;
	EditText selectedBox;
	Activity painless;
	NotificationManager notificationManager;
	NotificationCompat.Builder builder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		painless = this;
		infoIp = findViewById(R.id.infoip);
		infoMsg = findViewById(R.id.msg);
		main = findViewById(R.layout.main);
		select = findViewById(R.id.select);
		selectedBox = findViewById(R.id.selected);
		serverStatus = findViewById(R.id.serverStatus);
		select.setOnClickListener(selectPress());
		serverStatus.setOnCheckedChangeListener(serverSwitch());
		String[] allPerms ={Manifest.permission.WRITE_EXTERNAL_STORAGE};
		checkPermissions(allPerms, "This Application requires Internet & Storage to Serve Webpages.");
	  }


	public ToggleButton.OnCheckedChangeListener serverSwitch() {
		return new ToggleButton.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton toggle, boolean on) {
				if (on) {
					if (!selectedIndex) {
						Snackbar.make(infoIp, "Please select the index first", Snackbar.LENGTH_SHORT).show();
						toggle.setChecked(false);
					  } else {
						File selected = new File(selectedIndexFile);
						serverManager(true, selected.getParent(), selected.getName(), 8080);
						Snackbar.make(infoIp, "Starting Android HTTP Server", Snackbar.LENGTH_SHORT).show();
					  }
				  } else {
					Snackbar.make(infoIp, "Stopping Android HTTP Server", Snackbar.LENGTH_SHORT).show();
					serverManager(false, Environment.getExternalStorageDirectory() + "/web", "index.html", 8080);

				  }
			  }
		  };
	  }

	public void popup(String message, int length) {
		Snackbar.make(infoIp, message, length).show();
	  }

	public Button.OnClickListener selectPress() {
		return new Button.OnClickListener(){
			@Override
			public void onClick(View buttonClicked) {
				selectIndex();
			  }
		  };
	  }

	// HERE WE CAN START OR STOP THE SERVER SERVICE
	boolean started;
	String ip;
	public void serverManager(boolean alive, String folderRoot, String indexFile, int portNumber) {
				 
	  
	    ip = getIp();

	    server = new Intent(this, ServerService.class);
		if (alive) {
			if (!started) {
				server.putExtra("folder", folderRoot);
				server.putExtra("index", indexFile);
				server.putExtra("port", portNumber);
				startService(server);

				started = true;
				String showIP = "localhost";
				if (ip.length() < 1) {
					ip = showIP;
				  }
				infoIp.setText(ip + ":" + portNumber + "\n");
				msgLog("STARTED : " + folderRoot);
				startNotification("SERVER", "Active on - " + ip + ":" + portNumber);
			    serverLog(true);
			  } else {
				Snackbar.make(infoIp, "Server is Running", Snackbar.LENGTH_LONG).show();
			  }
		  } else {
			stopService(server);
			started = false;
			stopNotification();
		    serverLog(false);

		  }
	  }

	public void serverLog(boolean run) {
		if (run) {
			// start periodic log
			finishLogging = false;
			periodicLog();
		  } else {
			// set finish flag and let thread die
			finishLogging = true;
		  }
	  }

	public void startNotification(String Title, String Message) {
		builder =
		  new NotificationCompat.Builder(this);

        //Create the intent that’ll fire when the user taps the notification//

		// MainActivity used because we are running as singleTop in Manifest, return to homepage.
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle(Title);
		builder.setAutoCancel(false);
		builder.setOngoing(true);
        builder.setContentText(Message);

        notificationManager =
		  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify("ServerService", 001, builder.build());
	  }
	public void stopNotification() {
		notificationManager.cancel("ServerService", 001);
	  }

	private String getIp() {
		String ip = "";
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
				Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress.nextElement();
					if (inetAddress.isSiteLocalAddress()) {
							ip += "Address: " + inetAddress.getHostAddress();
					  }
				  }
			  }

		  } catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ip += "Something Wrong! " + e.toString() + "\n";
		  }

		return ip;
	  }

	Thread periodicLoop;
	Boolean finishLogging;
	public void periodicLog() {
		periodicLoop = new Thread(new Runnable(){
			  @Override
			  public void run() {
				  while (!finishLogging) {
					  SystemClock.sleep(1000);
					  if (ServerService.periodicLog.length() > 0) {
						  painless.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									infoMsg.append(ServerService.periodicLog);
									ServerService.periodicLog = "";
								  }
							  });
						}
					}
				}
			});
		if (!periodicLoop.isAlive()) {
			periodicLoop.start();
		  } else {
			finishLogging = true;
		  }
	  }



	public void msgLog(final String msg) {
		painless.runOnUiThread(new Runnable() {
			  @Override
			  public void run() {
				  infoMsg.post(new Runnable(){

						@Override
						public void run() {
							infoMsg.append(msg + "\n");
						  }
					  });
				}
			});
	  }


	// DO SOME PERMISSION CHECKS AS PER ANDROID 6 AND ABOVE
	private static int TEMP_PERMISSION_CODE = 101;
    public void checkPermissions(final String[] permissions, String permissionReasons) { 
		int permissionCount = 0;
		while (permissionCount != permissions.length) {
		    final int now = permissionCount;
			TEMP_PERMISSION_CODE = permissionCount;
			if (ContextCompat.checkSelfPermission(this, permissions[permissionCount]) == PackageManager.PERMISSION_DENIED) {
				serverStatus.setEnabled(false);
				Snackbar.make(infoIp, permissionReasons, Snackbar.LENGTH_INDEFINITE).setAction("ALLOW", new View.OnClickListener() {
					  @Override
					  public void onClick(View view) {
						  ActivityCompat.requestPermissions(MainActivity.this, new String[] {permissions[now]}, TEMP_PERMISSION_CODE); 
						}
					}).show();
			  } else {
				serverStatus.setEnabled(true);
			  }
			permissionCount++;
		  }
	  }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { 
        super.onRequestPermissionsResult(requestCode,  permissions, grantResults); 
        if (requestCode == TEMP_PERMISSION_CODE) { 
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { 
				serverStatus.setEnabled(true);
			  } else { 
				Snackbar.make(infoIp, "Denied Storage Access, Server could not Start.", Snackbar.LENGTH_SHORT).show();
				serverStatus.setEnabled(false);
			  }
		  } 

	  }

	// STOP SERVER FROM RESTARTING WHEN OPENED AGAIN < ANDROID 5
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			return true;
		  }
		return super.onKeyDown(keyCode, event);
	  }

	// STOP SERVER FROM RESTARTING WHEN OPENED AGAIN >= ANDROID 5
	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	  }

	boolean selectedIndex;
	String selectedIndexFile;
	public void selectIndex() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/html");
        startActivityForResult(intent, 0);
	  }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
				String usablePath = null;
				try {
					usablePath = getPath(this, uri);
				  } catch (Exception e) {
					msgLog(e.getMessage());
				  }
				selectedBox.setText(usablePath);
				selectedIndexFile = usablePath;
				selectedIndex = true;
				serverStatus.setEnabled(true);
			  }
		  }
	  }

	public String getPath(Context context, Uri uri) throws Exception {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;
			try {
				cursor = context.getContentResolver().query(uri, projection, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				  }
			  } catch (Exception e) {
				throw new Exception(e);
			  }
		  } else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		  }
		return null;
	  } 



  }
