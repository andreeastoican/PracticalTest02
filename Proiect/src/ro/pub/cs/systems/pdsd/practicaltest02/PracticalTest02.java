package ro.pub.cs.systems.pdsd.practicaltest02;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PracticalTest02 extends Activity {

	protected EditText portServer;
	protected EditText portClient;
	protected EditText clientAddress;
	protected TextView responseTextView;
	protected EditText queryEditText;
	ServerThread serverThread;

	private boolean serverStateRunning;

	public String getContent(String word) {

		String hints = "";
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(
					"http://autocomplete.wunderground.com/aq?query=" + word);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String content = httpClient.execute(httpGet, responseHandler);

			JSONObject result = new JSONObject(content);
			JSONArray jsonArray = result.getJSONArray("RESULTS");
			hints += jsonArray.getJSONObject(0).getString("name");
			for (int k = 1; k < jsonArray.length(); k++) {
				JSONObject jsonObject = jsonArray.getJSONObject(k);
				hints += "," + jsonObject.getString("name");
			}

		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return hints;
	}

	class CommunicationThread extends Thread {
		private Socket socket;

		public CommunicationThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			super.run();
			try {
				BufferedReader bufferedReader = Utilities.getReader(socket);
				String word = bufferedReader.readLine();

				String result = getContent(word);

				PrintWriter printWriter = Utilities.getWriter(socket);
				printWriter.println(result);
				printWriter.flush();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class ServerThread extends Thread {
		private ServerSocket serverSocket;

		public ServerThread() {
		}

		public void closeSocket() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (serverSocket != null) {
							serverSocket.close();
						}
					} catch (IOException ioException) {
						ioException.printStackTrace();
					}
				}
			}).start();
		}

		@Override
		public void run() {
			super.run();
			try {
				serverSocket = new ServerSocket(Integer.parseInt(portServer
						.getText().toString()));

				while (serverStateRunning) {
					Socket socket = serverSocket.accept();
					new CommunicationThread(socket).start();
				}

			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class ClientThread extends Thread {

		@Override
		public void run() {
			super.run();
			try {
				Socket socket = new Socket(clientAddress.getText().toString(),
						Integer.parseInt(portClient.getText().toString()));

				PrintWriter printWriter = Utilities.getWriter(socket);
				printWriter.println(queryEditText.getText().toString());
				printWriter.flush();

				BufferedReader bufferedReader = Utilities.getReader(socket);
				final String result = bufferedReader.readLine();

				responseTextView.post(new Runnable() {
					@Override
					public void run() {
						responseTextView.setText(result);
					}
				});
				socket.close();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_practical_test02);

		portServer = (EditText) findViewById(R.id.server_port_edit_text);

		clientAddress = (EditText) findViewById(R.id.client_address_edit_text);
		portClient = (EditText) findViewById(R.id.client_port_edit_text);

		responseTextView = (TextView) findViewById(R.id.response_text_view);
		queryEditText = (EditText) findViewById(R.id.query_edit_text);

		final Button buttonConectServer = (Button) findViewById(R.id.connect_button);
		Button buttonGetWeather = (Button) findViewById(R.id.get_weather_forecast_button);

		serverStateRunning = false;

		buttonConectServer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!serverStateRunning) {
					serverStateRunning = true;
					buttonConectServer.setText("Disconnect");
					serverThread = new ServerThread();
					serverThread.start();
				} else {
					buttonConectServer.setText("Connect");
					serverStateRunning = false;
					serverThread.closeSocket();
				}
			}
		});

		buttonGetWeather.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new ClientThread().start();
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.practical_test02, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
