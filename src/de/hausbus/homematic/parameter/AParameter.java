package de.hausbus.homematic.parameter;

import java.util.HashMap;

public abstract class AParameter<T>
{
    final HashMap<String, Object> map = new HashMap<>();

    public AParameter()
    {
        map.put("UNIT", "");
        map.put("OPERATIONS", Operations.READ_WRITE);
        map.put("FLAGS", Flags.VISIBLE);
    }

    public HashMap<String, Object> get()
    {
        return map;
    }

    public AParameter<T> setType(String type)
    {
        map.put("TYPE", type);
        return this;
    }

    public AParameter<T> setId(String id)
    {
        map.put("ID", id);
        return this;
    }

    public AParameter<T> setTabOrder(int tabOrder)
    {
        map.put("TAB_ORDER", tabOrder);
        return this;
    }

    public AParameter<T> setMin(Object value)
    {
        map.put("MIN", value);
        return this;
    }

    public AParameter<T> setMax(Object value)
    {
        map.put("MAX", value);
        return this;
    }

    public AParameter<T> setUnit(String value)
    {
        map.put("UNIT", value);
        return this;
    }

    public AParameter<T> setOperations(int value)
    {
        map.put("OPERATIONS", value);
        return this;
    }

    public AParameter<T> setControl(String control)
    {
        map.put("CONTROL", control);
        return this;
    }

    public AParameter<T> setFlags(int value)
    {
        map.put("FLAGS", value);
        return this;
    }

    public AParameter<T> setDefault(Object value)
    {
        map.put("DEFAULT", value);
        return this;
    }

    public AParameter<T> setValueList(Object... values)
    {
        map.put("VALUE_LIST", values);
        return this;
    }
}
