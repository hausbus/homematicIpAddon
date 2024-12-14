package de.hausbus.homematic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.util.ServerRunner;

import de.hausbus.com.BusException;
import de.hausbus.com.BusHandler;
import de.hausbus.com.HausBusUtils;
import de.hausbus.com.HomeServer;
import de.hausbus.com.HomeserverException;
import de.hausbus.com.IBusDevice;
import de.hausbus.com.IHomeServer;
import de.hausbus.homematic.util.MultipartUtility;
import de.hausbus.proxy.Ethernet;
import de.hausbus.proxy.controller.params.ESlotType;
import de.hausbus.proxy.controller.params.MLogicalButtonMask;
import de.hausbus.proxy.ethernet.data.Configuration;

public class Webserver extends NanoHTTPD
{
    public static final int PORT = 8911;
    private static String host = "";
    private final Logger logger = Logger.getLogger(getClass().getName());

    private String downloadError = "";
    private boolean downloadActive = false;

    public static void main(String[] args) throws Exception
    {
        ServerRunner.run(Webserver.class);
    }

    public Webserver() throws Exception
    {
        super(PORT);
        // start();
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();
        host = session.getHeaders().get("host");
        int pos = host.indexOf(":");
        if (pos != -1)
            host = host.substring(0, pos);

        logger.finer("host = " + host);

        if (uri.contains("/index"))
            return handleIndex(params);
        else if (uri.contains("/resetCache"))
            return handleResetCache(params);
        else if (uri.contains("/searchDevices"))
            return handleSearchDevices(params);
        else if (uri.contains("/restartCCU"))
            return handleRestartCCU(params);
        else if (uri.contains("/restart"))
            return handleRestart(params);
        else if (uri.contains("/addonUpdateCheck"))
            return handleAddonUpdateCheck(params);
        else if (uri.contains("/addonUpdate"))
            return handleAddonUpdate(params);
        else if (uri.contains("/support"))
            return handleSupport(params);
        else
        {
            logger.warning("Not found: " + uri);
            return Response.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "");
        }
    }

    private Response handleSearchDevices(Map<String, String> params)
    {
        String error = "";
        try
        {
            HomematicBridge.instance.homeserver.searchDevices(false);
        } catch (BusException | HomeserverException e)
        {
            // error = e.toString()+"<br><br>";
        }

        String content = loadPage("message.html");
        content = content.replace("%MESSAGE%", "Gerätesuche wurde gestartet. Posteingang prüfen!");
        content = content.replace("%STATUS%", "<br><br>" + error + "<a href='/index'>Zurück</a>");
        content = removeForward(content);

        return Response.newFixedLengthResponse(content);
    }

    private Response handleResetCache(Map<String, String> params)
    {
        String error = "";
        try
        {
            HomematicBridge.instance.homeserver.searchDevices(true);
        } catch (BusException | HomeserverException e)
        {
            // error = e.toString()+"<br><br>";
        }

        String content = loadPage("message.html");
        content = content.replace("%MESSAGE%", "Gerätecache wurde gelöscht und Geräte neu eingelesen");
        content = content.replace("%STATUS%", "<br><br>" + error + "<a href='/index'>Zurück</a>");
        content = removeForward(content);

        return Response.newFixedLengthResponse(content);
    }

    private Response handleIndex(Map<String, String> params)
    {
        String submitted = params.get("submitted");
        if ("1".equals(submitted))
        {
            HomematicBridge bridge = HomematicBridge.instance;
            if (bridge != null)
            {
                bridge.setLogLevel(params.get("logging"));
                String autoDelete = params.get("autoDelete");
                if ("1".equals(autoDelete))
                    bridge.autoDeleteUnreachableDevices = true;
                else
                    bridge.autoDeleteUnreachableDevices = false;
                bridge.save();
            }

            String bridgeIp = params.get("bridgeIp");
            if (bridgeIp != null)
                HomematicBridge.instance.configureBridgeIp(bridgeIp);

            String patch = params.get("patch");
            if (patch != null)
            {
                logger.finer("Patch: " + patch);
                boolean needsReboot = patch(patch);
                logger.finer("needsReboot = " + needsReboot);
                if (needsReboot)
                    return handleRestartCCU(params);
            }
        }

        String content = loadPage("index.html");
        content = content.replace("%VERSION%", "Version " + HomematicBridge.version);
        content = content.replace("%BRIDGE_IP%", HomematicBridge.bridgeIp);

        String status = "<table cellspacing=0 cellpadding=0 border=0><tr><td>";
        if (HomematicBridge.ccuConnected)
            status += "Mit CCU verbunden</td><td> &nbsp;&nbsp;<img src='" + getImageRoot() + "ok.png' border=0>";
        else
            status += "Nicht mit CCU verbunden. Vielleicht muss die CCU neu gestartet werden.<br>Falls gerade neu gestartet wurde, kann es noch ein paar Sekunden dauern.... <script>setTimeout(() => {location.reload()},5000);</script></td><td> &nbsp;&nbsp;<img src='" + getImageRoot() + "nok.png' border=0>";
        status += "</td></tr></table>";
        content = content.replace("%STATUS%", status);

        status = "<table cellspacing=0 cellpadding=0 border=0><tr><td>";
        try
        {
            String addonVersion = HausBusUtils.readUrl("http://www.haus-bus.de/homematicip/VERSION");
            boolean addonUpToDate = HomematicBridge.version.equalsIgnoreCase(addonVersion);
            // addonUpToDate = false;
            boolean firmwareUpToDate = true;
            if (HomematicBridge.instance != null)
                firmwareUpToDate = !HomematicBridge.instance.isAnyDeviceUpdateable();

            if (addonUpToDate && firmwareUpToDate)
                status += "Alles auf neustem Stand</td><td> &nbsp;&nbsp;<img src='" + getImageRoot() + "ok.png' border=0>";
            else
            {
                if (!addonUpToDate)
                    status += "Neue Version vom Haus-Bus Addon verfügbar (" + addonVersion + ").<br><a href='addonUpdate'>Hier klicken, um das Addon auch ohne CCU Neustart zu aktualisieren</a><br><br>";
                if (!firmwareUpToDate)
                    status += "Es gibt Firmwareaktualisierungen.<br>Firmware sollte nur bei Problemen aktualisiert werden im CCU Menü<br>Einstellungen -> Geräte-Firmware - Übersicht<br>";
            }
        } catch (IOException e)
        {
            logger.log(Level.SEVERE, "", e);
            status += "Die aktuelle Version konnte nicht abgefragt werden: " + e.toString() + "</td><td> &nbsp;&nbsp;<img src='" + getImageRoot() + "nok.png' border=0>";
        }
        status += "</td></tr></table>";
        content = content.replace("%UPDATES%", status);

        if (HomematicBridge.instance != null)
        {
            Level level = HomematicBridge.instance.logLevel;
            String logging = "";
            String selected = "";
            if (level == Level.ALL)
                selected = "selected";
            logging += "<option value='ALL' " + selected + ">Alles loggen";
            selected = "";
            if (level == Level.FINE)
                selected = "selected";
            logging += "<option value='FINE' " + selected + ">Normal loggen";
            selected = "";
            if (level == Level.SEVERE)
                selected = "selected";
            logging += "<option value='SEVERE' " + selected + ">Nur Fehler loggen";
            content = content.replace("%LOGGING%", logging);

            String checked = "";
            if (HomematicBridge.instance.autoDeleteUnreachableDevices)
                checked = "checked";
            content = content.replace("%AUTO_DELETE_CHECKED%", checked);
        }

        String news = null;
        try
        {
            news = HausBusUtils.readUrl("http://www.haus-bus.de/homematicip/news.php");
        } catch (IOException e)
        {
            logger.log(Level.SEVERE, "", e);
        }
        if (news == null || news.isEmpty())
        {
            content = content.replace("%NEWS%", "");
            content = content.replace("%THE_NEWS%", "");
        } else
        {
            content = content.replace("%NEWS%", "Neuigkeiten");
            content = content.replace("%THE_NEWS%", news);
        }

        return Response.newFixedLengthResponse(content);
    }

    private boolean patch(String patch)
    {
        logger.finer("Patch: " + patch);

        if ("100".equals(patch))
        {
            logger.warning("PATCH 100: exec patch");
            exec("patch", false);
            return false;
        } else if ("101".equals(patch))
        {
            logger.warning("PATCH 101: reverting images");
            writePatchFile(new ArrayList<String>(Arrays.asList("cp /www/config/devdescr/DEVDB.tcl.backup /www/config/devdescr/DEVDB.tcl", "cp /www/webui/webui.js.backup /www/webui/webui.js")));
            exec("chmod 777 patch", false);
        } else if ("103".equals(patch))
        {
            logger.warning("PATCH 103: reverting images");
            exec("mount -o remount,rw /", false);
            exec("cp /www/config/devdescr/DEVDB.tcl.backup /www/config/devdescr/DEVDB.tcl", false);
            exec("cp /www/webui/webui.js.backup /www/webui/webui.js", false);
            return false;
        } else if ("104".equals(patch))
        {
            logger.warning("PATCH 104: remount");
            exec("mount -o remount,rw /", false);
            return false;
        } else if (patch != null && patch.startsWith("999"))
        {
            String forcedDestination = patch.substring(3);
            try
            {
                InetAddress.getByName(forcedDestination);
                logger.finer("configuring forcedDestination " + forcedDestination);
                BusHandler.instance().forceDestination = forcedDestination;
            } catch (Exception e)
            {
                logger.log(Level.SEVERE, forcedDestination, e);
            }
            return false;
        } else if (patch != null && patch.startsWith("998"))
        {
            // 9983510_192.168.178.4
            try
            {
                String rest = patch.substring(3);
                int trenner = rest.indexOf("_");
                String deviceId = rest.substring(0, trenner);
                rest = rest.substring(trenner + 1);

                IHomeServer server = HomeServer.getInstance();
                IBusDevice device = server.getDevice(Integer.parseInt(deviceId));
                if (device == null)
                    logger.warning("could not find device " + deviceId);
                else
                {
                    List<Ethernet> ethernet = device.getFeatures(Ethernet.class, true);
                    if (ethernet.isEmpty())
                        logger.warning("could not find ethernet for device " + deviceId);
                    else
                    {
                        Configuration config = ethernet.get(0).getConfiguration();

                        StringTokenizer tk = new StringTokenizer(rest, ".");
                        config.setServer_IP0(Integer.parseInt(tk.nextToken()));
                        config.setServer_IP1(Integer.parseInt(tk.nextToken()));
                        config.setServer_IP2(Integer.parseInt(tk.nextToken()));
                        config.setServer_IP3(Integer.parseInt(tk.nextToken()));
                        logger.finer("configuring " + config);
                        ethernet.get(0).setConfiguration(config);
                        device.reset();
                    }
                }
            } catch (Throwable e)
            {
                logger.log(Level.SEVERE, patch, e);
            }
            return false;
        } else if (patch != null && patch.startsWith("7777"))
        {
            String deviceIdStr = patch.substring(4);
            int deviceId = Integer.parseInt(deviceIdStr);
            IBusDevice device = HomeServer.getInstance().getDevice(deviceId);
            logger.finer("convert to rollo " + deviceId + " device = " + device);
            if (device != null)
            {
                try
                {
                    device.setConfiguration(2, new MLogicalButtonMask(0), deviceId, 0, ESlotType.SHUTTER, ESlotType.SHUTTER, ESlotType.SHUTTER, ESlotType.SHUTTER, ESlotType.SHUTTER, ESlotType.SHUTTER, ESlotType.SHUTTER, ESlotType.SHUTTER);
                    device.reset();
                } catch (BusException e)
                {
                }

                try
                {
                    HomematicBridge.instance.homeserver.searchDevices(true);
                } catch (BusException | HomeserverException e)
                {
                    // error = e.toString()+"<br><br>";
                }
            }
            logger.warning("fehlerhafte ID " + deviceIdStr);
            return false;
        } else if (patch != null && patch.startsWith("6666"))
        {
            String deviceIdStr = patch.substring(4);
            logger.warning("lösche ID " + deviceIdStr);
            HomematicBridge.instance.deleteDevice(deviceIdStr);
            return false;
        }

        return false;
    }

    private void writePatchFile(List<String> commands)
    {
        try
        {
            HausBusUtils.writeFile("patch", commands, true);
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "patch failed: " + commands, e);
        }
    }

    private Response handleAddonUpdate(Map<String, String> params)
    {
        new Thread(() -> {
            downloadActive = true;
            downloadError = "";

            HausBusUtils.sleep(1000);
            try
            {
                exec("./updateAddon", true);
            } catch (RuntimeException e)
            {
                downloadError = e.toString();
            } finally
            {
                downloadActive = false;
            }
        }).start();

        String content = loadPage("message.html");
        content = content.replace("%ERROR%", "");

        content = content.replace("%MESSAGE%", "Das Haus-Bus Addon wird aktualisiert.<br><br>Bitte warten, die Seite lädt automatisch neu...");
        content = content.replace("%STATUS%", "");
        content = content.replace("%FORWARD_TIME%", "5000");
        content = content.replace("%FORWARD_TARGET%", "addonUpdateCheck");

        return Response.newFixedLengthResponse(content);
    }

    private void exec(String command, boolean throwException)
    {
        logger.finer("exec " + command);
        try
        {
            Process process = Runtime.getRuntime().exec(command);
            Reader r = new InputStreamReader(process.getInputStream());
            BufferedReader in = new BufferedReader(r);
            String line;
            while ((line = in.readLine()) != null)
            {
                logger.finer(line);
            }
            in.close();
        } catch (IOException e)
        {
            logger.log(Level.SEVERE, "", e);
            if (throwException)
                throw new RuntimeException(e);
        }
    }

    private Response handleAddonUpdateCheck(Map<String, String> params)
    {
        String content = loadPage("message.html");

        if (downloadError != null && !downloadError.isEmpty())
        {
            content = content.replace("%MESSAGE%", "Upps, da ist etwas schief gelaufen!");
            content = content.replace("%STATUS%", downloadError + "<br><br><a href='/index'>Zurück</a>");
            content = removeForward(content);
        } else if (!downloadActive)
        {
            content = content.replace("%ERROR%", "");
            content = content.replace("%MESSAGE%", "Update abgeschlossen");
            content = content.replace("%STATUS%", "");
            content = content.replace("%FORWARD_TIME%", "10");
            content = content.replace("%FORWARD_TARGET%", "restart");
        } else
        {
            content = content.replace("%ERROR%", "");
            content = content.replace("%MESSAGE%", "Update wird durchgeführt. Bitte warten....");
            content = content.replace("%STATUS%", "");
            content = content.replace("%FORWARD_TIME%", "5000");
            content = content.replace("%FORWARD_TARGET%", "addonUpdateCheck");
        }

        return Response.newFixedLengthResponse(content);
    }

    private Response handleSupport(Map<String, String> params)
    {
        String error = "";
        String submitted = params.get("submitted");
        if ("1".equals(submitted))
        {
            String email = params.get("email");
            String phone = params.get("phone");
            String message = params.get("message");
            if ((email == null || email.isEmpty()) && (phone == null || phone.isEmpty()))
                error = "Bitte Emailadresse oder Telefonnummer angeben";
            else if (message == null || message.isEmpty())
                error = "Bitte Fehlerbeschreibung angeben";
            else
            {
                String bugReportError = null;
                try
                {
                    prepareBugreport();
                } catch (IOException e)
                {
                    logger.log(Level.SEVERE, "error creating bugreport", e);
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    bugReportError = pw.toString();
                }

                String ticket = createTicket(bugReportError, email + "/" + phone, message);
                if (ticket.startsWith("Fehler"))
                {
                    String content = loadPage("message.html");
                    content = content.replace("%MESSAGE%", "Upps, da ist etwas schief gelaufen!");
                    content = content.replace("%STATUS%", ticket + "<br><br><a href='/index'>Zurück</a>");
                    content = removeForward(content);
                    return Response.newFixedLengthResponse(content);
                } else
                {
                    String content = loadPage("message.html");
                    content = content.replace("%MESSAGE%", "Alles klar, die Nachricht wurde gemeldet.<br>Ihr Ticketnummer lautet " + ticket + "<br>Wir melden uns so schnell wie möglich!");
                    content = content.replace("%STATUS%", downloadError + "<br><br><a href='/index'>Zurück</a>");
                    content = removeForward(content);
                    return Response.newFixedLengthResponse(content);
                }
            }
        }

        String content = loadPage("support.html");
        if (error != "")
            error = "<font color=#bb0000><b>" + error + "</font>";
        content = content.replace("%ERROR%", error);
        return Response.newFixedLengthResponse(content);
    }

    private String createTicket(String bugReportError, String email, String message)
    {
        String charset = "UTF-8";
        File uploadFile = new File("bugreport.zip");
        String requestURL = "http://www.haus-bus.de/loxone/tickets/createTicket.php";

        try
        {
            MultipartUtility multipart = new MultipartUtility(requestURL, charset);

            multipart.addHeaderField("User-Agent", "CodeJava");
            // multipart.addHeaderField("Test-Header", "Header-Value");

            multipart.addFormField("email", email);
            multipart.addFormField("message", message);
            multipart.addFormField("bugReportError", bugReportError);

            multipart.addFilePart("fileUpload", uploadFile);

            List<String> response = multipart.finish();

            if (response.size() == 0)
            {
                logger.finer("keine Antwort vom Server");
                return "Fehler: Server hat keine Antwort gesendet";
            } else
            {
                String ticketId = response.get(0);
                logger.finer("ticketId = " + ticketId);
                return ticketId;
            }
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "", e);
            return "Fehler: Internetverbindung vorhanden?\n" + e.toString();
        }
    }

    private void prepareBugreport() throws IOException
    {
        File bugreport = new File("bugreport.zip");
        if (bugreport.exists())
            bugreport.delete();

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(bugreport));
        try
        {
            addFile("logs/hausbusdeError_0_0.log", out);

            int nrStarts = 0;
            for (int i = 0; i < 10; i++)
            {
                String filename = "logs/hausbusdeTrace_0_" + i + ".log";
                File file = new File(filename);
                if (!file.exists())
                    break;

                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file))))
                {
                    String line = br.readLine();
                    if (line.contains("HomematicBridge version"))
                        nrStarts++;
                } catch (IOException e)
                {
                    throw new IOException("could not load" + file);
                }

                addFile(filename, out);
                if (nrStarts == 2)
                    break;
            }
        } finally
        {
            out.close();
        }
    }

    private void addFile(String filename, ZipOutputStream out) throws IOException
    {
        FileInputStream in = new FileInputStream(filename);

        out.putNextEntry(new ZipEntry(filename));

        byte[] b = new byte[1048576];
        int count;
        while ((count = in.read(b)) > 0)
        {
            out.write(b, 0, count);
        }
        in.close();
    }

    private String removeForward(String content)
    {
        return content.replace("forwardMe();", "");
    }

    private Response handleRestartCCU(Map<String, String> params)
    {
        new Thread(() -> {
            HausBusUtils.sleep(1000);
            exec("reboot 0", false);
        }).start();

        String content = loadPage("restartCCU.html");
        content = removeForward(content);
        content = content.replace("%HOST%", host);

        return Response.newFixedLengthResponse(content);
    }

    private Response handleRestart(Map<String, String> params)
    {
        new Thread(() -> {
            HausBusUtils.sleep(1000);
            exec("/etc/init.d/S60HausBusDeInterface restart", false);
        }).start();

        String content = loadPage("restart.html");
        content = content.replace("%HOST%", host);
        return Response.newFixedLengthResponse(content);
    }

    private String getImageRoot()
    {
        return "http://" + host + "/addons/hausbusdeip/";
    }

    private String loadPage(String page)
    {
        logger.finer("loadPage " + page);
        InputStream in = null;
        try
        {
            in = new FileInputStream("webpages/" + page);
        } catch (FileNotFoundException e)
        {
        }

        if (in == null)
        {
            logger.warning("not found " + page);
            return "";
        }

        try
        {
            String result = readFromInputStream(in);
            result = result.replaceAll("%IMG_ROOT%", getImageRoot());
            logger.finer("root: " + getImageRoot());
            return result;
        } catch (IOException e)
        {
            logger.log(Level.SEVERE, "", e);
            return "";
        }
    }

    private String readFromInputStream(InputStream inputStream) throws IOException
    {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8")))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
