package de.hausbus.homematic;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;

/**
 * Fehlercode Bedeutung -1 Allgemeiner Fehler -2 Unbekanntes Ger�t / unbekannter Kanal -3 Unbekannter Paramset -4 Es wurde eine Ger�teadresse erwartet -5 Unbekannter Parameter oder Wert -6 Operation wird vom Parameter nicht unterst�tzt -7 Das Interface ist nicht in der Lage ein Update durchzuf�hren -8 Es seht nicht gen�gend DutyCycle zur Verf�gung -9 Das Ger�t ist nicht in Reichweite
 */
public class CcuInterface
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final int maxAsyncThreads = 10;
    private volatile int actAsyncTreads = 0;
    final String url;
    volatile String ourInterfaceId;

    XmlRpcClient client;

    public static void main(String[] args)
    {
        // CcuInterface ccu = new CcuInterface("http://192.168.178.49:2000");
        // RpcServer.ccuConnected = true;
        // HBW0002027
        // Object result = ccu.callGenericMethod("getParamset","HBW0002027:21","HBW0002067:1"); //, "HBW0002027:1", "HBW0002067:1"); //, new String[] {"1620002027"});
        // Object result = ccu.callGenericMethod("getParamset", "HBW0002094:1", "HBW0002094:1");
        // System.out.println(result);

        // getParamsetDescription(String address, String paramset_type) -> link
        // getParamset(String address, String paramset_key) -> addresse des partners

        // getLinks
        // DESCRIPTION=Standardverkn�pfung Taster - Schaltaktor
        // SENDER=HBW0002027:1
        // FLAGS=0
        // RECEIVER=HBW0002067:1
        // NAME=HBW-SD6-Multikey-ESP HBW0002027:1 mit HBW-LC-IO32-DR-ESP HBW0002067:1

        // getLinkInfo
        // DESCRIPTION=Standardverkn�pfung Taster - Schaltaktor
        // NAME=HBW-SD6-Multikey-ESP HBW0002027:1 mit HBW-LC-IO32-DR-ESP HBW0002067:1

        // getLinkPeers(String)
        // HBW0002067:1
    }

    public CcuInterface(String url, String ourInterfaceId)
    {
        this.url = url;
        this.ourInterfaceId = ourInterfaceId;

        XmlRpcClientConfigImpl xmlRpcClientConfig = new XmlRpcClientConfigImpl();
        try
        {
            xmlRpcClientConfig.setServerURL(new URL(url));
        } catch (MalformedURLException e)
        {
            logger.log(Level.SEVERE, "", e);
        }

        final XmlRpcTransportFactory transportFactory = new XmlRpcTransportFactory()
        {
            @Override
            public XmlRpcTransport getTransport()
            {
                return new RpcLoggingTransport(client);
            }
        };

        client = new XmlRpcClient();
        client.setTransportFactory(transportFactory);
        client.setConfig(xmlRpcClientConfig);

        logger.finer("created interface for url " + url + " and our identifier " + ourInterfaceId);
    }

    public Object callGenericMethod(String method, Object... params)
    {
        Object result = null;

        if (HomematicBridge.ccuConnected || RpcServer.forceOnce)
        {
            RpcServer.forceOnce = false;

            logger.finer("calling RPC method " + method + ", params: " + RpcServer.format(params));
            try
            {
                result = client.execute(method, params);
            } catch (Exception e)
            {
                if (method.equals("newDevices"))
                    logger.log(Level.FINER, "rpc call " + method + " failed", e);
                else
                    logger.log(Level.SEVERE, "rpc call " + method + " failed", e);
            }
        } else
            logger.finer("ccu not connected yet");

        return result;
    }

    public void callGenericMethodAsync(String method, Object... params)
    {
        if (actAsyncTreads < maxAsyncThreads)
        {
            actAsyncTreads++;
            logger.finer("calling async " + method + "params = " + RpcServer.format(params) + ", actAsyncTreads = " + actAsyncTreads);

            new Thread(() -> {
                try
                {
                    callGenericMethod(method, params);
                } finally
                {
                    actAsyncTreads--;
                }
            }).start();
        } else
        {
            logger.fine("no async threads available!");
            callGenericMethod(method, params);
        }
    }

    /**
     * Mit dieser Methode teilt der Schnittstellenprozess der Logikschicht mit, dass sich ein Wert ge�ndert hat oder ein Event (z.B. Tastendruck) empfangen wurde. Der Parameter interface_id gibt die id des Schnittstellenprozesses an, der das Event sendet. Der Parameter address ist die Addresse des logischen Ger�tes, zu dem der ge�nderte Wert / das Event geh�rt. Der Parameter value_key ist der Name des entsprechenden Wertes. Die m�glichen Werte f�r value_key ergeben sich aus der ParamsetDescription des entsprechenden Parameter-Sets �VALUES�. Der Parameter value gibt den neuen Wert bzw. den dem Event zugeordneten Wert an. Der Datentyp von value ergibt sich aus der ParamsetDescription des Values-Parameter-Sets des entsprechenden logischen Ger�tes.
     */
    public void event(String address, String value_key, Object value)
    {
        callGenericMethodAsync("event", ourInterfaceId, address, value_key, value);
    }

    /**
     * Diese Methode gibt alle der Logikschicht bekannten Ger�te f�r den Schnittstellenprozess mit der Id interface_id in Form von Ger�tebeschreibungen zur�ck. Damit kann der Schnittstellenprozess durch Aufruf von newDevices() und deleteDevices() einen Abgleich vornehmen. Damit das funktioniert, muss sich die Logikschicht diese Informationen zumindest teilweise merken. Es ist dabei ausreichend, wenn jeweils die Member ADDRESS und VERSION einer DeviceDescription gesetzt sind.
     */
    Object listDevices(String interface_id)
    {
        Object result = callGenericMethod("listDevices", interface_id);
        return result;
    }

    /**
     * Mit dieser Methode wird der Logikschicht mitgeteilt, dass neue Ger�te gefunden wurden. Der Parameter interface_id gibt die id des Schnittstellenprozesses an, zu dem das Ger�t geh�rt. Der Parameter dev_descriptions ist ein Array, das die Beschreibungen der neuen Ger�te enth�lt. Wenn dev_descriptions Ger�te enth�lt, die der Logikschicht bereits bekannt sind, dann ist davon auszugehen, dass sich z.B. durch ein Firmwareupdate das Verhalten des Ger�tes ge�ndert hat. Die Basisplatform mu� dann einen Abgleich mit der neuen Beschreibung durchf�hren. Dabei sollte die Konfiguration des Ger�tes innerhalb der Logikschicht so weit wie m�glich erhalten bleiben.
     */
    void newDevices(HomematicDevice newDevice)
    {
        Vector devVector = new Vector();
        List<Map<String, Object>> deviceDescriptions = newDevice.getAllDeviceDescriptons();

        for (Map<String, Object> act : deviceDescriptions)
        {
            devVector.add(act);
        }

        callGenericMethod("newDevices", ourInterfaceId, devVector);
    }

    Object newDevices(Collection<HomematicDevice> newDevices)
    {
        Vector devVector = new Vector();

        for (HomematicDevice newDevice : newDevices)
        {
            List<Map<String, Object>> deviceDescriptions = newDevice.getAllDeviceDescriptons();

            for (Map<String, Object> act : deviceDescriptions)
            {
                devVector.add(act);
            }
        }

        return callGenericMethod("newDevices", ourInterfaceId, devVector);
    }

    /**
     * Mit dieser Methode wird der Logikschicht mitgeteilt, dass Ger�te im Schnittstellenprozess gel�scht wurden. Der Parameter interface_id gibt die id des Schnittstellenprozesses an, zu dem das Ger�t geh�rt. Der Parameter addresses ist ein Array, das die Adressen der gel�schten Ger�te enth�lt
     */
    void deleteDevices(String address)
    {
        callGenericMethodAsync("deleteDevices", ourInterfaceId, new String[] { address });
    }

    /**
     * Mit dieser Methode wird der Logikschicht mitgeteilt, dass sich an einem Ger�t etwas ge�ndert hat. Der Parameter interface_id gibt die id des Schnittstellenprozesses an, zu dem das Ger�t geh�rt. Der Parameter address ist die Adresse des Ger�tes oder des Kanals, auf das sich die Meldung bezieht. Der Parameter hint spezifiziert die �nderung genauer: UPDATE_HINT_ALL=0 Es hat eine nicht weiter spezifizierte �nderung stattgefunden und es sollen daher alle m�glichen �nderungen ber�cksichtigt werden. UPDATE_HINT_LINKS=1 Es hat sich die Anzahl der Verkn�pfungspartner ge�ndert. Derzeit werden nur �nderungen an den Verkn�pfungspartnern auf diesem Weg mitgeteilt.
     */
    void updateDevice(String interface_id, String address, int hint)
    {

    }

    /**
     * Mit dieser Methode wird der Logikschicht mitgeteilt, dass ein Ger�t getuscht wurde. Der Parameter interface_id gibt die id des Schnittstellenprozesses an, zu dem das Ger�t geh�rt. Der Parameter oldDeviceAddress ist die Adresse des ersetzten Ger�tes. Der Parameter newDeviceAddress ist die Adresse des Ger�tes welches an Stelle des alten Ger�tes im System eingef�gt wurde.
     */
    void replaceDevice(String interface_id, String oldDeviceAddress, String newDeviceAddress)
    {

    }

    /**
     * Diese Methode wird aufgerufen, wenn ein bereits angelerntes Ger�t in den Anlernmodus versetzt wird, w�hrend der Installations-Modus aktiviert ist (vgl. setInstallMode). Der Parameter interface_id gibt die id des Schnittstellenprozesses an, zu dem das Ger�t geh�rt. Der Parameter addresses ist ein Array, das die Adressen der gel�schten logischen Ger�te enth�lt.
     */
    void readdedDevice(String interfaceId, List<String> addresses)
    {

    }

}
