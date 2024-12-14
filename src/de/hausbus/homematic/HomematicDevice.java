package de.hausbus.homematic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.hausbus.com.BusDevice;
import de.hausbus.com.BusException;
import de.hausbus.com.BusMessage;
import de.hausbus.com.FirmwareUpdater;
import de.hausbus.com.HomeserverException;
import de.hausbus.com.IBusDevice;
import de.hausbus.com.IBusFeature;
import de.hausbus.com.IFirmwareUpdateListener;
import de.hausbus.com.ObjectId;
import de.hausbus.com.Templates;
import de.hausbus.com.Templates.FeatureEntry;
import de.hausbus.homematic.features.AnalogInputFeature;
import de.hausbus.homematic.features.BrightnessFeature;
import de.hausbus.homematic.features.DimmerFeature;
import de.hausbus.homematic.features.HumidityFeature;
import de.hausbus.homematic.features.LedFeature;
import de.hausbus.homematic.features.LogicalButtonFeature;
import de.hausbus.homematic.features.MaintenanceFeature;
import de.hausbus.homematic.features.RfidFeature;
import de.hausbus.homematic.features.RgbFeature;
import de.hausbus.homematic.features.RgbFeatureColor;
import de.hausbus.homematic.features.RolloFeature;
import de.hausbus.homematic.features.SchalterFeature;
import de.hausbus.homematic.features.SsrControlFeature;
import de.hausbus.homematic.features.TasterFeature;
import de.hausbus.homematic.features.TemperatureFeature;
import de.hausbus.homematic.parameter.IntegerParameter;
import de.hausbus.proxy.AnalogEingang;
import de.hausbus.proxy.Dimmer;
import de.hausbus.proxy.Feuchtesensor;
import de.hausbus.proxy.Helligkeitssensor;
import de.hausbus.proxy.Led;
import de.hausbus.proxy.LogicalButton;
import de.hausbus.proxy.RFIDReader;
import de.hausbus.proxy.RGBDimmer;
import de.hausbus.proxy.Rollladen;
import de.hausbus.proxy.Schalter;
import de.hausbus.proxy.Taster;
import de.hausbus.proxy.Temperatursensor;
import de.hausbus.proxy.controller.data.Configuration;
import de.hausbus.proxy.controller.data.ModuleId;

public class HomematicDevice extends AHomematicDevice
{
    private static final FirmwareUpdater firmwareUpdater = new FirmwareUpdater();
    private Map<String, AHomematicDevice> features;
    MaintenanceFeature maintenanceFeature = null;

    public boolean restarted = false;

    HomematicDevice(IBusDevice device, HomematicBridge bridge)
    {
        super(device.getName(), null, device, null, bridge, null, false, 1);
        firmwareUpdater.init();
    }

    @Override
    public void create()
    {
        createFeatures();
        super.create();
        addChildren(features.keySet());
    }

    protected void createFeatures()
    {
        features = new LinkedHashMap<>();
        maintenanceFeature = new MaintenanceFeature(device, this, bridge);
        features.put(AHomematicDevice.getHomematicAddress(deviceId, 0), maintenanceFeature);

        Configuration config = ((BusDevice) device).getConfigurationFromCacheOrCall();
        ModuleId moduleId = ((BusDevice) device).getModuleIdFromCacheOrCall();
        List<IBusFeature> allFeatures = device.getFeatures();
        List<IBusFeature> dones = new ArrayList<>();

        int index = 1;
        List<FeatureEntry> myFeatures = Templates.getInstance().getFeatures(moduleId.getFirmwareId(), config.getFCKE());
        if (myFeatures == null)
        {
            logger.severe("keine features " + moduleId.getFirmwareId() + ", " + config.getFCKE() + " in " + Templates.templateRootDir);
            System.exit(-1);
        } else
        {
            if (((BusDevice) device).specials.contains(BusDevice.ROLLOMODUL))
            {
                logger.finer("ergänze Rollos");
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("18,8,Rollo 8"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("18,7,Rollo 7"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("18,6,Rollo 6"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("18,5,Rollo 5"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("18,4,Rollo 4"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("18,3,Rollo 3"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("18,2,Rollo 2"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("18,1,Rollo 1"));
            } else if (((BusDevice) device).specials.contains(BusDevice.LEISTUNGSREGLER))
            {
                logger.finer("ergänze SSR Regler");
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,104,SSR Regler 16"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,103,SSR Regler 15"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,102,SSR Regler 14"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,101,SSR Regler 13"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,100,SSR Regler 12"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,99,SSR Regler 11"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,98,SSR Regler 10"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,97,SSR Regler 9"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,56,SSR Regler 8"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,55,SSR Regler 7"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,54,SSR Regler 6"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,53,SSR Regler 5"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,52,SSR Regler 4"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,51,SSR Regler 3"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,50,SSR Regler 2"));
                myFeatures.add(0, Templates.getInstance().new FeatureEntry("19,49,SSR Regler 1"));
            }

            logger.finer("features: " + myFeatures);
            for (FeatureEntry actFeature : myFeatures)
            {
                for (IBusFeature act : allFeatures)
                {
                    if (act instanceof RGBDimmer && device.getName().contains("PWM "))
                    {
                        logger.finer("filtering RGB channel for PWM module");
                        continue;
                    }

                    if (act instanceof Schalter && act.getName().toLowerCase().contains("rote modul"))
                    {
                        logger.finer("filtering Rote Modul LED");
                        continue;
                    }

                    ObjectId objectId = act.getObjectIdObj();
                    if (objectId.getClassId() == actFeature.getClassId() && objectId.getInstanceId() == actFeature.getInstanceId())
                    {
                        if (act instanceof Taster)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new TasterFeature((Taster) act, index, device, this, bridge));
                        else if (act instanceof Led)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new LedFeature((Led) act, index, device, this, bridge));
                        else if (act instanceof LogicalButton)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new LogicalButtonFeature((LogicalButton) act, index, device, this, bridge));
                        else if (act instanceof Dimmer)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new DimmerFeature((Dimmer) act, index, device, this, bridge));
                        else if (act instanceof RGBDimmer)
                        {
                            RgbFeature rgbFeature = new RgbFeature((RGBDimmer) act, index + 1, device, this, bridge);
                            RgbFeatureColor rgbColor = new RgbFeatureColor((RGBDimmer) act, index, device, this, bridge, rgbFeature);
                            rgbFeature.setColorFeature(rgbColor);

                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), rgbColor);
                            index++;

                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), rgbFeature);
                        } else if (act instanceof Schalter)
                        {
                            if (actFeature.name.contains("SSR"))
                                features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new SsrControlFeature((Schalter) act, index, device, this, bridge));
                            else
                                features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new SchalterFeature((Schalter) act, index, device, this, bridge));
                        } else if (act instanceof Rollladen)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new RolloFeature((Rollladen) act, index, device, this, bridge));
                        else if (act instanceof AnalogEingang)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new AnalogInputFeature((AnalogEingang) act, index, device, this, bridge));
                        else if (act instanceof RFIDReader)
                        {
                            int useIndex = (((index + 10) / 10) + 1) * 10;
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new RfidFeature((RFIDReader) act, useIndex, device, this, bridge));
                        } else if (act instanceof Feuchtesensor)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new HumidityFeature((Feuchtesensor) act, index, device, this, bridge));
                        else if (act instanceof Helligkeitssensor)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new BrightnessFeature((Helligkeitssensor) act, index, device, this, bridge));
                        else if (act instanceof Temperatursensor)
                            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new TemperatureFeature((Temperatursensor) act, index, device, this, bridge));
                        else
                        {
                            logger.warning("unknown feature " + act);
                            break;
                        }
                        dones.add(act);

                        index++;
                        break;
                    }
                }
            }
        }

        // Weitere Temperatursensoren ?
        for (Temperatursensor act : device.getFeatures(Temperatursensor.class, false))
        {
            if (dones.contains(act))
                continue;

            features.put(AHomematicDevice.getHomematicAddress(deviceId, index), new TemperatureFeature((Temperatursensor) act, index, device, this, bridge));
            index++;
        }
    }

    @Override
    protected void createMasterParamset()
    {
        addMasterParameter("DEVICE_ID", address, new IntegerParameter(1620000000, 1629999999));
    }

    @Override
    protected void createValuesParamset()
    {
    }

    public Map<String, Object> getDeviceDescription(String address)
    {
        if (this.address.equals(address))
            return getDeviceDescription();

        AHomematicDevice child = features.get(address);
        if (child != null)
            return child.getDeviceDescription();
        logger.warning("feature " + address + " not found");

        return new HashMap<>();
    }

    public List<Map<String, Object>> getAllDeviceDescriptons()
    {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(getDeviceDescription());
        for (AHomematicDevice act : features.values())
        {
            result.add(act.getDeviceDescription());
        }

        return result;
    }

    public HashMap<String, Object> getParamset(String address, String type)
    {
        if (this.address.equals(address))
            return getParamset(type);

        AHomematicDevice child = features.get(address);
        if (child != null)
            return child.getParamset(type);
        logger.warning("feature " + address + " not found");

        return new HashMap<>();
    }

    public HashMap<String, Object> getParamsetDescription(String address, String type)
    {
        if (this.address.equals(address))
            return getParamsetDescription(type);

        if (address.equals("1620002378:1"))
            System.nanoTime();

        AHomematicDevice child = features.get(address);
        if (child != null)
            return child.getParamsetDescription(type);
        logger.warning("feature " + address + " not found");

        return new HashMap<>();
    }

    public String getParamsetId(String address, String type)
    {
        if (this.address.equals(address))
            return getParamsetId(type);

        AHomematicDevice child = features.get(address);
        if (child != null)
            return child.getParamsetId(type);
        logger.warning("feature " + address + " not found");

        return "unknown 2";
    }

    public void putParamset(String address, String paramset_key, HashMap<String, Object> params)
    {
        if (this.address.equals(address))
        {
            setParamset(paramset_key, params);
            configureParams(params);
        } else
        {
            AHomematicDevice child = features.get(address);
            if (child != null)
            {
                child.setParamset(paramset_key, params);
                child.configureParams(params);
            } else
                logger.warning("feature " + address + " not found");
        }
    }

    @Override
    protected void configureParams(HashMap<String, Object> params)
    {
        // DeviceId neu setzen
        Object ownAddress = params.get("DEVICE_ID");
        if (ownAddress instanceof Integer)
        {
            int newAddress = HomematicDevice.getHausBusAddress((Integer) ownAddress);

            try
            {
                de.hausbus.proxy.controller.data.Configuration config = device.getConfiguration();
                config.setDeviceId(newAddress);
                device.setConfiguration(config);
                device.reset();

                new Thread(() -> {
                    sleep(1000);
                    bridge.deleteDevice(address);
                }).start();
            } catch (BusException e)
            {
            }
        }
    }

    public void setValue(String address, String key, Object value)
    {
        if (this.address.equals(address))
            setValue(key, value);
        else
        {
            AHomematicDevice child = features.get(address);
            if (child != null)
                child.setValue(key, value);
            else
                logger.warning("feature " + address + " not found");
        }
    }

    public Object getValue(String address, String key)
    {
        if (this.address.equals(address))
            return getValue(key);
        else
        {
            AHomematicDevice child = features.get(address);
            if (child != null)
                return child.getValue(key);

            logger.warning("feature " + address + " not found");
            return null;
        }
    }

    @Override
    protected void busMessageReceived(BusMessage message)
    {
        if (message.getSender() == device)
        {

        } else
        {
            for (AHomematicDevice act : features.values())
            {
                if (message.getSender() == act.feature)
                {
                    act.busMessageReceived(message);
                    // Hier kein Break, weil es SSR und Schalter gleichzeitig gibt
                }
            }
        }
    }

    public boolean updateFirmware()
    {
        try
        {
            new FirmwareUpdater().updateFirmware(device, new IFirmwareUpdateListener()
            {

                @Override
                public void updateProgress(int percent)
                {
                    logger.finer("update progress: " + percent + " %");
                }
            });
            return true;
        } catch (HomeserverException | BusException | IOException e)
        {
            return false;
        }
    }

    @Override
    protected void initReference()
    {
    }

    @Override
    protected void initValues() throws BusException
    {
    }

    @Override
    protected void setValue(String key, Object value)
    {
    }
}
