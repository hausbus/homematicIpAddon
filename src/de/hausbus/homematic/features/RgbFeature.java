package de.hausbus.homematic.features;

import java.util.HashMap;
import java.util.logging.Level;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.data.ColorHsv;
import de.hausbus.homematic.data.ColorRgb;
import de.hausbus.homematic.parameter.ActionParameter;
import de.hausbus.homematic.parameter.BooleanParameter;
import de.hausbus.homematic.parameter.FloatParameter;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.homematic.parameter.Operations;
import de.hausbus.proxy.RGBDimmer;

public class RgbFeature extends AHomematicDevice
{
    private RgbFeatureColor colorFeature;
    private RGBDimmer rgbDimmer;
    private int lastColor = 0;
    private double lastLevel = 0;
    private int oldColor = 100;
    private double oldLevel = 100;

    public RgbFeature(RGBDimmer feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
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
        addValuesParameter("LEVEL", getFromPersistence("LEVEL", 0d), new FloatParameter(0d, 1.0d).setControl("DIMMER.LEVEL").setUnit("100%"));
        addValuesParameter("RAMP_TIME", 12, new IntegerParameter(1, 250).setControl("NONE").setUnit("50ms"));
        addValuesParameter("ON_TIME", 0, new IntegerParameter(0, 250).setControl("NONE").setUnit("s"));
        addValuesParameter("OLD_LEVEL", true, new ActionParameter("NONE").setOperations(Operations.WRITE));
        addValuesParameter("WORKING", false, new BooleanParameter());
    }

    public void setColorFeature(RgbFeatureColor colorFeature)
    {
        this.colorFeature = colorFeature;
        initValuesInternal();
    }

    @Override
    protected void initReference()
    {
        rgbDimmer = (RGBDimmer) feature;
    }

    protected void initValues()  throws BusException
    {
    }

    protected void initValuesInternal()
    {
        lastLevel = getFromPersistence("LEVEL", 0d);
        lastColor = getFromPersistence("COLOR", 0);

        try
        {
            de.hausbus.proxy.rGBDimmer.data.Configuration config = rgbDimmer.getConfiguration();
            masterParamset.put("RAMP_TIME", config.getFadingTime());

            setColorAndLevel();
        } catch (BusException e)
        {
            logger.log(Level.SEVERE, address + ": getStatus failed", e);
        }
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        if (message.getData() instanceof de.hausbus.proxy.rGBDimmer.data.EvOn)
            setValueAndSendEvent("WORKING", false);
        else if (message.getData() instanceof de.hausbus.proxy.rGBDimmer.data.EvOff)
            setValueAndSendEvent("WORKING", false);
    }

    void setColorAndLevel()
    {
        ColorHsv hsv = new ColorHsv(0, 0, 0);
        hsv.setHue(lastColor * 18 / 10);
        hsv.setSaturation(lastColor == 200 ? ColorHsv.eMinValue : ColorHsv.eMaxValue);
        hsv.setValue((int) (lastLevel * 200));

        ColorRgb rgb = new ColorRgb(hsv);
        int red = (int) ((rgb.getRed() / 255f) * 100);
        int green = (int) ((rgb.getGreen() / 255f) * 100);
        int blue = (int) ((rgb.getBlue() / 255f) * 100);

        if (red > 0 || green > 0 || blue > 0)
        {
            oldColor = lastColor;
            oldLevel = lastLevel;
        }

        persistence.put("COLOR", lastColor);
        persistence.put("LEVEL", lastLevel);
        savePersistence();

        /*
         * JFrame frame = new JFrame(); frame.getContentPane().setBackground(new Color(red, green, blue));
         * frame.setSize(400, 400); frame.setVisible(true);
         * frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
         */

        red = adjustValue(red, "WHITE_ADJUSTMENT_VALUE_RED");
        green = adjustValue(green, "WHITE_ADJUSTMENT_VALUE_GREEN");
        blue = adjustValue(blue, "WHITE_ADJUSTMENT_VALUE_BLUE");

        try
        {
            setValueAndSendEvent("WORKING", true);
            rgbDimmer.setColor(red, green, blue, getOnTime());
        } catch (BusException e)
        {
        }

        sendEvent("LEVEL", lastLevel);
        if (colorFeature != null)
            colorFeature.setValueAndSendEvent("COLOR", lastColor);
    }

    private int adjustValue(int value, String param)
    {
        value += getFromPersistence(param, 0);

        if (value > 255)
            value = 255;
        if (value < 0)
            value = 0;

        return value;
    }

    @Override
    protected void setValue(String key, Object value)
    {
        super.setValue(key, value);

        if (key.equals("COLOR") && value instanceof Integer)
        {
            lastColor = (int) value;
            setColorAndLevel();
        } else if (key.equals("LEVEL") && value instanceof Double)
        {
            lastLevel = (double) value;
            setColorAndLevel();
        } else if (key.equals("OLD_LEVEL"))
        {
            lastColor = oldColor;
            lastLevel = oldLevel;
            setColorAndLevel();
        } else if (key.equals("RAMP_TIME") && value instanceof Integer)
            configureRampTime(value);
        else if (key.equals("ON_TIME") && value instanceof Integer)
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
            de.hausbus.proxy.rGBDimmer.data.Configuration config = rgbDimmer.getConfiguration();
            config.setFadingTime(rampTime);
            rgbDimmer.setConfiguration(config);
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

    public void colorStep(boolean up)
    {
        int distance = getFromPersistence("COLOR_UP_DOWN_DISTANCE", 10);

        if (up)
            lastColor += distance;
        else
            lastColor -= distance;

        if (lastColor > 200)
            lastColor -= 200;
        if (lastColor < 0)
            lastColor = 200 - lastColor;
        setColorAndLevel();
    }
}
