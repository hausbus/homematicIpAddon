package de.hausbus.homematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hausbus.com.BusDevice;
import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.com.IBusFeature;
import de.hausbus.homematic.parameter.AParameter;
import de.hausbus.homematic.parameter.BooleanParameter;
import de.hausbus.homematic.parameter.Direction;
import de.hausbus.homematic.parameter.Flags;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.homematic.parameter.Operations;
import de.hausbus.proxy.controller.data.ModuleId;

public abstract class AHomematicDevice
{
    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected AHomematicDevice parent = null;
    protected IBusDevice device = null;
    protected final String address;
    protected final String type;
    protected final Integer index;
    protected final int deviceId;
    protected final HomematicBridge bridge;
    protected final IBusFeature feature;

    protected final TreeMap<String, Object> deviceDescription = new TreeMap<>();
    private final String masterParamSetId;
    protected final HashMap<String, Object> masterParamset = new HashMap<>();
    protected final HashMap<String, Object> masterParamsetDescription = new HashMap<>();
    private final String valuesParamSetId;
    protected final HashMap<String, Object> valuesParamset = new HashMap<>();
    protected final HashMap<String, Object> valuesParamsetDescription = new HashMap<>();

    private String persistenceDir = "./";
    public final HashMap<String, Object> persistence = new HashMap<>();
    protected boolean locked = false;
    protected final int homematicValueFactor;

    protected AHomematicDevice(String type, Integer index, IBusDevice device, AHomematicDevice parent, HomematicBridge bridge, IBusFeature feature, boolean supportInhibit, int homematicValueFactor)
    {
        this.parent = parent;
        this.type = type;
        this.index = index;
        this.deviceId = device.getDeviceId();
        this.device = device;
        this.bridge = bridge;
        this.feature = feature;
        this.homematicValueFactor = homematicValueFactor;

        address = AHomematicDevice.getHomematicAddress(deviceId, index);
        masterParamSetId = address + "_master";
        valuesParamSetId = address + "_values";
        loadPersistence();
        create();
        initReference();

        try
        {
            logger.finer(address + " init values");
            initValues();
        } catch (BusException e)
        {
        }

        if (supportInhibit)
        {
            addValuesParameter("INHIBIT", false, new BooleanParameter().setOperations(Operations.READ_WRITE_EVENT));
            // sendEvent("INHIBIT", false);
        }

        logger.finer("Device " + address + " created: " + toString());
    }

    protected AHomematicDevice(String type, Integer index, String address, AHomematicDevice parent, HomematicBridge bridge, IBusFeature feature, boolean supportInhibit, int homematicValueFactor)
    {
        this.parent = parent;
        this.type = type;
        this.index = index;
        String basis = address;
        if (basis.contains(":"))
            basis = basis.substring(0, basis.indexOf(":"));
        this.deviceId = getHausBusAddress(Integer.parseInt(basis));
        this.bridge = bridge;
        this.feature = feature;
        this.homematicValueFactor = homematicValueFactor;

        this.address = address;
        masterParamSetId = address + "_master";
        valuesParamSetId = address + "_values";
        loadPersistence();
        create();
        initReference();

        try
        {
            logger.finer(address + " init values");
            initValues();
        } catch (BusException e)
        {
        }

        if (supportInhibit)
        {
            addValuesParameter("INHIBIT", false, new BooleanParameter().setOperations(Operations.READ_WRITE_EVENT));
            // sendEvent("INHIBIT", false);
        }

        logger.finer("Device " + address + " created: " + toString());
    }

    protected abstract void initReference();

    private void loadPersistence()
    {
        File file = getPersistenceFile();
        if (file.exists())
        {
            try (ObjectInputStream oin = new ObjectInputStream(new FileInputStream(file)))
            {
                persistence.putAll((HashMap<String, Object>) oin.readObject());
                logger.finer("loaded: " + persistence);
            } catch (Exception e)
            {
                logger.log(Level.SEVERE, "", e);
            }
        }
    }

    public void savePersistence()
    {
        File file = getPersistenceFile();
        try (ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(file)))
        {
            oout.writeObject(persistence);
            logger.finer("saved: " + persistence);
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "", e);
        }
    }

    public <T> T getFromPersistence(String key, T defaultValue)
    {
        Object result = persistence.get(key);
        if (result == null || result.getClass() != defaultValue.getClass())
            return defaultValue;
        return (T) result;
    }

    private File getPersistenceFile()
    {
        return new File(persistenceDir + address.replace(":", "_") + ".bin");
    }

    protected void create()
    {
        deviceDescription.clear();

        // Kanäle
        if (index != null)
        {
            deviceDescription.put("PARAMSETS", new Object[] { "MASTER", "VALUES" });
            if (device == null)
                deviceDescription.put("PARENT_TYPE", address.substring(0, address.indexOf(":")));
            else
                deviceDescription.put("PARENT_TYPE", device.getName().replaceAll(" ", "-"));
            deviceDescription.put("PARENT", parent.getAddress());
            deviceDescription.put("INDEX", (int) index);
            deviceDescription.put("FLAGS", Flags.VISIBLE);
            deviceDescription.put("DIRECTION", Direction.DIRECTION_NONE);
            deviceDescription.put("LINK_SOURCE_ROLES", "");
            deviceDescription.put("LINK_TARGET_ROLES", "");
        }
        // Gerät
        else
        {
            deviceDescription.put("PARAMSETS", new Object[] { "MASTER" });
            deviceDescription.put("UPDATABLE", 1);
            deviceDescription.put("PARENT", "");
            if (device == null)
            {
                deviceDescription.put("FIRMWARE", "1.1");
                deviceDescription.put("AVAILABLE_FIRMWARE", "1.1");
            } else
            {
                ModuleId moduleId = ((BusDevice) device).getModuleIdFromCacheOrCall();
                deviceDescription.put("FIRMWARE", moduleId.getMajorRelease() + "." + moduleId.getMinorRelease());
                String onlineVersion = bridge.onlineFirmwareVersions.get(moduleId.getFirmwareId().name());
                if (onlineVersion == null)
                    onlineVersion = "offline?";
                deviceDescription.put("AVAILABLE_FIRMWARE", onlineVersion);
            }
            deviceDescription.put("FLAGS", Flags.VISIBLE);
        }

        deviceDescription.put("ADDRESS", address);
        deviceDescription.put("VERSION", 1);
        deviceDescription.put("AES_ACTIVE", 0);
        deviceDescription.put("TYPE", type.replaceAll(" ", "-"));

        if (address.equals("1620002378:1"))
            System.nanoTime();

        createMasterParamset();
        createValuesParamset();
        
        deviceDescription.put("LINK_SOURCE_ROLES", "");
        deviceDescription.put("LINK_TARGET_ROLES", "");


        if (masterParamset.isEmpty())
            addMasterParameter("NOOP", 0, new IntegerParameter(1, 999).setFlags(Flags.INTERNAL));
    }

    public boolean isUpdateable()
    {
        String loadedVersion = (String) deviceDescription.get("FIRMWARE");
        String onlineVersion = (String) deviceDescription.get("AVAILABLE_FIRMWARE");
        if (device != null)
            logger.finer(device.getDevice() + " loadedVersion = " + loadedVersion + ", onlineVersion = " + onlineVersion);
        if (loadedVersion != null && onlineVersion != null && !loadedVersion.equalsIgnoreCase(onlineVersion))
            return true;
        return false;
    }

    protected abstract void createMasterParamset();

    protected abstract void createValuesParamset();

    protected abstract void initValues() throws BusException;

    protected abstract void configureParams(HashMap<String, Object> set);

    protected void setParamset(String paramset_key, HashMap<String, Object> set)
    {
        HashMap<String, Object> map;

        if (paramset_key.equals("MASTER"))
            map = masterParamset;
        else if (paramset_key.equals("VALUES"))
            map = valuesParamset;
        else
        {
            logger.warning("unknown paramset " + paramset_key);
            return;
        }

        map.putAll(set);
    }

    public void setValueAndSendEvent(String key, Object value)
    {
        valuesParamset.put(key, value);
        sendEvent(key, value);
    }

    protected void setValue(String key, Object value)
    {
        logger.finer(address + " setValue " + key + "=" + value);
        if (key.equals("INHIBIT"))
        {
            locked = (boolean) value;
            sendEvent("INHIBIT", value);
        }
    }

    protected void sendEvent(String event, Object value)
    {
        if (locked && !"INHIBIT".equals(event))
            logger.finer(address + " event: (locked) " + event + " = " + value);
        else
        {
            logger.finer(address + " event: " + event + " = " + value);
            bridge.ccuEvent(address, event, value);
        }
    }

    protected Object getValue(String key)
    {
        Object result = valuesParamset.get(key);
        if (result == null)
            logger.warning(address + ": value for key " + key + " not found");
        else
            System.nanoTime();

        return result;
    }

    protected abstract void busMessageReceived(BusMessage message);

    protected void addChildren(Set<String> children)
    {
        deviceDescription.put("CHILDREN", children.toArray());
    }

    public String getAddress()
    {
        return address;
    }

    protected TreeMap<String, Object> getDeviceDescription()
    {
        return deviceDescription;
    }

    protected String getParamSetId(String type)
    {
        if (type.equals("MASTER"))
            return masterParamSetId;
        if (type.equals("VALUES"))
            return valuesParamSetId;
        logger.severe("unknown paramset type " + type);
        return "";
    }

    protected HashMap<String, Object> getParamset(String type)
    {
        if (address.equals("1620002378:1"))
            System.nanoTime();

        if (type.equals("MASTER"))
            return masterParamset;
        if (type.equals("VALUES"))
            return valuesParamset;
        logger.severe("unknown paramset " + type);
        return new HashMap<>();
    }

    protected String getParamsetId(String type)
    {
        if (type.equals("MASTER"))
            return masterParamSetId;
        if (type.equals("VALUES"))
            return valuesParamSetId;
        logger.severe("unknown paramset " + type);
        return "unknown";
    }

    protected HashMap<String, Object> getParamsetDescription(String type)
    {
        if (address.equals("1620002378:1"))
            System.nanoTime();

        if (type.equals("MASTER"))
            return masterParamsetDescription;

        if (type.equals("VALUES"))
            return valuesParamsetDescription;

        logger.severe("unknown paramset " + type);
        return new HashMap<>();
    }

    public static String getHomematicAddress(int deviceId, Integer index)
    {
        if (index == null)
            return "" + (1620000000 + deviceId);
        else
            return (1620000000 + deviceId) + ":" + index;
    }

    public static int getHausBusAddress(Integer homematicAddress)
    {
        if (homematicAddress == null)
            return 0;

        return homematicAddress - 1620000000;
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

    protected void addMasterParameter(String key, Object value, AParameter<?> paramset)
    {
        if (!masterParamset.containsKey(key))
        {
            paramset.setId(key);
            paramset.setTabOrder(masterParamset.size());
            paramset.setDefault(value);

            masterParamset.put(key, value);
            masterParamsetDescription.put(key, paramset.get());
        }
    }

    protected void addValuesParameter(String key, Object value, AParameter<?> paramset)
    {
        addValuesParameter(key, key, value, paramset);
    }

    protected void addValuesParameter(String id, String key, Object value, AParameter<?> paramset)
    {
        if (!valuesParamset.containsKey(key))
        {
            paramset.setId(id);
            paramset.setTabOrder(valuesParamset.size());
            paramset.setDefault(value);

            valuesParamset.put(key, value);
            valuesParamsetDescription.put(key, paramset.get());
        }
    }

    protected int fromHomematicLevel(Object level)
    {
        int result = (int) (((Double) level) * Math.abs(homematicValueFactor));

        if (homematicValueFactor < 0)
            result = Math.abs(homematicValueFactor) - result;
        return result;
    }

    protected double toHomematicLevel(int level)
    {
        if (homematicValueFactor < 0)
            level = Math.abs(homematicValueFactor) - level;

        double result = level;
        result /= Math.abs(homematicValueFactor);
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("AHomematicDevice [address=");
        builder.append(address);
        builder.append(", type=");
        builder.append(type);
        builder.append(", index=");
        builder.append(index);
        builder.append(", deviceId=");
        builder.append(deviceId);
        builder.append(", deviceDescription=");
        builder.append(deviceDescription);
        builder.append(", masterParamSetId=");
        builder.append(masterParamSetId);
        builder.append(", masterParamset=");
        builder.append(masterParamset);
        builder.append(", masterParamsetDescription=");
        builder.append(masterParamsetDescription);
        builder.append(", valuesParamSetId=");
        builder.append(valuesParamSetId);
        builder.append(", valuesParamset=");
        builder.append(valuesParamset);
        builder.append(", valuesParamsetDescription=");
        builder.append(valuesParamsetDescription);
        builder.append("]");
        return builder.toString();
    }
}
