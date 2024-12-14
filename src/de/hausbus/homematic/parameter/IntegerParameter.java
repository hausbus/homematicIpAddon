package de.hausbus.homematic.parameter;

public class IntegerParameter extends AParameter<IntegerParameter>
{

    public IntegerParameter(int min, int max)
    {
        setType("INTEGER");
        setMin(min);
        setMax(max);
    }

}
