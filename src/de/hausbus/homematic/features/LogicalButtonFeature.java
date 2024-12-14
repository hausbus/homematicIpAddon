package de.hausbus.homematic.features;

import java.util.HashMap;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.FloatParameter;
import de.hausbus.proxy.Led;
import de.hausbus.proxy.LogicalButton;

public class LogicalButtonFeature extends AHomematicDevice
{
    private volatile LogicalButton logicalButton;

    public LogicalButtonFeature(LogicalButton led, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("DIMMER", index, busDevice, parent, bridge, led, true, 100);
    }

    @Override
    protected void createMasterParamset()
    {
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("LEVEL", 0.0d, new FloatParameter(0d, 1.0d).setControl("DIMMER.LEVEL"));
    }

    @Override
    protected void initReference()
    {
        logicalButton = (LogicalButton) feature;
    }

    @Override
    protected void initValues() throws BusException
    {
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        int newBrightness = -1;

        if (message.getData() instanceof de.hausbus.proxy.logicalButton.data.EvOn)
            newBrightness = ((de.hausbus.proxy.logicalButton.data.EvOn) message.getData()).getBrightness();
        else if (message.getData() instanceof de.hausbus.proxy.logicalButton.data.EvOff)
            newBrightness = 0;
        else if (message.getData() instanceof de.hausbus.proxy.logicalButton.data.Status)
            newBrightness = ((de.hausbus.proxy.led.data.Status) message.getData()).getBrightness();

        if (newBrightness != -1)
            setValueAndSendEvent("LEVEL", toHomematicLevel(newBrightness));
    }

    @Override
    protected void setValue(String key, Object value)
    {
        if (key.equals("LEVEL") && value instanceof Double)
        {
            try
            {
                logicalButton.setMinBrightness(fromHomematicLevel(value));
                setValueAndSendEvent("LEVEL", value);
            } catch (BusException e)
            {
            }
        }
    }

    @Override
    protected void configureParams(HashMap<String, Object> params)
    {
    }
}
