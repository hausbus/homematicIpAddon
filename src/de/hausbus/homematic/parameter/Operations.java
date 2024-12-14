package de.hausbus.homematic.parameter;

public class Operations
{
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int EVENT = 4;
    public static final int READ_WRITE = READ + WRITE;
    public static final int READ_EVENT = READ + EVENT;
    public static final int WRITE_EVENT = WRITE + EVENT;
    public static final int READ_WRITE_EVENT = READ + WRITE + EVENT;

}
