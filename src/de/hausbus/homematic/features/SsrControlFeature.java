package de.hausbus.homematic.features;

import java.util.HashMap;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.BooleanParameter;
import de.hausbus.homematic.parameter.FloatParameter;
import de.hausbus.proxy.Dimmer;
import de.hausbus.proxy.Schalter;
import de.hausbus.proxy.schalter.data.EvToggle;
import de.hausbus.proxy.schalter.data.Status;

public class SsrControlFeature extends AHomematicDevice
{
    private volatile Schalter schalter;

    public SsrControlFeature(Schalter feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("DIMMER", index, busDevice, parent, bridge, feature, true, 100);
    }

    @Override
    protected void createMasterParamset()
    {
        // addMasterParameter("RAMP_TIME", 12, new IntegerParameter(1, 250).setUnit("50 ms"));
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("LEVEL", 0.0d, new FloatParameter(0d, 1.0d).setControl("DIMMER.LEVEL").setUnit("100%"));
        addValuesParameter("WORKING", false, new BooleanParameter());
    }

    @Override
    protected void initReference()
    {
        schalter = (Schalter) feature;
    }

    @Override
    protected void initValues() throws BusException
    {
        de.hausbus.proxy.schalter.data.Status actStatus = schalter.getStatus();
        int brightness = getMyBrightness(actStatus.getOnTime(), actStatus.getOffTime());

        valuesParamset.put("LEVEL", toHomematicLevel(brightness));
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        int newValue = -1;

        if (message.getData() instanceof de.hausbus.proxy.schalter.data.EvToggle)
        {
            EvToggle toggle = (de.hausbus.proxy.schalter.data.EvToggle) message.getData();
            newValue = getMyBrightness(toggle.getOnTime(), toggle.getOffTime());
        } else if (message.getData() instanceof de.hausbus.proxy.schalter.data.EvOn)
            newValue = 100;
        else if (message.getData() instanceof de.hausbus.proxy.schalter.data.EvOff)
            newValue = 0;
        else if (message.getData() instanceof de.hausbus.proxy.schalter.data.Status)
        {
            Status status = (de.hausbus.proxy.schalter.data.Status) message.getData();
            newValue = getMyBrightness(status.getOnTime(), status.getOffTime());
        }

        if (newValue != -1)
        {
            setValueAndSendEvent("WORKING", false);
            setValueAndSendEvent("LEVEL", toHomematicLevel(newValue));
        }
    }

    private int getMyBrightness(int onTime, int offTime)
    {
        int duration = onTime + offTime;

        int result = 0;
        if (duration != 0)
            result = (onTime * 100) / duration;
        if (result > 99)
            result = 100;
        else if (result < 1)
            result = 0;

        // logger.finer("onTime "+onTime+", offTime "+offTime+" = "+result);
        return result;
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
                schalter.toggleByDuty(fromHomematicLevel(value), 0);
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
