package de.hausbus.homematic.features;

import java.util.HashMap;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.homematic.parameter.Operations;
import de.hausbus.proxy.RFIDReader;

public class RfidFeature extends AHomematicDevice
{
    private volatile RFIDReader rfidReader;

    public RfidFeature(RFIDReader feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("ANALOG_INPUT_TRANSMITTER", index, busDevice, parent, bridge, feature, false, 1);
    }

    @Override
    protected void createMasterParamset()
    {
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("TAG_ID", 0, new IntegerParameter(0, Integer.MAX_VALUE).setControl("ANALOG_INPUT_TRANSMITTER.VALUE").setOperations(Operations.READ_EVENT));
    }

    @Override
    protected void initReference()
    {
        rfidReader = (RFIDReader) feature;
    }

    @Override
    protected void initValues()  throws BusException
    {
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        if (message.getData() instanceof de.hausbus.proxy.rFIDReader.data.EvData)
        {
            de.hausbus.proxy.rFIDReader.data.EvData data = (de.hausbus.proxy.rFIDReader.data.EvData) message.getData();
            setValueAndSendEvent("TAG_ID", data.getTagID());
        }
    }

    @Override
    protected void setValue(String key, Object value)
    {
        logger.finer("setValue " + key + "=" + value);
    }

    @Override
    protected void configureParams(HashMap<String, Object> params)
    {
    }
}
