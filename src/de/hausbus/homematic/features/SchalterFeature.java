package de.hausbus.homematic.features;

import java.util.HashMap;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.BooleanParameter;
import de.hausbus.homematic.parameter.Direction;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.proxy.Schalter;
import de.hausbus.proxy.schalter.params.EState;

public class SchalterFeature extends AHomematicDevice
{
    private volatile Schalter schalter;

    public SchalterFeature(Schalter feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("SWITCH", index, busDevice, parent, bridge, feature, true, 1);
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
        addValuesParameter("STATE", false, new BooleanParameter().setControl("SWITCH.STATE"));
        addValuesParameter("ON_TIME", 0, new IntegerParameter(0, 65535).setUnit("s"));
        addValuesParameter("ON_DELAY", 0, new IntegerParameter(0, 65535).setUnit("s"));
        addValuesParameter("OFF_DELAY", 0, new IntegerParameter(0, 65535).setUnit("s"));
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
        valuesParamset.put("STATE", actStatus.getState() == EState.ON);
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        Boolean newValue = null;

        if (message.getData() instanceof de.hausbus.proxy.schalter.data.EvOn)
            newValue = true;
        else if (message.getData() instanceof de.hausbus.proxy.schalter.data.EvOff)
            newValue = false;
        else if (message.getData() instanceof de.hausbus.proxy.schalter.data.Status)
            newValue = ((de.hausbus.proxy.schalter.data.Status) message.getData()).getState() == EState.ON;

        if (newValue != null)
            setValueAndSendEvent("STATE", (boolean) newValue);
    }

    @Override
    protected void setValue(String key, Object value)
    {
        super.setValue(key, value);

        if (key.equals("STATE") && value instanceof Boolean)
        {
            try
            {
                boolean myNewValue = (boolean) value;
                if (myNewValue)
                    schalter.on(getParamValue("ON_TIME"), getParamValue("ON_DELAY"));
                else
                    schalter.off(getParamValue("OFF_DELAY"));
            } catch (BusException e)
            {
            }
        } else if (key.equals("ON_TIME") && value instanceof Integer)
        {
            int onTime = (int) value;
            logger.fine(address + ": ON_TIME = " + onTime);
            valuesParamset.put("ON_TIME", onTime);
        }
    }

    private int getParamValue(String param)
    {
        Integer paramValue = (Integer) valuesParamset.get(param);
        if (paramValue == null || paramValue < 0)
            paramValue = 0;
        return paramValue;
    }

    @Override
    public void configureParams(HashMap<String, Object> params)
    {

    }
}
