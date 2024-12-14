package de.hausbus.homematic.parameter;

public class ActionParameter extends AParameter<ActionParameter>
{
    public ActionParameter(String control)
    {
        setType("ACTION");
        setMin(false);
        setMax(true);
        setOperations(Operations.WRITE_EVENT);
        setControl(control);
    }
}
