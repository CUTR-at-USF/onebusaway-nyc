package org.onebusaway.nyc.util.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RestApiLibrary {

	private String _host = null;

	private String _apiPrefix = "/api/";

	private int _port = 80;

	private static Logger log = LoggerFactory.getLogger(RestApiLibrary.class);

	public RestApiLibrary(String host, Integer port, String apiPrefix) {
		_host = host;
		_apiPrefix = apiPrefix;
		if(port != null)
			_port = port;
	}

	public URL buildUrl(String baseObject, String... params) throws Exception {
		String url = _apiPrefix;

		url += baseObject;

		if(params.length > 0) {
			url += "/";
			for(int i = 0; i < params.length; i++) {
				String param = params[i];
				url += param;				
				if(i < params.length - 1)
					url += "/";
			}
		}

		return new URL("http", _host, _port, url);
	}	

	public String getContentsOfUrlAsString(URL requestUrl) throws Exception {
		URLConnection conn = requestUrl.openConnection();
		InputStream inStream = conn.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));

		StringBuilder output = new StringBuilder();
		while(br.ready()) {
			output.append(br.readLine());
		}

		br.close();
		inStream.close();

		return output.toString();
	}

	public ArrayList<JsonObject> getJsonObjectsForString(String string) throws Exception {
		JsonParser parser = new JsonParser();
		JsonObject response = (JsonObject)parser.parse(string);

		// check status
		if(response.has("status")) {
			if(!response.get("status").getAsString().equals("OK"))
				throw new Exception("Response error: status was not OK");
		} else
			throw new Exception("Invalid response: no status element was found.");

		ArrayList<JsonObject> output = new ArrayList<JsonObject>();

		// find "content" in the response
		for(Entry<String,JsonElement> item : response.entrySet()) {
			String type = item.getKey();
			JsonElement responseItemWrapper = item.getValue();

			if(type.equals("status"))
				continue;

			// our response "body" is always one array of things
			try {
				for(JsonElement arrayElement : responseItemWrapper.getAsJsonArray()) {
					output.add(arrayElement.getAsJsonObject());
				}
			} catch (Exception e) {
				continue;
			}
		}

		return output;
	}

	/**
	 * Writes config value to the given URL
	 * @param requestUrl URL of remote server
	 * @param value the config value to write
	 * @return response code returned by server
	 * @throws Exception
	 */
	public boolean setContents(URL requestUrl, String value) throws Exception {

		HttpURLConnection conn = getHttpURLConnection(requestUrl);
		int responseCode = 0;
		try {
			String content = "{\"config\":{\"value\":\"" + value +"\"}}";
			conn.connect();

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(content);
			outputStreamWriter.close();

			responseCode = conn.getResponseCode();

		} catch(Exception e) {
			log.error("Error writing value on TDM");
			e.printStackTrace();
		} finally {
			if(conn != null) {
				conn.disconnect();
			}
		}

		return responseCode == HttpURLConnection.HTTP_OK;
	}
	
	/**
	 * Logs the given message to the remote server. The message is sent as a post request to the server
	 * @param baseIdentifier base url indentifier of the remote resource 
	 * @param component chef component/role which initiated logging action
	 * @param priority message priority/severity
	 * @param message the actual message to log
	 * @return response text from the server
	 */
	public String log(String baseIdentifier, String component, Integer priority, String message) {
		String url =  _apiPrefix + baseIdentifier;
		HttpURLConnection conn = null;
		String response = null;
		URL requestUrl = null;
		
		try {
			requestUrl = new URL("http", _host, _port, url);
			conn = getHttpURLConnection(requestUrl);
			conn.connect();
			
			String content = buildMessage(component, priority, message);
			log.info("Writing content : {} to http connection", content);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(content);
			outputStreamWriter.close();
			
			response = conn.getResponseMessage();
			
		} catch (MalformedURLException e) {
			log.error("Error building url : {}", requestUrl.toString());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Error opening http connection to url : {}", requestUrl.toString());
			e.printStackTrace();
		} finally {
			if(conn != null) {
				conn.disconnect();
			}
		}
		
		return response;
	}

	private String buildMessage(String component, Integer priority, String message) {
		StringBuilder messageBuilder = new StringBuilder("{\"component\":\"");
		
		messageBuilder.append(component);
		messageBuilder.append("\",\"priority\":\"");
		messageBuilder.append(priority);
		messageBuilder.append("\",\"message\":\"");
		messageBuilder.append(message);
		messageBuilder.append("\"}");
		
		return messageBuilder.toString();
		
	}
	
	private HttpURLConnection getHttpURLConnection(URL requestUrl) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)requestUrl.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setUseCaches (false);
			conn.setDoOutput(true);
		} catch (IOException e) {
			log.error("Error opening Http Connection for url : {}", requestUrl.toString());
			e.printStackTrace();
		}
		
		return conn;
	}
}
