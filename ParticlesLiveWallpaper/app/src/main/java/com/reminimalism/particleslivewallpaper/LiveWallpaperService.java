package com.reminimalism.particleslivewallpaper;

import static com.reminimalism.particleslivewallpaper.SettingsActivityKt.PERSONALIZATION_SUPPORT_DARK_TEXT;
import static com.reminimalism.particleslivewallpaper.SettingsActivityKt.getAppPreferences;

import android.app.ActivityManager;
import android.app.WallpaperColors;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class LiveWallpaperService extends WallpaperService
{
    @Override
    public Engine onCreateEngine()
    {
        return new LiveWallpaperEngine();
    }

    private class LiveWallpaperEngine extends Engine
    {
        class WallpaperGLSurfaceView extends GLSurfaceView
        {
            WallpaperGLSurfaceView(Context context)
            {
                super(context);
            }

            @Override
            public SurfaceHolder getHolder()
            {
                return getSurfaceHolder();
            }

            public void onDestroy()
            {
                super.onDetachedFromWindow();
            }
        }

        WallpaperGLSurfaceView GLSurface = null;
        LiveWallpaperRenderer Renderer = null;

        @Override
        public WallpaperColors onComputeColors()
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            {
                SharedPreferences preferences = getAppPreferences(LiveWallpaperService.this);
                ColorManager colorManager = ColorManagerOptions.Get(preferences);
                colorManager.UpdateNow();
                float[] bg = colorManager.GetBGColor();
                Color primaryColor = Color.valueOf(bg[0], bg[1], bg[2]);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    boolean shouldHint = preferences.getBoolean(PERSONALIZATION_SUPPORT_DARK_TEXT, true);
                    if (shouldHint)
                    {
                        boolean supports_dark_text = ((bg[0] + bg[1] + bg[2]) / 3f) > 0.3f;
                        int hints = supports_dark_text ? WallpaperColors.HINT_SUPPORTS_DARK_TEXT : 0;
                        return new WallpaperColors(primaryColor, null, null, hints);
                    }
                }
                return new WallpaperColors(primaryColor, null, null);
            }
            return null;
        }

        @Override
        public void onCreate(final SurfaceHolder surface_holder)
        {
            super.onCreate(surface_holder);
            GLSurface = new WallpaperGLSurfaceView(LiveWallpaperService.this);

            boolean SupportsGLES2 = false;
            try
            {
                SupportsGLES2 =
                        ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE))
                                .getDeviceConfigurationInfo().reqGlEsVersion >= 0x20000;
            }
            catch (NullPointerException ignored) {}

            if (!SupportsGLES2)
                return;

            GLSurface.setEGLContextClientVersion(2);
            GLSurface.setPreserveEGLContextOnPause(true);

            Renderer = new LiveWallpaperRenderer(
                    LiveWallpaperService.this, this
            );
            GLSurface.setRenderer(Renderer);
        }

        @Override
        public void onVisibilityChanged(boolean visible)
        {
            super.onVisibilityChanged(visible);
            if (GLSurface != null)
                if (visible)
                {
                    Renderer.onResume();
                    GLSurface.onResume();
                }
                else
                {
                    GLSurface.onPause();
                    Renderer.onPause();
                }
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();
            Renderer.onDestroy();
            if (GLSurface != null)
                GLSurface.onDestroy();
        }
    }
}
