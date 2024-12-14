package de.hausbus.homematic.features;

import java.util.HashMap;

import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.IBusDevice;
import de.hausbus.homematic.AHomematicDevice;
import de.hausbus.homematic.HomematicBridge;
import de.hausbus.homematic.parameter.ActionParameter;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.proxy.RFIDReader;
import de.hausbus.proxy.RGBDimmer;

public class RgbFeatureColor extends AHomematicDevice
{
    private final RgbFeature rgbFeature;

    public RgbFeatureColor(RGBDimmer feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge, RgbFeature rgbFeature)
    {
        super("RGBW_COLOR", index, busDevice, parent, bridge, feature, true, 100);
        this.rgbFeature = rgbFeature;
        createMasterParamsetInternal();
        createValuesParamsetInternal();
    }

    @Override
    protected void createMasterParamset()
    {
    }

    protected void createMasterParamsetInternal()
    {
        addMasterParameter("WHITE_ADJUSTMENT_VALUE_RED", rgbFeature.getFromPersistence("WHITE_ADJUSTMENT_VALUE_RED", 0), new IntegerParameter(-100, 100));
        addMasterParameter("WHITE_ADJUSTMENT_VALUE_GREEN", rgbFeature.getFromPersistence("WHITE_ADJUSTMENT_VALUE_GREEN", 0), new IntegerParameter(-100, 100));
        addMasterParameter("WHITE_ADJUSTMENT_VALUE_BLUE", rgbFeature.getFromPersistence("WHITE_ADJUSTMENT_VALUE_BLUE", 0), new IntegerParameter(-100, 100));
        addMasterParameter("COLOR_UP_DOWN_DISTANCE", rgbFeature.getFromPersistence("COLOR_UP_DOWN_DISTANCE", 40), new IntegerParameter(1, 100));

    }

    @Override
    protected void createValuesParamset()
    {

    }

    protected void createValuesParamsetInternal()
    {
        addValuesParameter("COLOR", rgbFeature.getFromPersistence("COLOR", 0), new IntegerParameter(0, 255).setControl("RGBW_COLOR.COLOR"));
        addValuesParameter("COLOR_UP", true, new ActionParameter("NONE"));
        addValuesParameter("COLOR_DOWN", true, new ActionParameter("NONE"));
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
    protected void busMessageReceived(BusMessage message)
    {
    }

    @Override
    protected void setValue(String key, Object value)
    {
        super.setValue(key, value);

        if (key.equals("COLOR") && value instanceof Integer && rgbFeature != null)
            rgbFeature.setValue(key, value);
        else if (key.equals("COLOR_UP"))
            rgbFeature.colorStep(true);
        else if (key.equals("COLOR_DOWN"))
            rgbFeature.colorStep(false);
    }

    @Override
    public void configureParams(HashMap<String, Object> params)
    {
        Integer redAdjustmentValue = (Integer) params.get("WHITE_ADJUSTMENT_VALUE_RED");
        Integer greenAdjustmentValue = (Integer) params.get("WHITE_ADJUSTMENT_VALUE_GREEN");
        Integer blueAdjustmentValue = (Integer) params.get("WHITE_ADJUSTMENT_VALUE_BLUE");
        if (redAdjustmentValue != null && greenAdjustmentValue != null && blueAdjustmentValue != null)
        {
            masterParamset.put("WHITE_ADJUSTMENT_VALUE_RED", redAdjustmentValue);
            rgbFeature.persistence.put("WHITE_ADJUSTMENT_VALUE_RED", redAdjustmentValue);

            masterParamset.put("WHITE_ADJUSTMENT_VALUE_GREEN", greenAdjustmentValue);
            rgbFeature.persistence.put("WHITE_ADJUSTMENT_VALUE_GREEN", greenAdjustmentValue);

            masterParamset.put("WHITE_ADJUSTMENT_VALUE_BLUE", blueAdjustmentValue);
            rgbFeature.persistence.put("WHITE_ADJUSTMENT_VALUE_BLUE", blueAdjustmentValue);
            rgbFeature.savePersistence();
            rgbFeature.setColorAndLevel();
        }

        Integer colorUpDistance = (Integer) params.get("COLOR_UP_DOWN_DISTANCE");
        if (colorUpDistance != null)
        {
            masterParamset.put("COLOR_UP_DOWN_DISTANCE", colorUpDistance);
            rgbFeature.persistence.put("COLOR_UP_DOWN_DISTANCE", colorUpDistance);
        }

        Integer colorFlowSpeed = (Integer) params.get("COLOR_FLOW_SPEED");
        if (colorFlowSpeed != null)
        {
            masterParamset.put("COLOR_FLOW_SPEED", colorFlowSpeed);
            rgbFeature.persistence.put("COLOR_FLOW_SPEED", colorFlowSpeed);
        }
    }
}
