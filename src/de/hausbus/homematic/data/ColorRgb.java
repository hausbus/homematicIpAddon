package de.hausbus.homematic.data;

public class ColorRgb
{
    public static final int eMinValue = 0;
    public static final int eMidValue = 128;
    public static final int eMaxValue = 255;

    public int r = 0;
    public int g = 0;
    public int b = 0;

    public ColorRgb(ColorHsv hsv)
    {
        int v = convertToInternalRange(hsv.getValue());
        int s = convertToInternalRange(hsv.getSaturation());

        if (hsv.getSaturation() == 0)
        {
            r = v;
            g = v;
            b = v;
        } else
        {
            int base = ((eMaxValue - s) * v) >> 8;

            switch (hsv.getHue() / ColorHsv.eHueSection)
            {
            case 0:
                r = v;
                g = (((v - base) * hsv.getHue()) / ColorHsv.eHueSection) + base;
                b = base;
                break;

            case 1:
                r = (((v - base) * (ColorHsv.eHueSection - (hsv.getHue() % ColorHsv.eHueSection))) / ColorHsv.eHueSection) + base;
                g = v;
                b = base;
                break;

            case 2:
                r = base;
                g = v;
                b = (((v - base) * (hsv.getHue() % ColorHsv.eHueSection)) / ColorHsv.eHueSection) + base;
                break;

            case 3:
                r = base;
                g = (((v - base) * (ColorHsv.eHueSection - (hsv.getHue() % ColorHsv.eHueSection))) / ColorHsv.eHueSection) + base;
                b = v;
                break;

            case 4:
                r = (((v - base) * (hsv.getHue() % ColorHsv.eHueSection)) / ColorHsv.eHueSection) + base;
                g = base;
                b = v;
                break;

            case 5:
                r = v;
                g = base;
                b = (((v - base) * (ColorHsv.eHueSection - (hsv.getHue() % ColorHsv.eHueSection))) / ColorHsv.eHueSection) + base;
                break;
            }
        }
    }

    int convertToInternalRange(int value)
    {
        // need to round up
        return (value * eMaxValue + ColorHsv.eMidValue) / ColorHsv.eMaxValue;
    }

    public int getRed()
    {
        return r;
    }

    public int getGreen()
    {
        return g;
    }

    public int getBlue()
    {
        return b;
    }
}
