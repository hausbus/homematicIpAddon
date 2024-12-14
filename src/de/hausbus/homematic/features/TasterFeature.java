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
import de.hausbus.homematic.parameter.Direction;
import de.hausbus.homematic.parameter.EventParameter;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.proxy.Schalter;
import de.hausbus.proxy.Taster;
import de.hausbus.proxy.taster.data.Configuration;
import de.hausbus.proxy.taster.data.EvClicked;
import de.hausbus.proxy.taster.data.EvCovered;
import de.hausbus.proxy.taster.data.EvDoubleClick;
import de.hausbus.proxy.taster.data.EvFree;
import de.hausbus.proxy.taster.data.EvHoldEnd;
import de.hausbus.proxy.taster.data.EvHoldStart;
import de.hausbus.proxy.taster.data.Status;
import de.hausbus.proxy.taster.params.EState;
import de.hausbus.proxy.taster.params.MEventMask;

public class TasterFeature extends AHomematicDevice
{
    private volatile Taster taster;

    public TasterFeature(Taster feature, int index, IBusDevice busDevice, AHomematicDevice parent, HomematicBridge bridge)
    {
        super("KEY", index, busDevice, parent, bridge, feature, true, 1);
    }

    // Hier alle Konfigurationsparameter anlegen mit Default
    @Override
    protected void createMasterParamset()
    {
        deviceDescription.put("PARAMSETS", new Object[] { "MASTER", "VALUES","LINK" });
        deviceDescription.put("DIRECTION", Direction.DIRECTION_SENDER);
        deviceDescription.put("LINK_SOURCE_ROLES", "SWITCH");
        deviceDescription.put("LINK_TARGET_ROLES", "");

        addMasterParameter("LONG_PRESS_TIME", 1000, new IntegerParameter(400, 5000).setUnit("ms"));
        addMasterParameter("DOUBLE_CLICK_TIME", 0, new IntegerParameter(0, 2000).setUnit("ms"));
    }

    // Hier alle Funktionen und States anlegen
    @Override
    protected void createValuesParamset()
    {
        addValuesParameter("PRESS_SHORT", false, new ActionParameter("BUTTON.SHORT"));
        addValuesParameter("PRESS_LONG", false, new ActionParameter("BUTTON.LONG"));
        addValuesParameter("PRESS_LONG_RELEASE", false, new ActionParameter("NONE"));
        addValuesParameter("DOUBLE_CLICK", false, new ActionParameter("NONE"));
        addValuesParameter("SENSOR", true, new EventParameter("DOOR_SENSOR.STATE"));
    }

    @Override
    protected void initReference()
    {
        taster = (Taster) feature;
    }

    // Hier nach dem Start aktuelle Werte vom Gerät holen und Parameter und Values füllen
    @Override
    protected void initValues() throws BusException
    {
        Configuration config = taster.getConfiguration();
        masterParamset.put("LONG_PRESS_TIME", config.getHoldTimeout() * 10);

        boolean doubleClickConfigured = config.getEventMask().isNotifyOnDoubleClicked();
        int doubleClickTimeout = config.getWaitForDoubleClickTimeout() * 10;
        if (!doubleClickConfigured)
            doubleClickTimeout = 0;
        masterParamset.put("DOUBLE_CLICK_TIME", doubleClickTimeout);

        /*
         * covered -> state holdstart -> longPress clicked -> shortPress free -> state
         */
        MEventMask newMask = new MEventMask().setEnableFeedBack(true).setNotifyOnCovered(true).setNotifyOnFree(true).setNotifyOnClicked(true).setNotifyOnStartHold(true).setNotifyOnEndHold(true).setNotifyOnDoubleClicked(doubleClickConfigured);
        if (config.getEventMask().getValue() != newMask.getValue())
        {
            logger.finer(address + ": configuring mask " + newMask);
            config.setEventMask(newMask);
            taster.setConfiguration(config);
        }

        boolean actState = true;
        Status actStatus = taster.getStatus();
        if (actStatus.getState() == EState.PRESSED)
            actState = false;
        // logger.finer(address + ": initial state = " + actState);
        valuesParamset.put("SENSOR", actState);
    }

    // Hier kommt ein Aufruf, der ein Value umsetzt
    @Override
    protected void setValue(String key, Object value)
    {
        super.setValue(key, value);
    }

    // Hier kommt ein Konfigurationsaufruf
    @Override
    public void configureParams(HashMap<String, Object> params)
    {
        try
        {
            Configuration config = taster.getConfiguration();

            Object longPressTime = params.get("LONG_PRESS_TIME");
            if (longPressTime != null)
            {
                int deviceHoldTime = ((int) longPressTime) / 10;
                config.setHoldTimeout(deviceHoldTime);
                masterParamset.put("LONG_PRESS_TIME", longPressTime);
            }

            Object doubleClickTime = params.get("DOUBLE_CLICK_TIME");
            if (doubleClickTime != null)
            {
                int doubleClickTimeInt = ((int) doubleClickTime) / 10;
                boolean doubleClickActivated = doubleClickTimeInt > 0;

                MEventMask mask = config.getEventMask();
                mask.setNotifyOnDoubleClicked((boolean) doubleClickActivated);
                config.setEventMask(mask);
                if (doubleClickActivated)
                    config.setWaitForDoubleClickTimeout(doubleClickTimeInt);

                masterParamset.put("DOUBLE_CLICK_TIME", doubleClickTime);
            }

            logger.finer(address + " configuring " + config);
            taster.setConfiguration(config);
        } catch (BusException e)
        {
            throw new RuntimeException("configuration failed");
        }
    }

    // Hier Events verschicken
    @Override
    protected void busMessageReceived(BusMessage message)
    {
        System.nanoTime();
        if (message.getData() instanceof EvClicked)
            sendEvent("PRESS_SHORT", true);
        else if (message.getData() instanceof EvHoldStart)
            sendEvent("PRESS_LONG", true);
        else if (message.getData() instanceof EvHoldEnd)
            sendEvent("PRESS_LONG_RELEASE", true);
        else if (message.getData() instanceof EvDoubleClick)
            sendEvent("DOUBLE_CLICK", true);
        else if (message.getData() instanceof EvCovered)
            setValueAndSendEvent("SENSOR", false);
        else if (message.getData() instanceof EvFree)
            setValueAndSendEvent("SENSOR", true);
    }
}
