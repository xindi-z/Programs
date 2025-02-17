package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 *
 **/

 //imported file readers

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

//import javax.imageio.ImageIO;

public class WebWorker implements Runnable
{
	//added 3 global variables, valid, path and doc
	private Socket socket;
	public boolean valid = false;
	public String path;
	public File doc;
	
	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			readHTTPRequest(is);
			String contentType = readType(path);
			writeHTTPHeader(os, contentType);
			writeContent(os,contentType);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private void readHTTPRequest(InputStream is) 
	{	
		//getting file path after GET
		String line;
		String url = null;
		
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				//if i get the path, i check if it exist, if exist, set valid to true
				//updated <project root>/www/res/acc/test.html
				if(url == null){
					url = line;
					path = url.split("\s")[1];
					path = path.substring(1);
					path = "www\\res\\acc\\" + path;
					doc = new File(path);
					valid = doc.exists();
				}
				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return;
	}

	//returns the content type, so we can serve image files in the GIF, JPEG, jpg and PNG formats
	private String readType(String path){
		if (path.contains("gif")) return "image/gif";
		if (path.contains("jpeg")) return "image/jpeg";
		if (path.contains("png")) return "image/png";
		if (path.contains("jpg")) return "image/jpg";
		return "text/html";
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
	{
		//if file exist, write 200 ok, else write 404
		if(valid){
			os.write("HTTP/1.1 200 OK\n".getBytes());
		}
		else{
			os.write("HTTP/1.1 404\n".getBytes());
		}

		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Xindi's very own server\n".getBytes());
		//os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		//os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, String contentType) throws Exception{
		//check existing file again
		
		if(valid){
			//if contentType has html, read html page
			if(contentType.contains("html")){

				FileReader fr = new FileReader(path);
				BufferedReader read = new BufferedReader(new FileReader(path));

				//creating substitution for tags
				String webServer = "Xindi's Server";
				Date date = new Date();
				String sub;
				String lines = "";

				//replace <cs371date> and <cs371server> with something else
				//replace img with img link
				while((sub = read.readLine()) != null){
					lines += sub;
				}
				lines = lines.replaceAll("<cs371date>",date.toString());

				lines = lines.replaceAll("<cs371server>",webServer);

				lines = lines.replaceAll("\"img\"", "pix.jpg");

				//contents that is printing to the screen
				os.write("<html><head></head><body>\n".getBytes());
				os.write(lines.getBytes());
				os.write("</body></html>\n".getBytes());
				read.close();
				fr.close();
			}
			//if contentType has img, read img
			else if(contentType.contains("image")){
				FileInputStream img = new FileInputStream(doc);
				int cursor;

				while((cursor = img.read()) != -1) {
					os.write(cursor);
				}

				img.close();
			}
		}
		//print 404 to screen if link not found
		else{
			os.write("<html><head></head><body>\n".getBytes());
			os.write(("<h1> 404 not found</h1>\n").getBytes());
			os.write("</body></html>\n".getBytes());
		}
	}
} // end class