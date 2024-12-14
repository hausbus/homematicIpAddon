package de.hausbus.homematic.features;

import java.util.HashMap;
import java.util.logging.Level;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.ActionParameter;
import de.hausbus.homematic.parameter.BooleanParameter;
import de.hausbus.homematic.parameter.EnumParameter;
import de.hausbus.homematic.parameter.FloatParameter;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.homematic.parameter.Operations;
import de.hausbus.proxy.Dimmer;
import de.hausbus.proxy.Helligkeitssensor;
import de.hausbus.proxy.RGBDimmer;
import de.hausbus.proxy.dimmer.params.EDirection;

public class DimmerFeature extends AHomematicDevice
{
    private volatile Dimmer dimmer;
    private volatile int oldLevel = 100;
    public RgbFeature rgbFeature = null;
    public int rgbColor = 0;

    public DimmerFeature(Dimmer feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("DIMMER", index, busDevice, parent, bridge, feature, true, 100);
    }

    @Override
    protected void createMasterParamset()
    {
        addMasterParameter("RAMP_TIME", 12, new IntegerParameter(1, 250).setUnit("50 ms"));
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("LEVEL", 0.0d, new FloatParameter(0d, 1.0d).setControl("DIMMER.LEVEL").setUnit("100%"));
        addValuesParameter("DIMM", "TOGGLE", new EnumParameter("TO_LIGHT", "TO_DARK", "TOGGLE"));
        addValuesParameter("RAMP_TIME", 12, new IntegerParameter(1, 250).setControl("NONE").setUnit("50ms"));
        addValuesParameter("ON_TIME", 0, new IntegerParameter(0, 250).setControl("NONE").setUnit("s"));
        addValuesParameter("RAMP_STOP", true, new ActionParameter("NONE").setOperations(Operations.WRITE));
        addValuesParameter("OLD_LEVEL", true, new ActionParameter("NONE").setOperations(Operations.WRITE));
        addValuesParameter("WORKING", false, new BooleanParameter());
    }

    @Override
    protected void initReference()
    {
        dimmer = (Dimmer) feature;
    }

    @Override
    protected void initValues() throws BusException
    {
        de.hausbus.proxy.dimmer.data.Status actStatus = dimmer.getStatus();
        valuesParamset.put("LEVEL", toHomematicLevel(actStatus.getBrightness()));

        de.hausbus.proxy.dimmer.data.Configuration config = dimmer.getConfiguration();
        if (device.getName().contains("PWM ") && config.getFadingTime() != 1)
        {
            config.setFadingTime(1);
            dimmer.setConfiguration(config);
        }

        masterParamset.put("RAMP_TIME", config.getFadingTime());
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        int newValue = -1;

        if (message.getData() instanceof de.hausbus.proxy.dimmer.data.EvOn)
            newValue = ((de.hausbus.proxy.dimmer.data.EvOn) message.getData()).getBrightness();
        else if (message.getData() instanceof de.hausbus.proxy.dimmer.data.EvOff)
            newValue = 0;
        else if (message.getData() instanceof de.hausbus.proxy.dimmer.data.Status)
            newValue = ((de.hausbus.proxy.dimmer.data.Status) message.getData()).getBrightness();

        if (newValue != -1)
        {
            if (newValue > 0)
                oldLevel = newValue;
            setValueAndSendEvent("WORKING", false);
            setValueAndSendEvent("LEVEL", toHomematicLevel(newValue));
        }
    }

    @Override
    protected void setValue(String key, Object value)
    {
        super.setValue(key, value);

        if (key.equals("LEVEL") && value instanceof Double)
        {
            try
            {
                setValueAndSendEvent("WORKING", true);
                dimmer.setBrightness(fromHomematicLevel(value), getOnTime());
            } catch (BusException e)
            {
            }
        } else if (key.equals("OLD_LEVEL"))
        {
            try
            {
                setValueAndSendEvent("WORKING", true);
                dimmer.setBrightness(oldLevel, getOnTime());
            } catch (BusException e)
            {
            }
        } else if (key.equals("RAMP_STOP"))
        {
            try
            {
                dimmer.stop();
            } catch (BusException e)
            {
            }
        } else if (key.equals("RAMP_TIME") && value instanceof Integer)
            configureRampTime(value);
        else if (key.equals("DIMM") && value instanceof Integer)
        {
            try
            {
                int myValue = (int) value;

                EDirection direction;
                if (myValue == 0)
                    direction = EDirection.TO_LIGHT;
                else if (myValue == 1)
                    direction = EDirection.TO_DARK;
                else if (myValue == 2)
                    direction = EDirection.TOGGLE;
                else
                {
                    logger.warning("unknown dimm direction " + myValue);
                    return;
                }
                setValueAndSendEvent("WORKING", true);
                dimmer.start(direction);
            } catch (BusException e)
            {
            }
        } else if (key.equals("ON_TIME") && value instanceof Integer)
        {
            int onTime = (int) value;
            logger.fine("ON_TIME = " + onTime);
            valuesParamset.put("ON_TIME", onTime);
        }
    }

    private void configureRampTime(Object value)
    {
        try
        {
            int rampTime = (Integer) value;
            de.hausbus.proxy.dimmer.data.Configuration config = dimmer.getConfiguration();
            config.setFadingTime(rampTime);
            config.setDimmingTime(rampTime);
            dimmer.setConfiguration(config);
            sendEvent("RAMP_TIME", value);
        } catch (BusException e)
        {
            throw new RuntimeException("configuration failed");
        }

    }

    private int getOnTime()
    {
        Integer onTime = (Integer) valuesParamset.get("ON_TIME");
        if (onTime == null || onTime < 0)
            onTime = 0;
        return onTime;
    }

    @Override
    public void configureParams(HashMap<String, Object> params)
    {
        if (params.containsKey("RAMP_TIME"))
            configureRampTime(params.get("RAMP_TIME"));
    }
}
