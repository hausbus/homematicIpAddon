package de.hausbus.homematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RpcServer
{
    private static final Logger logger = Logger.getLogger(RpcServer.class.getName());
    static volatile HomematicBridge bridge;
    public static volatile boolean forceOnce = false;

    /**
     * Diese Methode erstellt eine Kommunikationsbeziehung zwischen zwei logischen Ger�ten. Die Parameter sender und receiver bezeichnen die beiden zu verkn�pfenden Partner. Die Parameter name und description sind optional und beschreiben die Verkn�pfung n�her.
     */
    public Object addLink(String sender, String receiver, String name, String description)
    {
        logger.warning("addLink(sender=" + sender + ", receiver=" + receiver + ", name=" + name + ", description = " + description);
        return new Object[0];
    }

    /**
     * Diese Methode l�scht alle zu einem Ger�t in der CCU gespeicherten Konfigurationsdaten. Diese werden nicht sofort wieder vom Ger�t abgefragt, sondern wenn sie das n�chste mal ben�tigt werden.
     */
    public Object clearConfigCache(String address)
    {
        logger.warning("clearConfigCache(address=" + address + ")");
        return new Object[0];
    }

    /**
     * Diese Methode l�scht ein Ger�t aus dem Schnittstellenprozess. Der Parameter address ist die Addresse des zu l�schenden Ger�tes. flags ist ein bitweises oder folgender Werte: 0x01=DELETE_FLAG_RESET Das Ger�t wird vor dem L�schen in den Werkszustand zur�ckgesetzt 0x02=DELETE_FLAG_FORCE Das Ger�t wird auch gel�scht, wenn es nicht erreichbar ist 0x04=DELETE_FLAG_DEFER Wenn das Ger�t nicht erreichbar ist, wird es bei n�chster Gelegenheit gel�scht
     */
    public Object deleteDevice(String address, int flags)
    {
        logger.fine("deleteDevice(address=" + address + ", flags=" + flags + ")");
        bridge.deleteDevice(address);
        return new Object[] {};
    }

    /**
     * Diese Methode gibt die Ger�tebeschreibung des als address �bergebenen Ger�tes zur�ck.
     */
    public Object getDeviceDescription(String address)
    {
        logger.finer("getDeviceDescription(address=" + address + ")");

        Map<String, Object> result = new LinkedHashMap<>();

        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            result = device.getDeviceDescription(address);
        else if (!address.isEmpty())
        {
            logger.warning("device " + address + " not found");
            /*
             * if (!address.isEmpty()) { String basis = address; if (basis.contains(":")) basis = basis.substring(0, basis.indexOf(":")); bridge.deleteDevice(basis); }
             */
            // return bridge.getDummyDevice(address).getDeviceDescription(address);
        }

        logger.finer("returns " + format(result));
        return result;
    }

    public static String format(Object object)
    {
        if (object == null)
            return "NULL";

        if (object instanceof Object[])
        {
            StringBuilder builder = new StringBuilder();

            builder.append("Array[").append(((Object[]) object).length).append("] ");
            for (Object act : (Object[]) object)
            {
                builder.append(act).append(", ");
            }
            return builder.toString();
        }

        return object.toString();
    }

    /**
     * Diese Methode gibt den Namen und die Beschreibung f�r eine bestehende Kommunikationsbeziehung zur�ck. Die Parameter sender_address und receiver_address bezeichnen die beiden verkn�pften Partner
     */
    public Object getLinkInfo(String sender_address, String receiver_address)
    {
        logger.warning("getLinkInfo(sender_address=" + sender_address + ", receiver_address=" + receiver_address + ")");
        Object result = new Object[0];

        /*
         * HomematicDevice device = bridge.getHomematicDevice(sender_address); if (device != null) result = device.getDeviceDescription(address); else if (!address.isEmpty()) { logger.warning("device " + address + " not found"); if (!address.isEmpty()) { String basis = address; if (basis.contains(":")) basis = basis.substring(0, basis.indexOf(":")); bridge.deleteDevice(basis); } // return bridge.getDummyDevice(address).getDeviceDescription(address); }
         */

        logger.finer("returns " + format(result));
        return result;
    }

    /**
     * Diese Methode gibt alle einem logischen Ger�t zugeordneten Kommunikationspartner zur�ck. Die zur�ckgegebenen Werte k�nnen als Parameter paramset_key f�r getParamset() und putParamset() verwendet werden. Der Parameter address ist die Adresse eines logischen Ger�tes.
     */
    public Object getLinkPeers(String address)
    {
        logger.warning("getLinkPeers(address=" + address + ")");
        return new Object[0];
    }

    /**
     * Diese Methode gibt alle einem logischen Kanal oder Ger�t zugeordneten Kommunikationsbeziehungen zur�ck. Der Parameter address ist die Kanal- oder Ger�teadresse des logischen Objektes, auf das sich die Abfrage bezieht. Bei address=="" werden alle Kommunikationsbeziehungen des gesamten Schnittstellenprozesses zur�ckgegeben. Der Parameter flags ist ein bitweises oder folgender Werte: 1 = GL_FLAG_GROUP Wenn address einen Kanal bezeichnet, der sich in einer Gruppe befindet, werden die Kommunikationsbeziehungen f�r alle Kan�le der Gruppe zur�ckgegeben. 2 = GL_FLAG_SENDER_PARAMSET Das Feld SENDER_PARAMSET des R�ckgabewertes wird gef�llt. 4 = GL_FLAG_RECEIVER_PARAMSET Das Feld RECEIVER_PARAMSET des R�ckgabewertes wird gef�llt. flags ist optional. Defaultwert ist 0x00.
     * 
     * Der R�ckgabewert ist ein Array von Strukturen. Jede dieser Strukturen enth�lt die folgenden Felder: SENDER Datentyp String. Adresse des Senders der Kommunikationsbeziehung RECEIVER Datentyp String. Adresse des Empf�ngers der Kommunikationsbeziehung FLAGS Datentyp Integer. FLAGS ist ein bitweises oder folgender Werte: 1=LINK_FLAG_SENDER_BROKEN Diese Verkn�pfung ist auf der Senderseite nicht intakt 2=LINK_FLAG_RECEIVER_BROKEN Diese Verkn�pfung ist auf der Empf�ngerseite nicht intakt NAME Datentyp String. Name der Kommunikationsbeziehung DESCRIPTION Datentyp String. Textuelle Beschreibung der Kommunikationsbeziehung SENDER_PARAMSET Datentyp Paramset. Parametersatz dieser Kommunikationsbeziehung f�r die Senderseite RECEIVER_PARAMSET Datentyp Paramset. Parametersatz dieser
     * Kommunikationsbeziehung f�r die Empf�ngerseite
     */
    public Object getLinks(String address, Integer flags)
    {
        logger.finer("getLinks(address=" + address + ", flags = " + flags + ")");
        Object result = new Object[0];
        logger.finer("returns " + format(result));
        return result;
    }

    /**
     * Mit dieser Methode wird ein komplettes Parameter-Set f�r ein logisches Ger�t gelesen. Der Parameter address ist die Addresses eines logischen Ger�tes. Der Parameter paramset_key ist �MASTER�, �VALUES� oder die Adresse eines Kommunikationspartners f�r das entsprechende Link-Parameter-Set (siehe getLinkPeers). Dem Parameter mode k�nnen folgende Werte �bergeben werden: 0 default: Keien Auswirkung, die Funktion verh�lt sicht wie der Aufruf ohne mode 1 UndefinedValues: Jeder Eintrag inerhalb des zur�ckgelieferten Paramset ins eine Struktur mit folgendem Aufbau: �UNDEFINED�(Boolean) Flag ob der angeforderte Wert initial gesetzt wurde und somit wahrscheinlich nicht der Realit�t entspricht oder ob der Wert von einem Ger�t empfangen wurde, true = Wert wurde initial gesetzt und noch nicht
     * ver�ndert, false = der Wert wurde neu gesetzt �VALUE�(ValueType) Wert des angeforderten Parameter. UndefindeValues kann nur f�r Parameter aus dem Parameterset �VALUES� abgefragt werden.
     */
    public Object getParamset(String address, String paramset_key)
    {
        logger.finer("getParamset(address=" + address + ", paramset_key=" + paramset_key + ")");

        if (address.endsWith("4260:12"))
            System.nanoTime();

        HashMap<String, Object> result = new HashMap<>();

        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            result = device.getParamset(address, paramset_key);
        else
        {
            logger.warning("device " + address + " not found");
            /*
             * if (!address.isEmpty()) { String basis = address; if (basis.contains(":")) basis = basis.substring(0, basis.indexOf(":")); bridge.deleteDevice(basis); }
             */
            // return bridge.getDummyDevice(address).getParamset(address, paramset_key);
        }

        logger.finer("returns " + format(result));
        return result;

    }

    /**
     * Mit dieser Methode wird die Beschreibung eines Parameter-Sets ermittelt. Der Parameter address ist die Adresse eines logischen Ger�tes (z.B. von listDevices zur�ckgegeben). Der Parameter paramset_type ist �MASTER�, �VALUES� oder �LINK�.
     */
    public Object getParamsetDescription(String address, String paramset_type)
    {
        logger.finer("getParamsetDescription(address=" + address + ", paramset_type=" + paramset_type + ")");

        if (address.endsWith("4260:12"))
            System.nanoTime();

        HashMap<String, Object> result = new HashMap<>();

        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            result = device.getParamsetDescription(address, paramset_type);
        else
        {
            logger.warning("device " + address + " not found");
            /*
             * if (!address.isEmpty()) { String basis = address; if (basis.contains(":")) basis = basis.substring(0, basis.indexOf(":")); bridge.deleteDevice(basis); }
             */
            // return bridge.getDummyDevice(address).getParamsetDescription(address, paramset_type);
        }

        logger.finer("returns " + format(result));
        return result;
    }

    /**
     * Diese Methode gibt die Id eines Parametersets zur�ck. Diese wird verwendet, um spezialisierte Konfigurationsdialoge (Easymode) den Parametersets zuzuordnen.
     */
    public Object getParamsetId(String address, String type)
    {
        logger.finer("getParamsetId(address=" + address + ", type=" + type + ")");

        String result = "";

        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            result = device.getParamsetId(address, type);
        else
        {
            logger.warning("device " + address + " not found");
            /*
             * if (!address.isEmpty()) { String basis = address; if (basis.contains(":")) basis = basis.substring(0, basis.indexOf(":")); bridge.deleteDevice(basis); }
             */
            // return bridge.getDummyDevice(address).getParamsetId(address, type);
        }

        logger.finer("returns " + format(result));
        return result;
    }

    /**
     * Mit dieser Methode wird ein einzelner Wert aus dem Parameter-Set �VALUES� gelesen. Der Parameter address ist die Addresse eines logischen Ger�tes. Der Parameter value_key ist der Name des zu lesenden Wertes. Die m�glichen Werte f�r value_key ergeben sich aus der ParamsetDescription des entsprechenden Parameter-Sets �VALUES�. Dem Parameter mode k�nnen folgende Werte �bergeben werden: 0 default: Keien Auswirkung, die Funktion verh�lt sicht wie der Aufruf ohne mode 1 UndefinedValues: Es wird ein Struktur zur�ckgeliefert die folgenden Aufbau hat: �UNDEFINED�(Boolean) Flag ob der angeforderte Wert initial gesetzt wurde und somit wahrscheinlich nicht der Realit�t entspricht oder ob der Wert von eimen Ger�t empfangen wurde, true = Wert wurde initial gesetzt und noch nicht ver�ndert, false
     * = der Wert wurde neu gesetzt �VALUE�(ValueType) Wert des angeforderten Parameter. UndefindeValues kann nur f�r Parameter aus dem Parameterset �VALUES� abgefragt werden die mit OPERATIONS = Read gekenzeichnet sind.
     */
    public Object getValue(String address, String value_key)
    {
        logger.finer("getValue(address=" + address + ", value_key=" + value_key + ")");
        Object result = new Object[0];
        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            result = device.getValue(address, value_key);
        else
            logger.warning("device not found " + address);

        logger.finer("returns " + format(result));
        return result;
    }

    /**
     * Mit dieser Methode teilt die Logikschicht dem Schnittstellenprozess mit, dass sie gerade gestartet wurde. Der Schnittstellenprozess wird sich daraufhin selbst initialisieren und z.B. mit listDevices() die der Logikschicht bekannten Ger�te abfragen. Der Parameter url gibt die Adresse des XmlRpc-Servers an, unter der die Logikschicht zu erreichen ist. Der Parameter interface_id teilt dem Schnittstellenprozess die Id, mit unter der er sich gegen�ber der Logikschicht identifiziert. Zum Abmelden von der Ereignisbehandlung wird interface_id l
     */
    public Object init(String url, String interface_id)
    {
        logger.fine("url = " + url + ", interface_id = " + interface_id);

        bridge.ccuInterfaceConnected(url, interface_id);

        return new Object[0];
    }

    /**
     * Diese Methode gibt alle dem Schnittstellenprozess bekannten Ger�te in Form von Ger�tebeschreibungen zur�ck.
     */
    public Object listDevices()
    {
        logger.finer("listDevices()");

        List<Map<String, Object>> result = new ArrayList<>();
        for (HomematicDevice act : bridge.devices.values())
        {
            result.addAll(act.getAllDeviceDescriptons());
        }

        logger.finer("returns " + format(result));
        return result.toArray();
    }

    /**
     * Diese Methode gibt alle dem Schnittstellenprozess bekannten Ger�te in Form von Ger�tebeschreibungen zur�ck.
     */
    public Object listDevices(boolean test)
    {
        logger.finer("listDevices(" + test + ")");

        List<Map<String, Object>> result = new ArrayList<>();
        for (HomematicDevice act : bridge.devices.values())
        {
            result.addAll(act.getAllDeviceDescriptons());
        }

        logger.finer("returns " + format(result));
        return result.toArray();
    }

    /**
     * Mit dieser Methode wird ein komplettes Parameter-Set f�r ein logisches Ger�t geschrieben. Der Parameter address ist die Addresses eines logischen Ger�tes. Der Parameter paramset_key ist �MASTER�, �VALUES� oder die Adresse eines Kommunikationspartners f�r das entsprechende Link-Parameter-Set (siehe getLinkPeers). Der Parameter set ist das zu schreibende Parameter-Set. In set nicht vorhandene Member werden einfach nicht geschrieben und behalten ihren alten Wert. (2) Es handelt sich um eine Erweiterung f�r Ger�te, die sowohl den rx_mode BURST als auch WAKEUP unterst�tzen. Mit dem Parameter rx_mode kann in diesem Fall angegeben werden, ob das �bergebene Paramset �ber BURST oder WAKEUP �bertragen werden soll. G�ltige Werte sind: BURST � �bertragung mit Burst WAKEUP � �bertragung unter
     * Verwendung des WakeUp Verfahrens.
     */
    public Object putParamset(String address, String paramset_key, HashMap<String, Object> set)
    {
        logger.finer("putParamset(address=" + address + ", paramset_key=" + paramset_key + ", set=" + set + ")");

        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            device.putParamset(address, paramset_key, set);
        else
            logger.warning("device not found " + address);

        return new Object[0];
    }

    /**
     * Diese Methode l�scht eine Kommunikationsbeziehung zwischen zwei Ger�ten. Die Parameter sender und receiver bezeichnen die beiden Kommunikationspartner deren Kommunikationszuordnung gel�scht werden soll.
     */
    public Object removeLink(String sender, String receiver)
    {
        logger.warning("removeLink(sender=" + sender + ", receiver=" + receiver + ")");
        return new Object[0];
    }

    /**
     * Diese Methode teilt dem Interfaceprozess in ref_counter mit, wie oft der Wert value_id des Kanals address innerhalb der Logikschicht (z.B. in Programmen) verwendet wird. Dadurch kann der Interfaceprozess die Verbindung mit der entsprechenden Komponente herstellen bzw. l�schen. Diese Funktion sollte bei jeder �nderung aufgerufen werden. Der R�ckgabewert ist true, wenn die Aktion sofort durchgef�hrt wurde. Er ist false, wenn die entsprechende Komponente nicht erreicht werden konnte und vom Benutzer zun�chst in den Config-Mode gebracht werden muss. Der Interfaceprozess hat dann aber die neue Einstellung �bernommen und wird sie bei n�chster Gelegenheit automatisch an die Komponente �bertragen. In diesem Fall ist dann auch der Wert CONFIG_PENDING im Kanal MAINTENANCE der Komponente
     * gesetzt.
     */
    public Object reportValueUsage(String address, String value_id, Integer ref_counter)
    {
        logger.finer("reportValueUsage(address=" + address + ", value_id=" + value_id + ", ref_counter=" + ref_counter + ")");
        return true;
    }

    /**
     * Diese Methode durchsucht den Bus nach neuen Ger�ten und gibt die Anzahl gefundener Ger�te zur�ck. Die neu gefundenen Ger�te werden mit newDevices der Logikschicht gemeldet.
     */
    public Object searchDevices()
    {
        logger.finer("searchDevices()");
        return new Object[0];
    }

    /**
     * Diese Methode �ndert die beschreibenden Texte einer Kommunikationsbeziehung. Die Parameter sender und receiver bezeichnen die beiden zu verkn�pfenden Partner. Die Parameter name und description beschreiben die Verkn�pfung textuell.
     */
    public Object setLinkInfo(String sender, String receiver, String name, String description)
    {
        logger.warning("setLinkInfo(sender=" + sender + ", receiver=" + receiver + ", name=" + name + ", description=" + description + ")");
        return new Object[0];
    }

    /**
     * Mit dieser Methode wird ein einzelner Wert aus dem Parameter-Set �VALUES� geschrieben. Der Parameter address ist die Addresse eines logischen Ger�tes. Der Parameter value_key ist der Name des zu schreibenden Wertes. Die m�glichen Werte f�r value_key ergeben sich aus der ParamsetDescription des entsprechenden Parameter-Sets �VALUES�. Der Parameter value ist der zu schreibende Wert. (2) Es handelt sich um eine Erweiterung f�r Ger�te, die sowohl den rx_mode BURST als auch WAKEUP unterst�tzen. Mit dem Parameter rx_mode kann in diesem Fall angegeben werden, ob der �bergebene Wert �ber BURST oder WAKEUP �bertragen werden soll. G�ltige Werte sind: BURST � �bertragung mit Burst WAKEUP � �bertragung unter Verwendung des WakeUp Verfahrens
     */
    public Object setValue(String address, String value_key, Double value)
    {
        logger.finer("setValueDouble(address=" + address + ", value_key=" + value_key + ", value=" + value + ")");

        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            device.setValue(address, value_key, value);
        else
            logger.warning("device not found " + address);

        return new Object[0];
    }

    public Object setValue(String address, String value_key, Boolean value)
    {
        logger.finer("setValueBoolean(address=" + address + ", value_key=" + value_key + ", value=" + value + ")");

        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            device.setValue(address, value_key, value);
        else
            logger.warning("device not found " + address);

        return new Object[0];
    }

    public Object setValue(String address, String value_key, Integer value)
    {
        logger.finer("setValueInteger(address=" + address + ", value_key=" + value_key + ", value=" + value + ")");

        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            device.setValue(address, value_key, value);
        else
            logger.warning("device not found " + address);

        return new Object[0];
    }

    public Object listMethods()
    {
        logger.finer("listMethods()");
        Object result = new Object[] { "clearConfigCache", "deleteDevice", "getDeviceDescription", "getParamset", "getParamsetDescription", "getParamsetId", "getValue", "init", "listDevices", "putParamset", "reportValueUsage", "searchDevices", "setValue", "listMethods", "methodHelp", "multicall", "updateFirmware", "replaceDevice", "listReplaceableDevices", "ping" };
        // "addLink" "getLinkInfo" "setLinkInfo", "getLinkPeers", "getLinks", "removeLink",
        logger.finer("returns " + format(result));
        return result;
    }

    public Object methodHelp(String methodName)
    {
        logger.finer("listMethods()");
        return new Object[] { "nix da" };
    }

    public Object[] multicall(Object[] objs)
    {
        logger.finer("listMethods()");
        System.out.println("multicall " + objs);

        return new Object[0];
    }

    /**
     * Diese Methode f�hrt ein Firmware-Update f�r das in device angegebene Ger�t durch. Das Ger�t wird durch seine Seriennummer spezifiziert. Der R�ckgabewert gibt an, ob das Firmware-Update erfolgreich war.
     */
    public Object updateFirmware(String address)
    {
        logger.finer("updateFirmware(device=" + address + ")");
        boolean result = false;
        HomematicDevice device = bridge.getHomematicDevice(address);
        if (device != null)
            result = device.updateFirmware();
        else
            logger.warning("device not found " + address);

        return new Object[] { result };
    }

    /**
     * Mit dieser Funktion kann ein altes gegen ein neues Ger�t ausgetauscht werden. Alle direkten Ger�teverkn�pfungen und Konfigurationen werden auf das neue Ger�t kopiert und das alte Ger�t gel�scht. Die Beiden Ger�te m�ssen hinsichtlich ihrer Funktionalit�t kompatibel sein. Mit der Mathode listReplaceableDevice() kann eine List kompatibeler Ger�te abgefragt werden. Das neue Ger�t muss an dem Schnittstellenprozess angemeldet sein und darf noch nicht in Verkn�pfungen verwendet werden. �ber die Parameter oldDeviceAddress und newDeviceAddress wird der Methode die Adresse des alten Ger�tes und des neuen Ger�tes �bergeben. Der R�ckgabewert ist true wenn der Tausch erfolgreich war, ansonsten false
     */
    public Object replaceDevice(String oldDeviceAddress, String newDeviceAddress)
    {
        logger.finer("replaceDevice(oldDeviceAddress=" + oldDeviceAddress + ", newDeviceAddress=" + newDeviceAddress + ")");
        return true;
    }

    /**
     * Mit dieser Funktionen kann eine Liste der Ger�te angefordert werden die durch das �bergebene Ger�t ersetzt werden k�nnen. �ber den Parameter newDeviceAddress wird die Adresse des neuen Ger�ts �bergeben f�r die die m�glichen Tauschpartner ermittelt werden sollen. Der R�ckgabewert ist ein Array der DeviceDescriptions. Diese Array enth�lt die Ger�te und die Ger�tekan�le.
     */
    public Object listReplaceableDevices(String newDeviceAddress)
    {
        logger.finer("listReplaceableDevices(newDeviceAddress=" + newDeviceAddress + ")");
        return new Object[0];
    }

    /**
     * Beim Aufruf dieser Funktion wird ein Event (im Folgenden PONG genannt) erzeugt und an alle registrierten Logikschichten versandt. Da das PONG Event an alle registrierten Logikschichten (wie bei allen anderen Events auch) verschickt wird, muss in einer Logikschicht damit gerechnet werden, ein PONG Event zu empfangen ohne zuvor ping aufgerufen zu haben. Der Parameter callerId ist vom Aufrufer zu �bergeben und wird als Wert des PONG Events verwendet. Der Inhalt des String ist unerheblich. Tritt w�hrend der Verarbeitung keine Exception auf, so wird von der Methode true zur�ckgegeben. Das PONG Event wird �ber die event Methode der Logikschicht ausgeliefert. Die Adresse ist dabei immer �CENTRAL�, der key lautet �PONG� und der Wert ist die im ping Aufruf �bergebene callerId.
     */
    public Object ping(String callerId)
    {
        logger.finer("ping(callerId=" + callerId + ")");
        return new Object[] { true };
    }

    public static String getHomematicAddress(int deviceId)
    {
        String result = "" + deviceId;
        for (int i = result.length() + 3; i < 10; i++)
        {
            result = "0" + result;
        }

        result = "162" + result;
        return result;
    }
}
