package com.reminimalism.particleslivewallpaper;

public class ConstantColorManager implements ColorManager
{
    public ConstantColorManager(float[] BGColor, float[] _10ParticleColors)
    {
        this.BGColor = BGColor;
        ParticleColors = _10ParticleColors;
    }

    public float[] GetBGColor()
    {
        return BGColor;
    }

    public float[] GetParticleColors()
    {
        return ParticleColors;
    }

    public void UpdateNow() {}

    private final float[] BGColor;
    private final float[] ParticleColors;
}
