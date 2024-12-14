package de.hausbus.homematic.parameter;

public class EnumParameter extends AParameter<EnumParameter>
{
    public EnumParameter(Object... values)
    {
        setType("ENUM");
        setMin(0);
        setMax(values.length);
        setValueList(values);
    }

}
