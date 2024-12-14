package de.hausbus.homematic.features;

import java.util.HashMap;
import java.util.logging.Level;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.Direction;
import de.hausbus.homematic.parameter.FloatParameter;
import de.hausbus.proxy.Feuchtesensor;
import de.hausbus.proxy.Led;

public class LedFeature extends AHomematicDevice
{
    private volatile Led led;

    public LedFeature(Led feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("DIMMER", index, busDevice, parent, bridge, feature, true, 100);
    }

    @Override
    protected void createMasterParamset()
    {
        deviceDescription.put("PARAMSETS", new Object[] { "MASTER", "VALUES","LINK" });
        deviceDescription.put("DIRECTION", Direction.DIRECTION_RECEIVER);
        deviceDescription.put("LINK_SOURCE_ROLES", "");
        deviceDescription.put("LINK_TARGET_ROLES", "SWITCH");
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("LEVEL", 0.0d, new FloatParameter(0d, 1.0d).setControl("DIMMER.LEVEL"));
    }

    @Override
    protected void initReference()
    {
        led = (Led) feature;
    }

    @Override
    protected void initValues() throws BusException
    {
        de.hausbus.proxy.led.data.Status actStatus = led.getStatus();
        valuesParamset.put("LEVEL", toHomematicLevel(actStatus.getBrightness()));
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        int newValue = -1;

        if (message.getData() instanceof de.hausbus.proxy.led.data.EvOn)
            newValue = ((de.hausbus.proxy.led.data.EvOn) message.getData()).getBrightness();
        else if (message.getData() instanceof de.hausbus.proxy.led.data.EvOff)
            newValue = 0;
        else if (message.getData() instanceof de.hausbus.proxy.led.data.Status)
            newValue = ((de.hausbus.proxy.led.data.Status) message.getData()).getBrightness();

        if (newValue != -1)
            setValueAndSendEvent("LEVEL", toHomematicLevel(newValue));
    }

    @Override
    protected void setValue(String key, Object value)
    {
        super.setValue(key, value);

        if (key.equals("LEVEL") && value instanceof Double)
        {
            try
            {
                led.on(fromHomematicLevel(value), 0, 0);
            } catch (BusException e)
            {
            }
        }
    }

    @Override
    public void configureParams(HashMap<String, Object> params)
    {

    }
}
