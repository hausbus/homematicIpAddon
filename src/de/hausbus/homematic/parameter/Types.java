package de.hausbus.homematic.parameter;

public class Types
{
    public static final String FLOAT = "FLOAT";
    public static final String INTEGER = "INTEGER";
    public static final String BOOL = "BOOL";
    public static final String ENUM = "ENUM";
    public static final String STRING = "STRING";
    /**
     * Datentyp des entsprechenden Parameters ist Boolean. Es wird beim Lesen immer FALSE zur�ckgegeben.
     * Bei einem Event ist der Parameter jedoch immer TRUE. Beim Schreiben auf den Parameter spielt der
     * geschriebene Wert keine Rolle. Der Typ ACTION wird verwendet, um Vorg�nge wie das Dr�cken einer
     * Fernbedienungstaste abzubilden. In diesem Fall wird beim Dr�cken der Taste ein Event generiert.
     * Umgekehrt wird beim Schreiben auf diesen Parameter ein Tastendruck simuliert.
     */
    public static final String ACTION = "ACTION";
}
