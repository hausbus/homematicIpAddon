package de.hausbus.homematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.nanohttpd.util.ServerRunner;

import de.hausbus.com.BusDevice;
import de.hausbus.com.BusException;
import de.hausbus.com.BusHandler;
import de.hausbus.com.BusMessage;
import de.hausbus.com.FirmwareUpdater;
import de.hausbus.com.HausBusUtils;
import de.hausbus.com.HomeServer;
import de.hausbus.com.HomeserverException;
import de.hausbus.com.IBusDevice;
import de.hausbus.com.IBusDeviceListener;
import de.hausbus.com.IBusMessageListener;
import de.hausbus.com.IHomeServer;
import de.hausbus.com.Templates;
import de.hausbus.proxy.controller.data.Configuration;
import de.hausbus.proxy.controller.data.EvStarted;
import de.hausbus.proxy.controller.data.ModuleId;

public class HomematicBridge implements IBusDeviceListener, IBusMessageListener
{
    public static String version = "10.0.166";
    private static final String persistenceFile = "bridge.bin";

    private static final Level hausBusConsoleLevel = Level.parse(System.getProperty("hausBusConsoleLevel", "ALL"));
    private static final int hausBusRpcPort = Integer.parseInt(System.getProperty("hausBusRpcPort", "8766"));
    private static final boolean hideOwnDevices = Boolean.parseBoolean(System.getProperty("hideOwnDevices", "true"));
    private static final boolean localMode = Boolean.parseBoolean(System.getProperty("localMode", "false"));

    public static volatile boolean ccuConnected = false;
    public Level logLevel = Level.ALL;
    public boolean autoDeleteUnreachableDevices = false;
    public FileHandler fileHandler;
    private final Logger logger = Logger.getLogger(getClass().getName());
    public static HomematicBridge instance;
    public List<CcuInterface> ccuInterfaces = new ArrayList<>();
    final IHomeServer homeserver;
    private final FirmwareUpdater firmwareUpdater;
    final Map<String, String> onlineFirmwareVersions = new HashMap<>();

    final HashMap<String, HomematicDevice> devices = new HashMap<>();
    final HashMap<Integer, Long> lastPing = new HashMap<>();
    private String lastOurCcuIdentifier = "";
    public static String bridgeIp = "";

    public static void main(String[] args) throws IOException
    {
        Templates.templateRootDir = "HomematicTemplates/";
        if (System.getProperty("templateRootDir") != null)
            Templates.templateRootDir = System.getProperty("templateRootDir") + "/";
        instance = new HomematicBridge();
    }

    private HomematicBridge() throws IOException
    {
        load();
        configureLogging();
        configureBridgeIp(bridgeIp);
        logger.fine("HomematicBridge version " + version + " starting ");

        HomeServer.hideOwnDevices = hideOwnDevices;
        BusHandler.UDP_PORT = 5855;
        homeserver = HomeServer.getInstance();
        homeserver.addBusMessageListener(this);

        firmwareUpdater = new FirmwareUpdater();
        onlineFirmwareVersions.putAll(firmwareUpdater.init());

        startBridge();
        checkReconnectCcu();
        startWebserver();

        logger.finer("Homematic Bridge Started. CCU connected = " + ccuConnected);
    }

    private void checkReconnectCcu()
    {
        if (!ccuConnected)
        {
            if (lastOurCcuIdentifier.isEmpty())
                logger.finer("ccu not connected yet and interface id unknown yet");
            /*
             * else if (devices.isEmpty()) logger.finer("no devices to report yet");
             */
            else
            {
                for (int i = 0; i < 5; i++)
                {
                    logger.finer("ccu not connected yet. trying to contact with last interfaceId " + lastOurCcuIdentifier + ". Attempt " + (i + 1) + "/5");
                    RpcServer.forceOnce = true;
                    Object result = ccuNewDevices(devices.values());
                    if (result != null)
                    {
                        logger.finer("ccu reconnected!");
                        ccuConnected = true;
                        break;
                    } else if (i < 4)
                        HausBusUtils.sleep(3000);
                }
            }

            if (!ccuConnected)
                logger.finer("ccu connect failed. Waiting for CCU to connect us");
        }
    }

    Object ccuNewDevices(Collection<HomematicDevice> newDevices)
    {
        logger.finer("reporting new devices to " + ccuInterfaces.size() + " ccuInterfaces -> " + newDevices.size() + ": " + newDevices);
        Object result = null;
        for (CcuInterface act : ccuInterfaces)
        {
            result = act.newDevices(newDevices);
        }

        return result;
    }

    void ccuReportNewDevice(HomematicDevice newDevice)
    {
        logger.finer("reporting new device to " + ccuInterfaces.size() + " ccuInterfaces");
        for (CcuInterface act : ccuInterfaces)
        {
            act.newDevices(newDevice);
        }
    }

    void ccuEvent(String address, String value_key, Object value)
    {
        logger.finer("reporting event to " + ccuInterfaces.size() + " ccuInterfaces: " + address + ", " + value_key + " = " + value);
        for (CcuInterface act : ccuInterfaces)
        {
            act.event(address, value_key, value);
        }
    }

    void ccuDeleteDevice(String address)
    {
        logger.finer("ccuDeleteDevice to " + ccuInterfaces.size() + " " + address);
        for (CcuInterface act : ccuInterfaces)
        {
            act.deleteDevices(address);
        }
    }

    void ccuInterfaceConnected(String url, String interface_id)
    {
        if (url.contains("xmlrpc_bin"))
        {
            ccuConnected = true;
            lastOurCcuIdentifier = interface_id;
            logger.info("our interface id = " + lastOurCcuIdentifier);

            boolean found = false;
            for (CcuInterface act : ccuInterfaces)
            {
                if (act.url.contains(":1999"))
                {
                    act.ourInterfaceId = lastOurCcuIdentifier;
                    found = true;
                    break;
                }
            }
            if (!found)
                logger.severe("ccu interface not found!");
        } else if (url.startsWith("http") && !url.contains("/bidcos"))
        {
            boolean found = false;
            for (CcuInterface act : ccuInterfaces)
            {
                if (act.url.equals(url))
                {
                    logger.finer("interface " + url + " already exists");
                    act.ourInterfaceId = interface_id;
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                logger.finer("creating new interface for " + url + ", " + interface_id);
                ccuInterfaces.add(new CcuInterface(url, interface_id));
            }
        }

        if (!devices.isEmpty())
            ccuNewDevices(devices.values());

        save();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("HomematicBridge [logLevel=");
        builder.append(logLevel);
        builder.append(", autoDeleteUnreachableDevices=");
        builder.append(autoDeleteUnreachableDevices);
        builder.append(", lastOurCcuIdentifier=");
        builder.append(lastOurCcuIdentifier);
        builder.append(", bridgeIp=");
        builder.append(bridgeIp);
        builder.append("]");
        return builder.toString();
    }

    private void startWebserver()
    {
        logger.fine("starting webserver at port " + Webserver.PORT);
        ServerRunner.run(Webserver.class);
        logger.fine("webserver started");
    }

    private void startBridge() throws IOException
    {
        if (localMode)
            ccuInterfaces.add(new CcuInterface("http://192.168.178.49:1999", lastOurCcuIdentifier));
        else
            ccuInterfaces.add(new CcuInterface("http://127.0.0.1:1999", lastOurCcuIdentifier));
        RpcServer.bridge = this;
        startRpcServer();
        // RpcServer.ourInterfaceId = "1197";
        startHomeServer();
        Logger.getLogger("hausbus").setLevel(Level.OFF);
        Logger.getLogger("de.hausbus.homematic").setLevel(Level.ALL);
        logger.fine("Haus-Bus Homematic Bridge started");
    }

    private void startHomeServer()
    {
        try
        {
            logger.fine("starting homeserver at port " + BusHandler.UDP_PORT);
            homeserver.addBusDeviceListener(this);
            homeserver.searchDevices(true);

            Map<Integer, BusDevice> theDevices = homeserver.getDevices();
            logger.fine("creating homematic devices: " + theDevices);
            for (IBusDevice act : theDevices.values())
            {
                checkCreateDevice(act);
            }
        } catch (BusException | HomeserverException e)
        {
            logger.log(Level.WARNING, "search devices failed", e);
        }
        logger.finer("returns");
    }

    private void startRpcServer() throws IOException
    {
        logger.fine("starting RPC server at port " + hausBusRpcPort);
        WebServer webServer = new WebServer(hausBusRpcPort);
        webServer.start();

        XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

        PropertyHandlerMapping phm = new MyHandlerMapping();

        try
        {
            // add handler class
            phm.addHandler("methods", RpcServer.class);
            phm.addHandler("system", RpcServer.class);

            xmlRpcServer.setHandlerMapping(phm);

            // server config
            XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
            serverConfig.setEnabledForExtensions(true);
            serverConfig.setContentLengthOptional(false);
        } catch (XmlRpcException e)
        {
            logger.log(Level.SEVERE, "", e);
            throw new IOException("Init of RPC server failed", e);
        }

        logger.fine("returns");
    }

    private void configureLogging() throws IOException
    {
        HomeServer.CONSOLE_LEVEL = hausBusConsoleLevel;
        HomeServer.SKIP_FILE_HANDLERS = true;

        Formatter formatter = new HausBusLogFormatter();

        Logger.getLogger("hausbus").setLevel(Level.ALL);
        Logger.getLogger("de.hausbus.homematic").setLevel(Level.ALL);
        // Logger.getLogger("org.apache.xmlrpc").setLevel(Level.ALL);

        String logdir = "logs/";
        new File(logdir).mkdir();

        fileHandler = new FileHandler(logdir + "hausbusdeTrace_%u_%g.log", 15000000, 2, false);
        fileHandler.setLevel(logLevel);
        fileHandler.setFormatter(formatter);
        Logger.getLogger("").addHandler(fileHandler);

        FileHandler errorHandler = new FileHandler(logdir + "hausbusdeError_%u_%g.log", 15000000, 2, false);
        errorHandler.setLevel(Level.WARNING);
        errorHandler.setFormatter(formatter);
        Logger.getLogger("").addHandler(errorHandler);
    }

    @Override
    public void devicesSearchStarted()
    {
    }

    @Override
    public void devicesSearchFinished()
    {
    }

    @Override
    public void deviceSearched(IBusDevice foundDevice)
    {
        checkCreateDevice(foundDevice);
    }

    @Override
    public void newDeviceDetected(IBusDevice newDevice)
    {
        checkCreateDevice(newDevice);
    }

    @Override
    public void knownDeviceRestarted(IBusDevice newDevice)
    {
        checkCreateDevice(newDevice);
    }

    @Override
    public void deviceGoneOffline(IBusDevice offlineDevice)
    {
        logger.finer("calling manually offline");
        ccuEvent(AHomematicDevice.getHomematicAddress(offlineDevice.getDeviceId(), 0), "UNREACH", true);
    }

    public HomematicDevice getHomematicDevice(int deviceId)
    {
        for (HomematicDevice act : devices.values())
        {
            if (act.device.getDeviceId() == deviceId)
                return act;
        }

        return null;
    }

    public HomematicDevice getHomematicDevice(String addressIn)
    {
        String address = addressIn;
        int pos = address.indexOf(":");
        if (pos != -1)
            address = address.substring(0, pos);

        HomematicDevice result = devices.get(address);
        if (result == null)
        {
            int deviceId = HomematicDevice.getHausBusAddress(Integer.parseInt(address));
            Long myLastPing = lastPing.get(deviceId);

            if (myLastPing != null && HausBusUtils.getClockIndependMillis() - myLastPing < 30000)
                logger.warning("Device address not found " + deviceId + " but last ping already done before " + (HausBusUtils.getClockIndependMillis() - myLastPing));
            else
            {
                logger.warning("Device address not found. Trying to ping " + deviceId);
                IBusDevice device = new BusDevice(deviceId);

                lastPing.put(deviceId, HausBusUtils.getClockIndependMillis());

                try
                {
                    BusHandler.shortTimeout.set(50);
                    device.ping();

                    for (int i = 0; i < 8; i++)
                    {
                        result = devices.get(address);
                        if (result != null)
                        {
                            logger.info("refound device " + deviceId);
                            break;
                        }
                        HausBusUtils.sleep(1000);
                    }
                } catch (Exception e)
                {
                } finally
                {
                    BusHandler.shortTimeout.set(null);
                }
            }

            // ccu.event(address + ":0", "UNREACH", true);
            // logger.warning("autodeleting unreachable device " + addressIn + " / " + address);
            // deleteDevice(address);
        }

        return result;
    }

    public void checkCreateDevice(IBusDevice device)
    {
        if (device.getName().contains("LAN-RS485"))
        {
            logger.finer("filtering bridge " + device.getDeviceId());
            return;
        }

        synchronized (devices)
        {
            HomematicDevice existingDevice = getHomematicDevice(device.getDeviceId());
            if (existingDevice != null && existingDevice.restarted)
            {
                logger.finer("recreating device after restart: " + existingDevice.address);
                devices.remove(existingDevice.address);
                existingDevice = null;
            }

            if (existingDevice == null)
            {
                if (!checkDeviceValid(device))
                    return;

                logger.finer("creating device " + device.getDeviceId());
                HomematicDevice newDevice = new HomematicDevice(device, this);
                devices.put(newDevice.getAddress(), newDevice);

                ccuReportNewDevice(newDevice);
            } else
                logger.finer("Device " + device.getDeviceId() + " already existing");
        }
    }

    private boolean checkDeviceValid(IBusDevice device)
    {
        Configuration config = ((BusDevice) device).getConfigurationFromCacheOrCall();
        ModuleId moduleId = ((BusDevice) device).getModuleIdFromCacheOrCall();
        if (Templates.getInstance().getFeatures(moduleId.getFirmwareId(), config.getFCKE()) == null)
        {
            logger.warning("Could not find features for device with Id: " + device.getDeviceId() + ", FirmwareId: " + moduleId.getFirmwareId() + ", FCKE: " + config.getFCKE());
            return false;
        }
        return true;
    }

    public static void sleep(int sleep)
    {
        try
        {
            Thread.sleep(sleep);
        } catch (InterruptedException e)
        {
        }
    }

    public void deleteDevice(String address)
    {
        IBusDevice hausBusDevice = null;

        HomematicDevice device = devices.get(address);
        if (device != null)
        {
            if (device.device != null)
            {
                logger.info("found hausbus device " + device.deviceId);
                hausBusDevice = device.device;
            } else
                logger.warning("hausbus device of homematic device " + address + " null");
        }

        if (hausBusDevice == null)
        {
            logger.warning("homematic device " + address + " unknown. Trying to read device id manually");
            try
            {
                int deviceId = HomematicDevice.getHausBusAddress(Integer.parseInt(address));
                hausBusDevice = homeserver.getDevice(deviceId);
                logger.finer("got device " + hausBusDevice);
            } catch (NumberFormatException e)
            {
                logger.log(Level.WARNING, "", e);
            }
        }

        if (hausBusDevice != null)
        {
            logger.finer("deleting hausBus device " + hausBusDevice.getDeviceId());
            homeserver.deleteDevice(hausBusDevice);
        } else
            logger.warning("no hausbus device found for " + address);

        devices.remove(address);
        ccuDeleteDevice(address);
    }

    private class MyHandlerMapping extends PropertyHandlerMapping
    {
        public XmlRpcHandler getHandler(String pHandlerName) throws XmlRpcException
        {
            XmlRpcHandler result = null;

            try
            {
                result = super.getHandler(pHandlerName);
            } catch (Exception ex)
            {
                // ignore
            }

            if (result == null)
                result = super.getHandler("system." + pHandlerName);

            if (result == null)
                logger.severe("no handler found: " + pHandlerName);

            return result;
        }
    }

    @Override
    public void busMessageReceived(BusMessage message)
    {
        IBusDevice senderDevice = null;
        int senderDeviceId = 0;

        if (message.getSender() != null)
        {
            senderDevice = message.getSender().getDevice();
            senderDeviceId = senderDevice.getDeviceId();
        }

        HomematicDevice homematicSenderDevice = null;
        for (HomematicDevice act : devices.values())
        {
            if (act.deviceId == senderDeviceId)
                homematicSenderDevice = act;

            act.busMessageReceived(message);
        }

        if (homematicSenderDevice == null && senderDevice != null && ((BusDevice) senderDevice).featuresCreated)
            checkCreateDevice(senderDevice);
        else if (homematicSenderDevice != null && message.getData() instanceof EvStarted)
        {
            logger.finer("marking device as restarted: " + homematicSenderDevice.address);
            homematicSenderDevice.restarted = true;
        }

        if (message.getData() instanceof EvStarted)
        {
            logger.finer("researching devices after evStarted");
            homeserver.searchDevicesAsync(false);
        }
    }

    public boolean isAnyDeviceUpdateable()
    {
        for (HomematicDevice act : devices.values())
        {
            if (act.isUpdateable())
                return true;
        }
        return false;
    }

    private void load()
    {
        File file = new File(persistenceFile);
        if (file.exists())
        {
            try (ObjectInputStream oin = new ObjectInputStream(new FileInputStream(file)))
            {
                logLevel = Level.parse((String) oin.readObject());
                autoDeleteUnreachableDevices = oin.readBoolean();
                lastOurCcuIdentifier = (String) oin.readObject();
                bridgeIp = (String) oin.readObject();
                if (bridgeIp != null && bridgeIp.startsWith("http"))
                {
                    logger.finer("clearing bridgeIp");
                    bridgeIp = "";
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            logger.finer("loaded: " + toString());
        }
    }

    public void save()
    {
        File file = new File(persistenceFile);
        try (ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(file)))
        {
            oout.writeObject(logLevel.getName());
            oout.writeBoolean(autoDeleteUnreachableDevices);
            oout.writeObject(lastOurCcuIdentifier);
            oout.writeObject(bridgeIp);
            logger.finer("saved: " + toString());
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "", e);
        }
    }

    public void setLogLevel(String logLevel)
    {
        logger.fine("setLogLevel " + logLevel);
        try
        {
            this.logLevel = Level.parse(logLevel);
            fileHandler.setLevel(this.logLevel);
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "", e);
        }
    }

    public DummyHomematicDevice getDummyDevice(String address)
    {
        return new DummyHomematicDevice(address, this);
    }

    public void configureBridgeIp(String bridgeIp)
    {
        logger.finer("BridgeIp: " + bridgeIp);
        HomematicBridge.bridgeIp = bridgeIp;
        save();

        if (bridgeIp.isEmpty())
            BusHandler.forcedBridgeIp = null;
        else
        {
            try
            {
                BusHandler.forcedBridgeIp = InetAddress.getByName(bridgeIp);
            } catch (UnknownHostException e)
            {
                logger.log(Level.SEVERE, "", e);
            }
        }
    }
}
