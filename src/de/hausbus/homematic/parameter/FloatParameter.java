package de.hausbus.homematic.parameter;

public class FloatParameter extends AParameter<IntegerParameter>
{

    public FloatParameter(double min, double max)
    {
        setType("FLOAT");
        setMin(min);
        setMax(max);
    }

}
