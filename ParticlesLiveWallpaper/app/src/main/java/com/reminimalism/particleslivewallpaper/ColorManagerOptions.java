package com.reminimalism.particleslivewallpaper;

import android.content.SharedPreferences;

public class ColorManagerOptions
{
    public static ColorManager Get(SharedPreferences preferences)
    {
        if (preferences.getBoolean("custom_colors_enabled", false))
        {
            return ColorManagerOptions.Custom(
                    preferences.getString("custom_day_particles", "white"),
                    preferences.getString("custom_day_bg", "sky_blue"),
                    (float)preferences.getInt("custom_day_particles_shuffle", 75)/100f,
                    (float)preferences.getInt("custom_day_brightness", 90)/100f,
                    preferences.getString("custom_dawn_particles", "white"),
                    preferences.getString("custom_dawn_bg", "orange"),
                    (float)preferences.getInt("custom_dawn_particles_shuffle", 50)/100f,
                    (float)preferences.getInt("custom_dawn_brightness", 75)/100f,
                    preferences.getString("custom_night_particles", "salmon_pink"),
                    preferences.getString("custom_night_bg", "blue"),
                    (float)preferences.getInt("custom_night_particles_shuffle", 50)/100f,
                    (float)preferences.getInt("custom_night_brightness", 20)/100f
            );
        }
        else
        {
            return ColorManagerOptions.GetByName(
                    preferences.getString("colors", "day_night_sky_blue")
            );
        }
    }
    public static ColorManager GetByName(String name)
    {
        if (name.equals("day_night_sky_blue"))
            return DayNightSkyBlue();
        if (name.equals("day_night_green"))
            return DayNightGreen();
        if (name.equals("day_night_pink"))
            return DayNightPink();
        if (name.equals("grayscale"))
            return Grayscale();
        if (name.equals("night"))
            return NightSkyBlue();
        return Default();
    }

    public static DayNightColorManager Custom(
            String day_particles_name,
            String day_bg_name,
            float day_particles_shuffle,
            float day_brightness,
            String dawn_particles_name,
            String dawn_bg_name,
            float dawn_particles_shuffle,
            float dawn_brightness,
            String night_particles_name,
            String night_bg_name,
            float night_particles_shuffle,
            float night_brightness
    )
    {
        DayNightColorManager color_manager = new DayNightColorManager();

        float[] custom_day_particles = GenerateParticlesColor(
                GetColorFromName(day_particles_name),
                LerpFloatVector(max_particle_range, min_particle_range, day_brightness),
                day_particles_shuffle
        );
        float[] custom_day_bg = GenerateBGColor(
                GetColorFromName(day_bg_name),
                LerpFloatVector(min_bg_range, max_bg_range, day_brightness)
        );
        float[] custom_dawn_particles = GenerateParticlesColor(
                GetColorFromName(dawn_particles_name),
                LerpFloatVector(max_particle_range, min_particle_range, dawn_brightness),
                dawn_particles_shuffle
        );
        float[] custom_dawn_bg = GenerateBGColor(
                GetColorFromName(dawn_bg_name),
                LerpFloatVector(min_bg_range, max_bg_range, dawn_brightness)
        );
        float[] custom_semi_dawn_bg = LerpFloatVector(
                custom_dawn_bg,
                custom_dawn_bg,
                0.5f
        );
        float[] custom_night_particles = GenerateParticlesColor(
                GetColorFromName(night_particles_name),
                LerpFloatVector(max_particle_range, min_particle_range, night_brightness),
                night_particles_shuffle
        );
        float[] custom_night_bg = GenerateBGColor(
                GetColorFromName(night_bg_name),
                LerpFloatVector(min_bg_range, max_bg_range, night_brightness)
        );
        float[] custom_midnight_particles = LerpFloatVector(
                custom_night_particles,
                black_bg,
                0.25f
        );
        float[] custom_midnight_bg = LerpFloatVector(
                custom_night_bg,
                black_bg,
                0.25f
        );

        color_manager.AddColorPoint(0, 0, 0, custom_midnight_bg, custom_night_particles);
        color_manager.AddColorPoint(3, 0, 0, custom_midnight_bg, custom_midnight_particles);
        color_manager.AddColorPoint(6, 0, 0, custom_midnight_bg, custom_midnight_particles);
        color_manager.AddColorPoint(6, 30, 0, custom_dawn_bg, custom_dawn_particles);
        color_manager.AddColorPoint(7, 0, 0, custom_semi_dawn_bg, custom_day_particles);
        color_manager.AddColorPoint(9, 0, 0, custom_day_bg, custom_day_particles);
        color_manager.AddColorPoint(16, 0, 0, custom_day_bg, custom_day_particles);
        color_manager.AddColorPoint(18, 0, 0, custom_semi_dawn_bg, custom_dawn_particles);
        color_manager.AddColorPoint(18, 30, 0, custom_dawn_bg, custom_dawn_particles);
        color_manager.AddColorPoint(19, 0, 0, custom_night_bg, custom_night_particles);

        return color_manager;
    }

    public static DayNightColorManager Default()
    {
        return DayNightSkyBlue();
    }

    public static DayNightColorManager DayNightSkyBlue()
    {
        return DayNight(sky_blue_bg, dark_sky_blue_bg);
    }

    public static DayNightColorManager DayNightGreen()
    {
        return DayNight(bright_teal_bg, dark_teal_bg);
    }

    public static DayNightColorManager DayNightPink()
    {
        return DayNight(bright_pink_bg, dark_pink_bg);
    }

    public static ConstantColorManager Grayscale()
    {
        return new ConstantColorManager(black_bg, grayscale_particles);
    }

    public static DayNightColorManager DayNight(float[] day_bg, float[] dark_bg)
    {
        DayNightColorManager color_manager = new DayNightColorManager();

        color_manager.AddColorPoint(0, 0, 0, black_bg, night_particles);
        color_manager.AddColorPoint(3, 0, 0, black_bg, midnight_particles);
        color_manager.AddColorPoint(6, 0, 0, black_bg, midnight_particles);
        color_manager.AddColorPoint(6, 30, 0, dawn_bg, hybrid_particles);
        color_manager.AddColorPoint(7, 0, 0, semi_dawn_bg, day_particles);
        color_manager.AddColorPoint(9, 0, 0, day_bg, day_particles);
        color_manager.AddColorPoint(16, 0, 0, day_bg, day_particles);
        color_manager.AddColorPoint(18, 0, 0, semi_dawn_bg, hybrid_particles);
        color_manager.AddColorPoint(18, 30, 0, dawn_bg, hybrid_particles);
        color_manager.AddColorPoint(19, 0, 0, dark_bg, night_particles);

        return color_manager;
    }

    public static DayNightColorManager NightSkyBlue()
    {
        return Night(dark_sky_blue_bg);
    }

    public static DayNightColorManager Night(float[] dark_bg)
    {
        DayNightColorManager color_manager = new DayNightColorManager();

        color_manager.AddColorPoint(0, 0, 0, black_bg, night_particles);
        color_manager.AddColorPoint(3, 0, 0, black_bg, midnight_particles);
        color_manager.AddColorPoint(6, 0, 0, black_bg, midnight_particles);
        color_manager.AddColorPoint(7, 0, 0, dark_bg, midnight_particles);
        color_manager.AddColorPoint(8, 0, 0, dark_bg, night_particles);
        color_manager.AddColorPoint(19, 0, 0, dark_bg, night_particles);

        return color_manager;
    }

    /*public static DayNightColorManager Default()
    {
        DayNightColorManager color_manager = new DayNightColorManager();



        color_manager.AddColorPoint(0, 0, 0, black_bg, night_theme);
        color_manager.AddColorPoint(3, 0, 0, black_bg, midnight_theme);
        color_manager.AddColorPoint(6, 0, 0, black_bg, midnight_theme);
        color_manager.AddColorPoint(6, 30, 0, dawn_bg, hybrid_theme);
        color_manager.AddColorPoint(7, 0, 0, semi_dawn_bg, day_theme);
        color_manager.AddColorPoint(9, 0, 0, sky_blue_bg, day_theme);
        color_manager.AddColorPoint(16, 0, 0, sky_blue_bg, day_theme);
        color_manager.AddColorPoint(18, 0, 0, semi_dawn_bg, hybrid_theme);
        color_manager.AddColorPoint(18, 30, 0, dawn_bg, hybrid_theme);
        color_manager.AddColorPoint(19, 0, 0, dark_bg, night_theme);

        return color_manager;
    }*/

    private static float[] LerpFloatVector(float[] A, float[] B, float t)
    {
        int len = Math.max(A.length, B.length);
        float[] result = new float[len];
        int ai, bi;
        for (int i = 0; i < result.length; i++)
        {
            if (A.length == B.length)
            {
                ai = bi = i;
            }
            else if (A.length < B.length)
            {
                ai = i % A.length;
                bi = i;
            }
            else
            {
                ai = i;
                bi = i % B.length;
            }
            result[i] = A[ai] + (B[bi] - A[ai]) * t;
        }
        return result;
    }

    private static float[] GenerateBGColor(float[] color, float[] range)
    {
        return new float[] {
                BringToRange(color[0], range),
                BringToRange(color[1], range),
                BringToRange(color[2], range)
        };
    }

    private static float[] GenerateParticlesColor(float[] color, float[] range, float shuffle)
    {
        float[] result = new float[10 * 3];
        for (int i = 0; i < 10; i++)
        {
            int s = i * 3;
            result[s] = BringToRange(Normalize(Lerp(color[0], vibrant_colors[s], shuffle)), range);
            result[s+1] = BringToRange(Normalize(Lerp(color[1], vibrant_colors[s+1], shuffle)), range);
            result[s+2] = BringToRange(Normalize(Lerp(color[2], vibrant_colors[s+2], shuffle)), range);
        }
        return result;
    }

    private static float Lerp(float A, float B, float t)
    {
        return A + (B - A) * t;
    }

    private static float Normalize(float value)
    {
        return Math.min(1, Math.max(0, value));
    }

    private static float BringToRange(float value, float[] range)
    {
        return range[0] + value * (range[1] - range[0]);
    }

    private static float[] GetColorFromName(String name)
    {
        if (name.equals("white"))
            return white;
        if (name.equals("red"))
            return red;
        if (name.equals("orange"))
            return orange;
        if (name.equals("yellow"))
            return yellow;
        if (name.equals("lime"))
            return lime;
        if (name.equals("green"))
            return green;
        if (name.equals("mint"))
            return mint;
        if (name.equals("cyan"))
            return cyan;
        if (name.equals("sky_blue"))
            return sky_blue;
        if (name.equals("blue"))
            return blue;
        if (name.equals("lavender"))
            return lavender;
        if (name.equals("pink"))
            return pink;
        if (name.equals("salmon_pink"))
            return salmon_pink;
        if (name.equals("watermelon"))
            return watermelon;
        return white;
    }

    private static final float[] black_bg = { 0, 0, 0 };
    private static final float[] dark_sky_blue_bg = { 0.2f, 0.2f, 0.3f };
    private static final float[] dark_teal_bg = { 0.2f, 0.25f, 0.3f };
    private static final float[] dark_cyan_bg = { 0.2f, 0.22f, 0.3f };
    private static final float[] dark_lavender_bg = { 0.25f, 0.2f, 0.3f };
    private static final float[] dark_pink_bg = { 0.3f, 0.2f, 0.3f };
    private static final float[] dawn_bg = { 0.6f, 0.5f, 0.3f };
    private static final float[] semi_dawn_bg = { 0.6f, 0.6f, 0.5f };
    private static final float[] sky_blue_bg = { 0.4f, 0.6f, 0.7f };
    private static final float[] bright_cyan_bg = { 0.4f, 0.6f, 0.6f };
    private static final float[] bright_lavender_bg = { 0.5f, 0.4f, 0.7f };
    private static final float[] bright_teal_bg = { 0.4f, 0.7f, 0.55f };
    private static final float[] bright_pink_bg = { 0.7f, 0.4f, 0.6f };

    private static final float[] day_particles = {
            0.8f, 0.4f, 0.4f,
            0.8f, 0.4f, 0.65f,
            0.7f, 0.6f, 0.4f,
            0.8f, 0.8f, 0.4f,
            0.4f, 0.8f, 0.4f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.8f, 0.4f, 0.8f,
            0.4f, 0.6f, 0.7f,
            0.4f, 0.4f, 0.8f,
    };

    private static final float[] night_particles = {
            1.5f, 0.5f, 0.5f,
            1.5f, 0.5f, 0.5f,
            1.2f, 1.25f, 1,
            1.5f, 1.5f, 1,
            1, 1.4f, 1.2f,
            1, 1, 1,
            1, 1, 1,
            1, 1, 1.2f,
            1, 1, 1.2f,
            1, 1, 1.5f
    };

    private static final float[] hybrid_particles = {
            1, 0.5f, 0.5f,
            1, 0.5f, 0.75f,
            0.8f, 0.6f, 0.5f,
            0.8f, 0.8f, 0.5f,
            0.5f, 0.8f, 0.8f,
            0.6f, 0.6f, 0.6f,
            0.7f, 0.7f, 0.7f,
            0.8f, 0.6f, 0.8f,
            0.5f, 0.5f, 0.8f,
            0.6f, 0.6f, 1,
    };

    private static final float[] midnight_particles = {
            1.5f, 0.5f, 0.5f,
            0.6f, 0.5f, 0.5f,
            1, 1, 0.8f,
            1, 1, 1,
            0.4f, 0.6f, 0.5f,
            0.6f, 0.6f, 0.6f,
            0.8f, 0.8f, 0.8f,
            0.6f, 0.5f, 0.6f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.6f
    };

    private static final float[] grayscale_particles = {
            1, 1, 1,
            1, 1, 1,
            1, 1, 1,
            1, 1, 1,
            1, 1, 1,
            1, 1, 1,
            1, 1, 1,
            1, 1, 1,
            1, 1, 1,
            1, 1, 1
    };

    /*private static final float[] vibrant_night_particles = {
            1.5f, 0.5f, 0.5f,
            0.5f, 1.5f, 0.5f,
            0.5f, 0.5f, 1.5f,
            1.5f, 1.5f, 0.5f,
            0.5f, 1.5f, 1.5f,
            1.5f, 0.5f, 1.5f,
            1.5f, 1.0f, 0.5f,
            0.5f, 1.5f, 1.0f,
            1.0f, 0.5f, 1.5f,
            1f, 1f, 1f,
    };

    private static final float[] vibrant_dawn_particles = {
            1.0f, 0.5f, 0.5f,
            0.5f, 1.0f, 0.5f,
            0.5f, 0.5f, 1.0f,
            1.0f, 1.0f, 0.5f,
            0.5f, 1.0f, 1.0f,
            1.0f, 0.5f, 1.0f,
            1.0f, 0.75f, 0.5f,
            0.5f, 1.0f, 0.75f,
            0.75f, 0.5f, 1.0f,
            0.75f, 0.75f, 0.75f,
    };

    private static final float[] vibrant_day_particles = {
            0.8f, 0.4f, 0.4f,
            0.4f, 0.8f, 0.4f,
            0.4f, 0.4f, 0.8f,
            0.8f, 0.8f, 0.4f,
            0.4f, 0.8f, 0.8f,
            0.8f, 0.4f, 0.8f,
            0.8f, 0.6f, 0.4f,
            0.4f, 0.8f, 0.6f,
            0.6f, 0.4f, 0.8f,
            0.6f, 0.6f, 0.6f,
    };*/

    private static final float[] vibrant_colors = {
            1, 0, 0,
            0, 1, 0,
            0, 0, 1,
            1, 1, 0,
            0, 1, 1,
            1, 0, 1,
            1.0f, 0.5f, 0.0f,
            0.0f, 1.0f, 0.5f,
            0.5f, 0.0f, 1.0f,
            1, 1, 1,
    };

    private static final float[] max_bg_range = { 0.5f, 0.8f };
    private static final float[] min_bg_range = { 0.0f, 0.1f };
    private static final float[] max_particle_range = { 0.5f, 1.5f };
    private static final float[] min_particle_range = { 0.4f, 0.8f };

    /*private static final float[] day_bg_range = { 0.4f, 0.7f };
    private static final float[] dawn_bg_range = { 0.3f, 0.6f };
    private static final float[] night_bg_range = { 0.2f, 0.3f };
    private static final float[] day_particle_range = { 0.4f, 0.8f };
    private static final float[] dawn_particle_range = { 0.5f, 1.0f };
    private static final float[] night_particle_range = { 0.5f, 1.5f };*/

    private static final float[] white = { 0.5f, 0.5f, 0.5f };
    private static final float[] red = { 1, 0, 0 };
    private static final float[] orange = { 1, 0.5f, 0 };
    private static final float[] yellow = { 1, 1, 0 };
    private static final float[] lime = { 0.5f, 1, 0 };
    private static final float[] green = { 0, 1, 0 };
    private static final float[] mint = { 0, 1, 0.5f };
    private static final float[] cyan = { 0, 1, 1 };
    private static final float[] sky_blue = { 0, 0.5f, 1 };
    private static final float[] blue = { 0, 0, 1 };
    private static final float[] lavender = { 0.5f, 0, 1 };
    private static final float[] pink = { 1, 0, 0.8f };
    private static final float[] salmon_pink = { 0.8f, 0.4f, 0.38f };
    private static final float[] watermelon = { 1, 0, 0.25f };
}
