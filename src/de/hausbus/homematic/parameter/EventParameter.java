package de.hausbus.homematic.parameter;

public class EventParameter extends AParameter<EventParameter>
{
    public EventParameter(String control)
    {
        setType("BOOL");
        setOperations(Operations.READ_EVENT);
        map.put("CONTROL", control);
    }
}
