package de.hausbus.homematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;

public class DummyHomematicDevice extends AHomematicDevice
{
    private Map<String, AHomematicDevice> features= new LinkedHashMap<>();

    DummyHomematicDevice(String address, HomematicBridge bridge)
    {
        super("Dummy" + address, null, address, null, bridge, null, false, 1);
    }

    @Override
    protected void createMasterParamset()
    {
    }

    @Override
    protected void createValuesParamset()
    {
    }

    public Map<String, Object> getDeviceDescription(String address)
    {
        if (this.address.equals(address))
            return getDeviceDescription();

        AHomematicDevice child = features.get(address);
        if (child != null)
            return child.getDeviceDescription();
        logger.warning("feature " + address + " not found");

        return new HashMap<>();
    }

    public List<Map<String, Object>> getAllDeviceDescriptons()
    {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(getDeviceDescription());
        for (AHomematicDevice act : features.values())
        {
            result.add(act.getDeviceDescription());
        }

        return result;
    }

    public HashMap<String, Object> getParamset(String address, String type)
    {
        if (this.address.equals(address))
            return getParamset(type);

        AHomematicDevice child = features.get(address);
        if (child != null)
            return child.getParamset(type);
        logger.warning("feature " + address + " not found");

        return new HashMap<>();
    }

    public HashMap<String, Object> getParamsetDescription(String address, String type)
    {
        if (this.address.equals(address))
            return getParamsetDescription(type);

        if (address.equals("1620002378:1"))
            System.nanoTime();

        AHomematicDevice child = features.get(address);
        if (child != null)
            return child.getParamsetDescription(type);
        logger.warning("feature " + address + " not found");

        return new HashMap<>();
    }

    public String getParamsetId(String address, String type)
    {
        if (this.address.equals(address))
            return getParamsetId(type);

        AHomematicDevice child = features.get(address);
        if (child != null)
            return child.getParamsetId(type);
        logger.warning("feature " + address + " not found");

        return "unknown 2";
    }

    public void putParamset(String address, String paramset_key, HashMap<String, Object> params)
    {
    }

    @Override
    protected void configureParams(HashMap<String, Object> params)
    {
    }

    public void setValue(String address, String key, Object value)
    {
    }

    public Object getValue(String address, String key)
    {
        return null;
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
    }

    public boolean updateFirmware()
    {
        return true;
    }

    @Override
    protected void initReference()
    {
    }

    @Override
    protected void initValues() throws BusException
    {
    }

    @Override
    protected void setValue(String key, Object value)
    {
    }
}
