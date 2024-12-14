package de.hausbus.homematic.data;

public class ColorHsv
{
    public static final int eMaxHue = 360;
    public static final int eMaxSections = 6;
    public static final int eHueSection = eMaxHue / eMaxSections;
    public static final int eHueSectionStartRed = 0;
    public static final int eHueSectionStartGreen = 120;
    public static final int eHueSectionStartBlue = 240;
    public static final int eMinValue = 0;
    public static final int eMaxValue = 200;
    public static final int eMidValue = eMaxValue / 2;

    private int h;
    private int s;
    private int v;

    public ColorHsv(int hue, int saturation, int value)
    {
        this.h = hue;
        this.s = saturation;
        this.v = value;
        checkValueRange();
    }

    public ColorHsv(ColorRgb rgb)
    {
        int rgbMin = rgb.getRed() < rgb.getGreen() ? (rgb.getRed() < rgb.getBlue() ? rgb.getRed() : rgb.getBlue()) : (rgb.getGreen() < rgb.getBlue() ? rgb.getGreen() : rgb.getBlue());
        int rgbMax = rgb.getRed() > rgb.getGreen() ? (rgb.getRed() > rgb.getBlue() ? rgb.getRed() : rgb.getBlue()) : (rgb.getGreen() > rgb.getBlue() ? rgb.getGreen() : rgb.getBlue());
        v = rgbMax;

        if (v == eMinValue)
        {
            h = eMinValue;
            s = eMinValue;
        } else
        {
            s = ColorRgb.eMaxValue * (rgbMax - rgbMin) / v;

            if (s == eMinValue)
            {
                h = eMinValue;
            } else
            {
                if (rgbMax == rgb.getRed())
                {
                    h = eHueSectionStartRed + eHueSection * (rgb.getGreen() - rgb.getBlue()) / (rgbMax - rgbMin);
                } else if (rgbMax == rgb.getGreen())
                {
                    h = eHueSectionStartGreen + eHueSection * (rgb.getBlue() - rgb.getRed()) / (rgbMax - rgbMin);
                } else
                {
                    h = eHueSectionStartBlue + eHueSection * (rgb.getRed() - rgb.getGreen()) / (rgbMax - rgbMin);
                }

                if (h > eMaxHue)
                {
                    h += eMaxHue;
                }
            }

            v = convertToInternalRange(v);
            s = convertToInternalRange(s);
        }
    }

    int convertToInternalRange(int value)
    {
        return (value * eMaxValue) / ColorRgb.eMaxValue;
    }

    void checkValueRange()
    {
        while (h >= eMaxHue)
            h -= eMaxHue;

        if (s > eMaxValue)
            s = eMaxValue;

        if (v > eMaxValue)
            v = eMaxValue;
    }

    public int getValue()
    {
        return v;
    }

    public int getSaturation()
    {
        return s;
    }

    public int getHue()
    {
        return h;
    }

    public void setHue(int h)
    {
        this.h = h;
    }

    public void setSaturation(int s)
    {
        this.s = s;
    }

    public void setValue(int v)
    {
        this.v = v;
    }
}
