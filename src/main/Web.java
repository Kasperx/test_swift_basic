package main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.GsonBuilder;

public class Web
{
    private static boolean testFixValues = false;
    private Server server;
    private static String httpbase;
    private static int httpport;
    public Web () {}
    private void initHttpService(String httpbase, int port)
    {
        try
        {
        	System.out.println("webfolder: "+Web.httpbase);
        	System.out.println("port = "+httpport);
        	if(httpport < 0)
        		return;
        	if (server != null)
        		server = null;
        	server = new Server(port);
            ///////////////////////////////////////////////
            // manually handle all requests...
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            ServletHolder sh1 = new ServletHolder(new Web_());
            sh1.setInitParameter("resourceBase", httpbase);
            sh1.setInitParameter("dirAllowed", "true");
            context.addServlet(sh1, httpbase);
            context.setResourceBase(httpbase);
            context.setAllowNullPathInfo(true);
            ServletHolder sh = new ServletHolder(new Web_());
            context.addServlet(sh, "/");
            server.setHandler(context);
            ///////////////////////////////////////////////
            // end: manually handle all requests...
            
//            // automatically handle all requests (not doget, no control)...
//            ResourceHandler resource_handler = new ResourceHandler();
//            resource_handler.setResourceBase(httpbase);
//            //resource_handler.setResourceBase(httpbase+File.separator+"index.html");
//            resource_handler.setDirectoriesListed(true);
//            resource_handler.setWelcomeFiles(new String[]{"index.html"});
//            Path userDir = Paths.get(httpbase);
//            PathResource pathResource = new PathResource(userDir);
//            resource_handler.setBaseResource(pathResource);
//            HandlerList handlers = new HandlerList();
//            handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
//            server.setHandler(handlers);
//            // end: automatically handle all requests (not doget, no control)...
            server.start();
            server.join();
            
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String args[])
    {
        /////////////////////////////////////////////////////
        if(testFixValues)
        {
        List<String> args_temp = new ArrayList<String>();
        args_temp.add("-port");
        args_temp.add("9000");
        args_temp.add("-folder");
        args_temp.add(System.getProperty("user.home")+"/workspace_vscode_html/iafisspy_in_react");
        int f=0;
        args = new String [args_temp.size()];
        for (String temp: args_temp)
            args[f++] = temp;
        }
        /////////////////////////////////////////////////////
        httpbase = System.getProperty("user.dir");
	httpport = 8000;
        for(int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-h") || args[i].equals("-help") || args[i].equals("-?") || args[i].equals("?"))
                showHelp();
            if(args[i].toLowerCase().equals("-port"))
            {
                try {
					httpport = Integer.parseInt(args[i + 1]);
				} catch (NumberFormatException e) {
					httpport = 8000;
				}
            }
            if(args[i].toLowerCase().equals("-folder"))
            {
                if(new File(args[i + 1]).isDirectory())
                    httpbase = args[i + 1];
		else
		    httpbase = System.getProperty("user.dir");
            }
        }
	if(httpport > 0)
	        new Web().initHttpService(httpbase, httpport);
	else
		showHelp();
    }
    
    private static void showHelp ()
    {
            System.out.println();
            System.out.println("### This program is a webserver with a custom backend that connects to the custom webfolder (by parameter) ###");
            System.out.println(" It will show you available database tables to select, shows nearly all content and can export a file (-> new iafisspy)");
            System.out.println("Syntax: [-help | -h | -? | ?] <-port{1025-65536}> [-folder{}]");
            System.out.println("\t Options");
            System.out.println("\t\t -h/-help/-?/?  show this help and exit");
            System.out.println("\t\t -port          the port on that the server opens a connection");
            System.out.println("\t\t -folder        the folder where to find the website");
            System.out.println("\nBye");
            System.exit(0);
    }

    public class Web_ extends HttpServlet
	{
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// coming from js, ask browser what should be transferred
        private String request_stringToGetWebsite = "/";
        private String request_stringToShowTable = "/showtable";
        private String request_getAllTableNames = "/get_all_tablenames";
        private String requestByClient = "";
        public Web_() {}
        
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            requestByClient = request.getRequestURI().toLowerCase();
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            // 1st: answer the normal connection, so return all files
            // request: "/". so blacklist for all other requests, otherwise client asks with specific command like "request_stringToGetElements"
            if (requestByClient.contains(request_stringToGetWebsite)
                    && !requestByClient.equals(request_getAllTableNames)
                    )
            {
                clientRequest_Website(request, response);
            }
            
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            // 2nd: client asks for all tables
            else if (requestByClient.equals(request_getAllTableNames))
            {
                clientRequest_TableNames(request, response);
            }
            
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            // 3nd: client asks for tablename
//                else if (requestByClient.equals(request_deleteTableData))
//                {
//                    clientRequest_DeleteTableData(request, response);
//                }
        } // end of "doget"

        private void clientRequest_Website (HttpServletRequest request, HttpServletResponse response)
        {
            try {
                
                // value of filenames by client come with a slash, but java doesn't find files with slash, so cut first char...
                String wantedFileFromClient = httpbase+File.separator+request.getServletPath().substring(1);
                String fileContent;
                if (new File(wantedFileFromClient+"index.html").exists())
                	wantedFileFromClient += "index.html";
                fileContent = readFile(wantedFileFromClient);
                PrintWriter out = response.getWriter();
                if (wantedFileFromClient.endsWith(".html"))
                    response.setContentType("text/html;charset=UTF-8");
                else if (wantedFileFromClient.endsWith(".css"))
                    response.setContentType("text/css;charset=UTF-8");
                else if (wantedFileFromClient.endsWith(".js"))
                    response.setContentType("application/json;charset=UTF-8");
                
                response.addHeader("Access-Control-Allow-Origin", "*");
                response.setStatus(HttpServletResponse.SC_OK);
                response.reset();
                out.print(fileContent);
                //out.close();
            }
            catch (Exception e) {
                System.err.println(e.toString());
            }
        }

        private void clientRequest_TableNames (HttpServletRequest request, HttpServletResponse response)
        {
            JSONObject json_mapForJSON = null;
            try
            {
                /////////////////////////////////////////////////////
                /////////////////////////////////////////////////////
                // get column names of wanted table
//                columnNames = new ArrayList();
//                ResultSet rs = dbc.getResultSet("select tablename from tables where TYPE = 'TABLE'");
                JSONArray array = new JSONArray();
//                while(dbc.next(rs))
//                {
//                    array.put(dbc.getString(rs, 1));
//                }
//                if (showPasswords)
//                    array.put("showpasswords");
//                dbc.closeSTMT(pstmt, rs);
                array.put("hallo1");
                array.put("hallo2");
                array.put("hallo3");
                json_mapForJSON = new JSONObject();
                json_mapForJSON.put("Tablenames", array);
            String result = new GsonBuilder().create().toJson(json_mapForJSON);
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.print(result);
            out.close();
            }
            catch (Exception e) {
                System.err.println(e.toString());
            }
        }
        
    private List <List<String>> vectorToArrayList2D (Vector dataRows)
    {
        List <List<String>> array = new ArrayList<List<String>>();
        
        for (int i=0; i<dataRows.size(); i++)
        {
             Vector data_cols = (Vector)dataRows.elementAt(i);
             ArrayList <String> temp = new ArrayList<String>();
             for(int col=0; col<data_cols.size(); col++)
                 temp.add(data_cols.elementAt(col).toString());
             array.add(temp);
        }
        return array;
    }
    
    private String readFile (String fileName)
    {        
        List<String> filecontent = loadFile(fileName);
        if(filecontent == null)
        	return null;
        String ckasl= "";
        for (String temp: filecontent)
            ckasl += temp + "\n";
        filecontent.clear(); 
        System.out.println("Reading file: "+fileName);
        return ckasl;
    }

    public List <String> loadFile(String fpath)
    {
        try
        {
            return new ArrayList(
                    Files.readAllLines(
                            Paths.get(fpath),
                            StandardCharsets.UTF_8)
                    );
            
        } catch (Exception e) {
        	System.err.println("File not found: "+fpath);
            return null;
        }
    }
    
    private ArrayList <String> makeListEntriesUnique (ArrayList <String> array)
    {
        return (ArrayList) array.stream().distinct().collect(Collectors.toList());
    }
    }
}

