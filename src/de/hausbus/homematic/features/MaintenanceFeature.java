package de.hausbus.homematic.features;

import java.util.HashMap;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.BooleanParameter;
import de.hausbus.homematic.parameter.Flags;
import de.hausbus.homematic.parameter.IntegerParameter;

public class MaintenanceFeature extends AHomematicDevice
{
    public MaintenanceFeature(IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("MAINTENANCE", 0, busDevice, parent, bridge, null, false, 1);
    }

    @Override
    protected void create()
    {
        super.create();
        deviceDescription.put("FLAGS", Flags.VISIBLE | Flags.INTERNAL);
    }

    @Override
    protected void createMasterParamset()
    {
        addMasterParameter("Test", 1, new IntegerParameter(0, 1));
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("CONFIG_PENDING", false, new BooleanParameter().setFlags(Flags.SERVICE));
        addValuesParameter("STICKY_UNREACH", false, new BooleanParameter().setFlags(Flags.SERVICE));
        addValuesParameter("UNREACH", false, new BooleanParameter().setFlags(Flags.SERVICE));
    }

    @Override
    public void configureParams(HashMap<String, Object> params)
    {
        logger.warning("params = " + params);
    }

    public void busMessageReceived(BusMessage message)
    {
    }

    @Override
    protected void initReference()
    {
    }

    @Override
    protected void initValues()  throws BusException
    {

    }

    @Override
    protected void setValue(String key, Object value)
    {
    }
}
