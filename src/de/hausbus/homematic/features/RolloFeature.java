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
import de.hausbus.proxy.RGBDimmer;
import de.hausbus.proxy.Rollladen;
import de.hausbus.proxy.rollladen.params.EDirection;

public class RolloFeature extends AHomematicDevice
{
    private volatile Rollladen rollo;

    public RolloFeature(Rollladen feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("BLIND", index, busDevice, parent, bridge, feature, true, -100);
    }

    @Override
    protected void createMasterParamset()
    {
        addMasterParameter("OPENING_TIME", 50, new FloatParameter(1d, 600.0d).setUnit("s"));
        addMasterParameter("CLOSING_TIME", 50, new FloatParameter(1d, 600.0d).setUnit("s"));
        addMasterParameter("INVERT", false, new BooleanParameter());
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("LEVEL", 0.0d, new FloatParameter(0.0d, 1.0d).setUnit("100%").setControl("BLIND.LEVEL").setOperations(Operations.READ_WRITE_EVENT));
        addValuesParameter("TO_OPEN", true, new ActionParameter("NONE").setOperations(Operations.WRITE));
        addValuesParameter("TO_CLOSE", true, new ActionParameter("NONE").setOperations(Operations.WRITE));
        addValuesParameter("TOGGLE", true, new ActionParameter("NONE").setOperations(Operations.WRITE));
        addValuesParameter("MOVE_DISTANCE", 0.0d, new IntegerParameter(-100, 100).setOperations(Operations.READ_WRITE_EVENT));
        addValuesParameter("WORKING", false, new BooleanParameter().setOperations(Operations.READ_EVENT));
        addValuesParameter("DIRECTION", "NONE", new EnumParameter("NONE", "UP", "DOWN", "UNDEFINED").setOperations(Operations.READ_EVENT));

        addValuesParameter("STOP", true, new ActionParameter("BLIND.STOP").setOperations(Operations.WRITE));
    }

    @Override
    protected void initReference()
    {
        rollo = (Rollladen) feature;
    }

    @Override
    protected void initValues() throws BusException
    {
        de.hausbus.proxy.rollladen.data.Status actStatus = rollo.getStatus();
        valuesParamset.put("LEVEL", toHomematicLevel(actStatus.getPosition()));

        de.hausbus.proxy.rollladen.data.Configuration config = rollo.getConfiguration();
        masterParamset.put("OPENING_TIME", (double) config.getOpenTime());
        masterParamset.put("CLOSING_TIME", (double) config.getCloseTime());
        masterParamset.put("INVERT", config.getOptions().isInvertDirection());
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        if (message.getData() instanceof de.hausbus.proxy.rollladen.data.EvClosed)
        {
            de.hausbus.proxy.rollladen.data.EvClosed actStatus = (de.hausbus.proxy.rollladen.data.EvClosed) message.getData();
            setValueAndSendEvent("LEVEL", toHomematicLevel(actStatus.getPosition()));
            setValueAndSendEvent("WORKING", false);
            setValueAndSendEvent("DIRECTION", "NONE");
        } else if (message.getData() instanceof de.hausbus.proxy.rollladen.data.EvOpen)
        {
            setValueAndSendEvent("LEVEL", toHomematicLevel(0));
            setValueAndSendEvent("WORKING", false);
            setValueAndSendEvent("DIRECTION", "NONE");
        } else if (message.getData() instanceof de.hausbus.proxy.rollladen.data.EvStart)
        {
            de.hausbus.proxy.rollladen.data.EvStart actStatus = (de.hausbus.proxy.rollladen.data.EvStart) message.getData();
            EDirection direction = actStatus.getDirection();
            if (direction == EDirection.TO_CLOSE)
                setValueAndSendEvent("DIRECTION", "DOWN");
            if (direction == EDirection.TO_OPEN)
                setValueAndSendEvent("DIRECTION", "UP");

            setValueAndSendEvent("WORKING", true);
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
                rollo.moveToPosition(fromHomematicLevel(value));
            } catch (BusException e)
            {
            }
        } else if (key.equals("MOVE_DISTANCE") && value instanceof Integer)
        {
            int actLevel = fromHomematicLevel(valuesParamset.get("LEVEL"));
            int diff = (int)value;
            int newLevel = actLevel + diff;
            if (newLevel < 0)
                newLevel = 0;
            else if (newLevel > 100)
                newLevel = 100;
            logger.finer("actLevel = " + actLevel + ", diff = " + diff + ", newLevel = " + newLevel);

            try
            {
                rollo.moveToPosition(newLevel);
            } catch (BusException e)
            {
            }
        } else if (key.equals("STOP"))
        {
            try
            {
                rollo.stop();
            } catch (BusException e)
            {
            }
        } else if (key.equals("TO_OPEN"))
        {
            try
            {
                rollo.start(EDirection.TO_OPEN);
            } catch (BusException e)
            {
            }
        } else if (key.equals("TO_CLOSE"))
        {
            try
            {
                rollo.start(EDirection.TO_CLOSE);
            } catch (BusException e)
            {
            }
        } else if (key.equals("TOGGLE"))
        {
            try
            {
                rollo.start(EDirection.TOGGLE);
            } catch (BusException e)
            {
            }
        }
    }

    @Override
    public void configureParams(HashMap<String, Object> params)
    {
        Object openingTime = params.get("OPENING_TIME");
        Object closingTime = params.get("CLOSING_TIME");
        Object invert = params.get("INVERT");
        if (openingTime != null && closingTime != null && invert != null)
        {
            try
            {
                de.hausbus.proxy.rollladen.data.Configuration config = rollo.getConfiguration();
                config.setOpenTime(((Double) openingTime).intValue());
                config.setCloseTime(((Double) closingTime).intValue());
                config.setOptions(config.getOptions().setInvertDirection((boolean) invert));
                rollo.setConfiguration(config);
            } catch (BusException e)
            {
                throw new RuntimeException("configuration failed");
            }
        }
    }
}
