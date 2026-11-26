package com.reminimalism.particleslivewallpaper;

import java.util.ArrayList;
import java.util.Calendar;

public class DayNightColorManager implements ColorManager
{
    private float[] BGColor = new float[3];
    private float[] ParticleColors = new float[3 * 10];
    private int Counter = 100;
    private ArrayList<Integer> PointTimes = new ArrayList<>();
    private ArrayList<float[]> PointBGColors = new ArrayList<>();
    private ArrayList<float[]> PointParticleColors = new ArrayList<>();
    private int PointIndex = 0;

    /// MUST BE CALLED IN ORDER
    public void AddColorPoint(int hour24, int minute, int second, float[] BGColor, float[] _10ParticleColors)
    {
        if (_10ParticleColors.length < 10 * 3)
            return;
        PointTimes.add(hour24 * 60 * 60 + minute * 60 + second);
        PointBGColors.add(BGColor);
        PointParticleColors.add(_10ParticleColors);
        PrepareToUpdateNextTime();
    }

    public void ClearColorPoints()
    {
        PointTimes.clear();
        PointBGColors.clear();
        PointParticleColors.clear();
        PointIndex = 0;
        PrepareToUpdateNextTime();
    }

    public float[] GetBGColor()
    {
        UpdateColorsSometimes();
        return BGColor;
    }

    public float[] GetParticleColors()
    {
        UpdateColorsSometimes();
        return ParticleColors;
    }

    public void UpdateNow()
    {
        UpdateColors();
    }

    int COUNTER_LIMIT = 60;

    private void PrepareToUpdateNextTime()
    {
        Counter = COUNTER_LIMIT;
    }

    private void UpdateColorsSometimes()
    {
        if (Counter < COUNTER_LIMIT)
        {
            Counter++;
            return;
        }
        Counter = 0;
        UpdateColors();
    }

    private void UpdateColors()
    {
        if (PointTimes.size() == 0)
            return;
        Calendar calendar = Calendar.getInstance();
        int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int time = hour24 * 60 * 60 + minute * 60 + second;

        int i1, i2, t1, t2;

        while (true)
        {
            i1 = PointIndex % PointTimes.size();
            i2 = (PointIndex + 1) % PointTimes.size();
            t1 = PointTimes.get(i1);
            t2 = PointTimes.get(i2);
            if (t2 <= t1)
            {
                if (time <= t2 || t1 <= time)
                    break;
            }
            else
            {
                if (t1 <= time && time <= t2)
                    break;
            }
            PointIndex++;
            if (PointIndex >= PointTimes.size())
                PointIndex = PointIndex % PointTimes.size();
        }

        if (t2 <= t1)
            t2 = ConvertToNextPeriod(t2);
        if (time < t1)
            time = ConvertToNextPeriod(time);

        float t = (float)(time - t1) / (float)(t2 - t1);

        float[] c1 = PointParticleColors.get(i1);
        float[] c2 = PointParticleColors.get(i2);
        for (int i = 0; i < ParticleColors.length; i++)
            ParticleColors[i] = c1[i] + (c2[i] - c1[i]) * t;

        c1 = PointBGColors.get(i1);
        c2 = PointBGColors.get(i2);
        for (int i = 0; i < BGColor.length; i++)
            BGColor[i] = c1[i] + (c2[i] - c1[i]) * t;
    }

    private int ConvertToNextPeriod(int Time)
    {
        return Time + 24 * 60 * 60;
    }
}
