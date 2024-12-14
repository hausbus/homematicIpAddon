package de.hausbus.homematic.parameter;

public class BooleanParameter extends AParameter<BooleanParameter>
{
    public BooleanParameter()
    {
        setType("BOOL");
        setMin(false);
        setMax(true);
    }
}
