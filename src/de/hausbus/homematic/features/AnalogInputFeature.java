package de.hausbus.homematic.features;

import java.util.HashMap;
import java.util.logging.Level;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.FloatParameter;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.homematic.parameter.Operations;
import de.hausbus.proxy.AnalogEingang;

public class AnalogInputFeature extends AHomematicDevice
{
    private volatile AnalogEingang analogInput;

    public AnalogInputFeature(AnalogEingang feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("ANALOG_INPUT_TRANSMITTER", index, busDevice, parent, bridge, feature, false, 1);
    }

    @Override
    protected void createMasterParamset()
    {
        addMasterParameter("SEND_INTERVAL", 150, new IntegerParameter(1, 255).setUnit("s"));
        addMasterParameter("SEND_DELTA", 0.5d, new FloatParameter(0.1d, 25.0d).setUnit("V"));
        addMasterParameter("CALIBRATION", 0, new FloatParameter(-10.0d, 10.0d).setUnit("V"));
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("VALUE", 0, new IntegerParameter(0, 100).setControl("ANALOG_INPUT_TRANSMITTER.VALUE").setOperations(Operations.READ_EVENT));
    }

    @Override
    protected void initReference()
    {
        analogInput = (AnalogEingang) feature;
    }

    @Override
    protected void initValues() throws BusException
    {
        de.hausbus.proxy.analogEingang.data.Configuration config = analogInput.getConfiguration();
        if (config.getReportTimeBase() != 1)
        {
            config.setReportTimeBase(1);
            config.setMinReportTime(1);
            config.setMaxReportTime(240);
            analogInput.setConfiguration(config);
        }

        masterParamset.put("SEND_INTERVAL", config.getMaxReportTime() * config.getReportTimeBase());
        masterParamset.put("SEND_DELTA", (double) (config.getHysteresis() * 0.1));
        masterParamset.put("CALIBRATION", (double) (config.getCalibration() * 0.1));

        de.hausbus.proxy.analogEingang.data.Status actStatus = analogInput.getStatus();
        valuesParamset.put("VALUE", actStatus.getValue());
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        int newValue = -1;

        if (message.getData() instanceof de.hausbus.proxy.analogEingang.data.EvStatus)
        {
            de.hausbus.proxy.analogEingang.data.EvStatus status = (de.hausbus.proxy.analogEingang.data.EvStatus) message.getData();
            newValue = status.getValue();
        }

        if (newValue != -1)
            setValueAndSendEvent("VALUE", newValue);
    }

    @Override
    protected void setValue(String key, Object value)
    {
        logger.finer("setValue " + key + "=" + value);
    }

    @Override
    protected void configureParams(HashMap<String, Object> params)
    {
        boolean changes = false;
        try
        {
            de.hausbus.proxy.analogEingang.data.Configuration config = analogInput.getConfiguration();

            Object value = params.get("CALIBRATION");
            if (value != null)
            {
                int myValue = (int) (((double) value) * 10);
                if (config.getCalibration() != myValue)
                {
                    config.setCalibration(myValue);
                    changes = true;
                }
            }

            value = params.get("SEND_DELTA");
            if (value != null)
            {
                int myValue = (int) (((double) value) * 10);
                if (config.getHysteresis() != myValue)
                {
                    config.setHysteresis(myValue);
                    changes = true;
                }
            }

            value = params.get("SEND_INTERVAL");
            if (value != null)
            {
                int myValue = ((int) value) / config.getReportTimeBase();
                if (config.getMaxReportTime() != myValue)
                {
                    if (myValue < 1)
                        myValue = 1;
                    config.setMinReportTime(1);
                    config.setReportTimeBase(10);
                    config.setMaxReportTime(myValue);
                    changes = true;
                }
            }

            if (changes)
            {
                logger.finer("setting " + config);
                analogInput.setConfiguration(config);
            }
        } catch (BusException e)
        {
            throw new RuntimeException("configuration failed");
        }
    }
}
