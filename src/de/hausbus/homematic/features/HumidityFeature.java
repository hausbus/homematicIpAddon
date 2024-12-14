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
import de.hausbus.proxy.Dimmer;
import de.hausbus.proxy.Feuchtesensor;

public class HumidityFeature extends AHomematicDevice
{
    private volatile Feuchtesensor feuchtesensor;

    public HumidityFeature(Feuchtesensor feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("HUMIDITY", index, busDevice, parent, bridge, feature, false, 100);
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
        addValuesParameter("HUMIDITY", 0.0d, new FloatParameter(0d, 99d).setUnit("%").setOperations(Operations.READ_EVENT));
    }

    @Override
    protected void initReference()
    {
        feuchtesensor = (Feuchtesensor) feature;
    }

    @Override
    protected void initValues() throws BusException
    {
        de.hausbus.proxy.feuchtesensor.data.Configuration config = feuchtesensor.getConfiguration();
        if (config.getReportTimeBase() != 10)
        {
            config.setReportTimeBase(10);
            feuchtesensor.setConfiguration(config);
        }

        masterParamset.put("SEND_INTERVAL", config.getMaxReportTime() * config.getReportTimeBase());
        masterParamset.put("SEND_DELTA", (double) (config.getHysteresis() * 0.1));
        masterParamset.put("CALIBRATION", (double) (config.getCalibration() * 0.1));

        de.hausbus.proxy.feuchtesensor.data.Status actStatus = feuchtesensor.getStatus();
        valuesParamset.put("HUMIDITY", toHomematicLevel(actStatus.getRelativeHumidity() * 100 + actStatus.getCentiHumidity() / 100));
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        int newValue = -1;

        if (message.getData() instanceof de.hausbus.proxy.feuchtesensor.data.EvStatus)
        {
            de.hausbus.proxy.feuchtesensor.data.EvStatus status = (de.hausbus.proxy.feuchtesensor.data.EvStatus) message.getData();
            newValue = status.getRelativeHumidity() * 100 + status.getCentiHumidity();
        }

        if (newValue != -1)
            setValueAndSendEvent("HUMIDITY", toHomematicLevel(newValue));
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
            de.hausbus.proxy.feuchtesensor.data.Configuration config = feuchtesensor.getConfiguration();

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
                feuchtesensor.setConfiguration(config);
            }
        } catch (BusException e)
        {
            throw new RuntimeException("configuration failed");
        }
    }
}
