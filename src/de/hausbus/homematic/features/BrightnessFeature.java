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
import de.hausbus.proxy.Feuchtesensor;
import de.hausbus.proxy.Helligkeitssensor;

public class BrightnessFeature extends AHomematicDevice
{
    private volatile Helligkeitssensor helligkeitsSensor;

    public BrightnessFeature(Helligkeitssensor feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("LUXMETER", index, busDevice, parent, bridge, feature, false, 10);
    }

    @Override
    protected void createMasterParamset()
    {
        addMasterParameter("SEND_INTERVAL", 150, new IntegerParameter(10, 2500).setUnit("s"));
        addMasterParameter("SEND_DELTA", 0.5d, new FloatParameter(0.1d, 25.0d).setUnit("&#x2103;"));
        addMasterParameter("CALIBRATION", 0, new FloatParameter(-10.0d, 10.0d).setUnit("&#x2103;"));
    }

    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("LUX", 0.0d, new FloatParameter(0d, 99d).setUnit("Lux").setControl("LUXMETER.LUX").setOperations(Operations.READ_EVENT));
    }

    @Override
    protected void initReference()
    {
        helligkeitsSensor = (Helligkeitssensor) feature;
    }

    @Override
    protected void initValues() throws BusException
    {
        de.hausbus.proxy.helligkeitssensor.data.Configuration config = helligkeitsSensor.getConfiguration();
        if (config.getReportTimeBase() != 10)
        {
            config.setReportTimeBase(10);
            helligkeitsSensor.setConfiguration(config);
        }

        masterParamset.put("SEND_INTERVAL", config.getMaxReportTime() * config.getReportTimeBase());
        masterParamset.put("SEND_DELTA", (double) (config.getHysteresis() * 0.1));
        masterParamset.put("CALIBRATION", (double) (config.getCalibration() * 0.1));

        de.hausbus.proxy.helligkeitssensor.data.Status actStatus = helligkeitsSensor.getStatus();
        valuesParamset.put("LUX", toHomematicLevel(actStatus.getBrightness()));
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        int newValue = -1;

        if (message.getData() instanceof de.hausbus.proxy.helligkeitssensor.data.EvStatus)
        {
            de.hausbus.proxy.helligkeitssensor.data.EvStatus status = (de.hausbus.proxy.helligkeitssensor.data.EvStatus) message.getData();
            newValue = status.getBrightness();
        }

        if (newValue != -1)
            setValueAndSendEvent("LUX", toHomematicLevel(newValue));
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
            de.hausbus.proxy.helligkeitssensor.data.Configuration config = helligkeitsSensor.getConfiguration();

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
                helligkeitsSensor.setConfiguration(config);
            }
        } catch (BusException e)
        {
            throw new RuntimeException("configuration failed");
        }
    }
}
