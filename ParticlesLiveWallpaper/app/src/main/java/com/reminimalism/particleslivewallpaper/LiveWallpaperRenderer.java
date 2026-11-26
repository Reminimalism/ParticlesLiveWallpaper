package com.reminimalism.particleslivewallpaper;

import static com.reminimalism.particleslivewallpaper.SettingsActivityKt.getAppPreferences;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.Surface;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LiveWallpaperRenderer implements GLSurfaceView.Renderer
{
    float MIN_PARTICLES_BOUNDARY = 4;
    float MAX_PARTICLES_BOUNDARY = 10;
    float MIN_PARTICLE_DENSITY = 1000;
    float MAX_PARTICLE_DENSITY = 2000;
    float MIN_ACCELERATION = 0;
    float MAX_ACCELERATION = 2;
    float MIN_ROTATION_SPEED = 0.5f;
    float MAX_ROTATION_SPEED = 5;

    float MAX_FOV_RADIAN = 0.5f;

    // Loaded from preferences
    int NumberOfParticles = 5000;
    float ParticlesBoundary = 4;
    float Acceleration = 1;
    float RotationSpeed = 1.4f;

    // Depending on preferences
    float FarPlane = ParticlesBoundary * 0.5f;

    float ParticleMinSizeReferenceValue = 0.005f;
    float ParticleSizeDiffReferenceValue = 1f;

    Context context;
    WallpaperService.Engine engine;

    SharedPreferences Preferences = null;
    SharedPreferences.OnSharedPreferenceChangeListener PreferenceChangeListener = null;
    SensorManager SensorManagerInstance = null;
    SensorEventListener RotationSensorEventListener = null;
    SensorEventListener AccelerationSensorEventListener = null;

    float[] ScreenFront = new float[3];
    float[] ScreenUp = new float[3];
    float[] ScreenRight = new float[3];
    float[] RotationMatrix = { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
    float[] TargetRotationMatrix = { 1, 0, 0, 0, 1, 0, 0, 0, 1 };

    float[] AccelerationVector = { 0, 0, 0 };

    FloatBuffer ParticleVertexPositionsBuffer;
    FloatBuffer ParticleVertexInfoBuffer;
    ShortBuffer ParticleTriangleIndicesBuffer;

    boolean LimitFramerate = false;
    int FrameMinDuration_ms = 10;

    long LastColorChangeNotifyTime = 0;

    LiveWallpaperRenderer(Context context, WallpaperService.Engine engine)
    {
        this.context = context;
        this.engine = engine;
        Preferences = getAppPreferences(context);
        SensorManagerInstance = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        PreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
        {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
                InitializeBasedOnPreferences();
                notifyColorsChanged();
            }
        };
        Preferences.registerOnSharedPreferenceChangeListener(PreferenceChangeListener);

        if (SensorManagerInstance != null)
        {
            RotationSensorEventListener = new SensorEventListener()
            {
                @Override
                public void onSensorChanged(SensorEvent event)
                {
                    if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
                    {
                        //System.arraycopy(event.values, 0, TargetRotationVector, 0, 4);
                        SensorManager.getRotationMatrixFromVector(TargetRotationMatrix, event.values);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            };
            AccelerationSensorEventListener = new SensorEventListener()
            {
                @Override
                public void onSensorChanged(SensorEvent event)
                {
                    if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
                    {
                        System.arraycopy(event.values, 0, AccelerationVector, 0, 3);
                        for (int i = 0; i < 3; i++)
                            AccelerationVector[i] *= Acceleration;
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            };
            RegisterSensorListeners();
        }
    }

    private void notifyColorsChangedPeriodically()
    {
        final long COLOR_CHANGE_NOTIFY_INTERVAL_MS = 60000L * 30L; // 30 min
        if (Time - LastColorChangeNotifyTime > COLOR_CHANGE_NOTIFY_INTERVAL_MS)
        {
            notifyColorsChanged();
        }
    }

    private void notifyColorsChanged()
    {
        LastColorChangeNotifyTime = Time;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && engine != null)
        {
            engine.notifyColorsChanged();
        }
    }

    private void RegisterSensorListeners()
    {
        if (SensorManagerInstance != null)
        {
            Sensor RotationSensor = SensorManagerInstance.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            Sensor AccelerationSensor = SensorManagerInstance.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            SensorManagerInstance.registerListener(
                    RotationSensorEventListener,
                    RotationSensor,
                    SensorManager.SENSOR_DELAY_GAME
            );
            SensorManagerInstance.registerListener(
                    AccelerationSensorEventListener,
                    AccelerationSensor,
                    SensorManager.SENSOR_DELAY_GAME
            );
        }
    }

    private void UnregisterSensorListeners()
    {
        if (RotationSensorEventListener != null)
            SensorManagerInstance.unregisterListener(RotationSensorEventListener);
        if (AccelerationSensorEventListener != null)
            SensorManagerInstance.unregisterListener(AccelerationSensorEventListener);
    }

    void onDestroy()
    {
        DeleteVertexBufferObjects();
        if (SensorManagerInstance != null)
        {
            UnregisterSensorListeners();
            if (Preferences != null && PreferenceChangeListener != null)
                Preferences.unregisterOnSharedPreferenceChangeListener(PreferenceChangeListener);
        }
    }

    void onPause()
    {
        UnregisterSensorListeners();
    }

    void onResume()
    {
        RegisterSensorListeners();
    }

    private int report_notif_id = 0;
    void ResetReportId()
    {
        report_notif_id = 0;
    }
    void ReportError(String Message)
    {
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        new Throwable().printStackTrace(pwriter);
        String StackTrace = swriter.toString();
        final String Text = Message + "\n\nStack Trace:\n" + StackTrace;

        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder notification_builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel("errors", "Errors", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Errors are reported here.");
            nm.createNotificationChannel(channel);
            notification_builder = new Notification.Builder(context, "errors");
        }
        else
            notification_builder = new Notification.Builder(context);
        Notification notification = notification_builder
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(report_notif_id == 0 ? "First error (copied to clipboard)" : "Error")
                .setContentText(Text)
                .setStyle(new Notification.BigTextStyle())
                .build();
        nm.notify(report_notif_id++, notification);

        if (report_notif_id == 0)
            ((ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("Error text", Text));
    }
    int CompileShader(int ShaderType, String Source, String Label)
    {
        int Shader = GLES20.glCreateShader(
                ShaderType
        );
        if (Shader != 0)
        {
            GLES20.glShaderSource(Shader, Source);
            GLES20.glCompileShader(Shader);

            final int[] compile_status = new int[1];
            GLES20.glGetShaderiv(Shader, GLES20.GL_COMPILE_STATUS, compile_status, 0);

            if (compile_status[0] == 0)
            {
                String log = GLES20.glGetShaderInfoLog(Shader);
                GLES20.glDeleteShader(Shader);
                ReportError(Label
                        + (ShaderType == GLES20.GL_FRAGMENT_SHADER ? " fragment" : " vertex")
                        + " shader compilation failed. " + log);
                return 0;
            }
        }
        else ReportError(Label
                + (ShaderType == GLES20.GL_FRAGMENT_SHADER ? " fragment" : " vertex")
                + " shader creation failed.");
        return Shader;
    }

    int ParticleProgram = 0;

    int PositionAttribute;
    int VertexInfoAttribute;
    int ViewUniform;
    int ProjectionUniform;
    int MinSizeUniform;
    int SizeDiffUniform;
    int ColorsUniform;

    float[] CameraAcceleration = { 0, 0, 0 };
    float[] CameraSpeed = { 0, 0, 0 };
    float[] CameraPosition = { 0, 0, 0 };
    float[] ViewMatrix = new float[16];
    float[] ProjectionMatrix = new float[16];
    ColorManager _ColorManager;

    int ParticleVertexPositionsBufferId = 0;
    int ParticleVertexInfoBufferId = 0;
    int ParticleTriangleIndicesBufferId = 0;
    int IndicesCount = 0;
    void CreateVertexBufferObjects()
    {
        DeleteVertexBufferObjects();
        final int[] buffers = new int[3];
        GLES20.glGenBuffers(3, buffers, 0);
        ParticleVertexPositionsBufferId = buffers[0];
        ParticleVertexInfoBufferId = buffers[1];
        ParticleTriangleIndicesBufferId = buffers[2];
        IndicesCount = ParticleTriangleIndicesBuffer.capacity();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ParticleVertexPositionsBufferId);
        GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                ParticleVertexPositionsBuffer.capacity() * 4,
                ParticleVertexPositionsBuffer,
                GLES20.GL_STATIC_DRAW
        );
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ParticleVertexInfoBufferId);
        GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                ParticleVertexInfoBuffer.capacity() * 4,
                ParticleVertexInfoBuffer,
                GLES20.GL_STATIC_DRAW
        );
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ParticleTriangleIndicesBufferId);
        GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER,
                ParticleTriangleIndicesBuffer.capacity() * 2,
                ParticleTriangleIndicesBuffer,
                GLES20.GL_STATIC_DRAW
        );
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    void DeleteVertexBufferObjects()
    {
        if (ParticleVertexPositionsBufferId == 0)
            return;
        final int[] buffers = new int[] {
                ParticleVertexPositionsBufferId,
                ParticleVertexInfoBufferId,
                ParticleTriangleIndicesBufferId
        };
        GLES20.glDeleteBuffers(buffers.length, buffers, 0);
        ParticleVertexPositionsBufferId = 0;
        ParticleVertexInfoBufferId = 0;
        ParticleTriangleIndicesBufferId = 0;
    }

    void InitializeBasedOnPreferences()
    {
        // Preferences

        _ColorManager = ColorManagerOptions.Get(Preferences);

        Acceleration =
                MIN_ACCELERATION
                        + (MAX_ACCELERATION - MIN_ACCELERATION)
                        * (float)Preferences.getInt("acceleration", 50) / 100f;

        RotationSpeed =
                MIN_ROTATION_SPEED
                        + (MAX_ROTATION_SPEED - MIN_ROTATION_SPEED)
                        * (float)Preferences.getInt("rotation_speed", 20) / 100f;

        ParticlesBoundary =
                MIN_PARTICLES_BOUNDARY
                        + (MAX_PARTICLES_BOUNDARY - MIN_PARTICLES_BOUNDARY)
                        * (float)Preferences.getInt("space_size", 0) / 100f;

        float particles_density =
                MIN_PARTICLE_DENSITY
                        + (MAX_PARTICLE_DENSITY - MIN_PARTICLE_DENSITY)
                        * (float)Preferences.getInt("particles_density", 25) / 100f;

        FarPlane = ParticlesBoundary * 0.5f;

        NumberOfParticles = (int) (ParticlesBoundary * particles_density);
    }

    void Initialize()
    {
        ResetReportId();

        // GL setup

        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);

        int ParticleVertexShader = CompileShader(GLES20.GL_VERTEX_SHADER, ReadRawTextResource(R.raw.particle_vertex_shader), "Particles");
        if (ParticleVertexShader == 0)
            return;
        int ParticleFragmentShader = CompileShader(GLES20.GL_FRAGMENT_SHADER, ReadRawTextResource(R.raw.particle_fragment_shader), "Particles");
        if (ParticleFragmentShader == 0)
        {
            GLES20.glDeleteShader(ParticleFragmentShader);
            return;
        }
        ParticleProgram = GLES20.glCreateProgram();
        if (ParticleProgram == 0)
        {
            GLES20.glDeleteShader(ParticleVertexShader);
            GLES20.glDeleteShader(ParticleFragmentShader);
            return;
        }
        GLES20.glAttachShader(ParticleProgram, ParticleVertexShader);
        GLES20.glAttachShader(ParticleProgram, ParticleFragmentShader);
        GLES20.glBindAttribLocation(ParticleProgram, 0, "Position");
        GLES20.glBindAttribLocation(ParticleProgram, 1, "VertexInfo");
        GLES20.glLinkProgram(ParticleProgram);
        final int[] link_status = new int[1];
        GLES20.glGetProgramiv(ParticleProgram, GLES20.GL_LINK_STATUS, link_status, 0);
        GLES20.glDeleteShader(ParticleVertexShader);
        GLES20.glDeleteShader(ParticleFragmentShader);
        if (link_status[0] == 0)
        {
            GLES20.glDeleteProgram(ParticleProgram);
            ParticleProgram = 0;
            ReportError("Particle program linking failed.");
            return;
        }
        PositionAttribute = GLES20.glGetAttribLocation(ParticleProgram, "Position");
        VertexInfoAttribute = GLES20.glGetAttribLocation(ParticleProgram, "VertexInfo");
        ViewUniform = GLES20.glGetUniformLocation(ParticleProgram, "View");
        ProjectionUniform = GLES20.glGetUniformLocation(ParticleProgram, "Projection");
        MinSizeUniform = GLES20.glGetUniformLocation(ParticleProgram, "MinSize");
        SizeDiffUniform = GLES20.glGetUniformLocation(ParticleProgram, "SizeDiff");
        ColorsUniform = GLES20.glGetUniformLocation(ParticleProgram, "Colors");

        GLES20.glUseProgram(ParticleProgram);

        InitializeBasedOnPreferences();

        // Particle positions
        Random random = new Random();
        float[] positions_array = new float[NumberOfParticles * 3 * 4]; // (x, y, z) * 4 (quads)
        float[] uv_type_array = new float[NumberOfParticles * 3 * 4]; // (u, v, type_index) * 4 (quads)
        short[] indices_array = new short[NumberOfParticles * 3 * 2]; // 3 indices per triangle
        float ParticlesArea = ParticlesBoundary * 2;
        for (int i = 0; i < NumberOfParticles; i++)
        {
            // set vertex positions for this particle
            for (int k = 0; k < 3; k++)
            {
                float position_value = -ParticlesBoundary + ParticlesArea * random.nextFloat();
                for (int j = 0; j < 4; j++)
                {
                    positions_array[i * 3 * 4 + j * 3 + k] = position_value;
                }
            }
            // set vertex UVs and type indices for this particle
            float type_index = (float)(i * 10 / NumberOfParticles);
            for (int j = 0; j < 4; j++)
            {
                if (j % 2 == 0)
                    uv_type_array[i * 3 * 4 + j * 3] = -1;
                else
                    uv_type_array[i * 3 * 4 + j * 3] = 1;
                if (j < 2)
                    uv_type_array[i * 3 * 4 + j * 3 + 1] = -1;
                else
                    uv_type_array[i * 3 * 4 + j * 3 + 1] = 1;
                uv_type_array[i * 3 * 4 + j * 3 + 2] = type_index;
            }
            // set indices for this particle
            for (int j = 0; j < 3; j++)
                indices_array[i * 3 * 2 + j] = (short)(i * 4 + j);
            for (int j = 0; j < 3; j++)
                indices_array[i * 3 * 2 + j + 3] = (short)(i * 4 + 1 + j);
        }
        ParticleVertexPositionsBuffer = ByteBuffer.allocateDirect(positions_array.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        ParticleVertexPositionsBuffer.put(positions_array).position(0);
        ParticleVertexInfoBuffer = ByteBuffer.allocateDirect(uv_type_array.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        ParticleVertexInfoBuffer.put(uv_type_array).position(0);
        ParticleTriangleIndicesBuffer = ByteBuffer.allocateDirect(indices_array.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        ParticleTriangleIndicesBuffer.put(indices_array).position(0);

        CreateVertexBufferObjects();

        // The constant part of ViewMatrix

        // NOTE: column-major order
        // m[offset +  0] m[offset +  4] m[offset +  8] m[offset + 12]
        // m[offset +  1] m[offset +  5] m[offset +  9] m[offset + 13]
        // m[offset +  2] m[offset +  6] m[offset + 10] m[offset + 14]
        // m[offset +  3] m[offset +  7] m[offset + 11] m[offset + 15]

        ViewMatrix[3] = 0;
        ViewMatrix[7] = 0;
        ViewMatrix[11] = 0;
        ViewMatrix[15] = 1;

        for (int i = 0; i < 15; i++)
            ProjectionMatrix[i] = 0;

        // Update time
        GetTimeDiff_ms();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Initialize();
    }

    int Width = 100;
    int Height = 100;
    float AspectRatio = 1;
    float FOVRadian = MAX_FOV_RADIAN;
    float[] ParticleMinSize = { 0.005f, 0.005f };
    float[] ParticleSizeDiff = { 1f, 1f };

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
        Width = width;
        Height = height;
        AspectRatio = (float)width / (float)height;
        FOVRadian = AspectRatio < 1 ? MAX_FOV_RADIAN : MAX_FOV_RADIAN / AspectRatio;
        if (width < height)
        {
            ParticleMinSize[0] = ParticleMinSizeReferenceValue / AspectRatio;
            ParticleSizeDiff[0] = ParticleSizeDiffReferenceValue / AspectRatio;
            ParticleMinSize[1] = ParticleMinSizeReferenceValue;
            ParticleSizeDiff[1] = ParticleSizeDiffReferenceValue;
        }
        else
        {
            ParticleMinSize[0] = ParticleMinSizeReferenceValue;
            ParticleSizeDiff[0] = ParticleSizeDiffReferenceValue;
            ParticleMinSize[1] = ParticleMinSizeReferenceValue * AspectRatio;
            ParticleSizeDiff[1] = ParticleSizeDiffReferenceValue * AspectRatio;
        }
    }

    long Time = 0;
    int GetTimeDiff_ms()
    {
        long CurrentTime = System.currentTimeMillis();
        long Diff = CurrentTime - Time;
        Time = CurrentTime;
        if (Diff > 0)
            return (int)Diff;
        else
            return 0;
    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (ParticleProgram == 0)
            return;

        final float MAX_LIMITED_DELTA = 0.05f;
        final float PAUSE_TIME_THRESHOLD = 1;

        // Time

        int time_diff_ms;
        if (LimitFramerate)
        {
            time_diff_ms = GetTimeDiff_ms();
            int remaining = FrameMinDuration_ms - time_diff_ms;
            if (remaining > 0) try
            {
                Thread.sleep(remaining);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            time_diff_ms += GetTimeDiff_ms();
        }
        else
            time_diff_ms = GetTimeDiff_ms();
        float time_diff = (float) time_diff_ms * 0.001f;
        float limited_time_diff = Math.min(time_diff, MAX_LIMITED_DELTA);
        if (time_diff > PAUSE_TIME_THRESHOLD)
        {
            _ColorManager.UpdateNow();
        }
        notifyColorsChangedPeriodically();

        // Interpolate

        for (int i = 0; i < 9; i++)
        {
            RotationMatrix[i] += (TargetRotationMatrix[i] - RotationMatrix[i]) * limited_time_diff * RotationSpeed;
        }
        /*for (int i = 0; i < 3; i++)
        {
            AccelerationVector[i] += (TargetAccelerationVector[i] - AccelerationVector[i]) * limited_time_diff;
        }*/

        // Calculate rotation

        int rotation;
        try
        {
            rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay()
                    .getRotation();
        }
        catch (NullPointerException e)
        {
            rotation = Surface.ROTATION_0;
        }
        boolean SwapUpAndRight = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
        int UpIndexOffset = SwapUpAndRight ? 0 : 1;
        int RightIndexOffset = SwapUpAndRight ? 1 : 0;
        float UpFinalCoefficient = rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_180 ? -1 : 1;
        float RightFinalCoefficient = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_180 ? -1 : 1;

        ScreenFront[0] = RotationMatrix[2];
        ScreenFront[1] = RotationMatrix[5];
        ScreenFront[2] = RotationMatrix[8];
        ScreenUp[0] = RotationMatrix[UpIndexOffset] * UpFinalCoefficient;
        ScreenUp[1] = RotationMatrix[3 + UpIndexOffset] * UpFinalCoefficient;
        ScreenUp[2] = RotationMatrix[6 + UpIndexOffset] * UpFinalCoefficient;
        ScreenRight[0] = RotationMatrix[RightIndexOffset] * RightFinalCoefficient;
        ScreenRight[1] = RotationMatrix[3 + RightIndexOffset] * RightFinalCoefficient;
        ScreenRight[2] = RotationMatrix[6 + RightIndexOffset] * RightFinalCoefficient;

        // Normalize

        float screen_front_size = 0, screen_up_size = 0, screen_right_size = 0;
        for (int i = 0; i < 3; i++)
        {
            screen_front_size += ScreenFront[i] * ScreenFront[i];
            screen_up_size += ScreenUp[i] * ScreenUp[i];
            screen_right_size += ScreenRight[i] * ScreenRight[i];
        }
        screen_front_size = (float)Math.sqrt(screen_front_size);
        screen_up_size = (float)Math.sqrt(screen_up_size);
        screen_right_size = (float)Math.sqrt(screen_right_size);

        for (int i = 0; i < 3; i++)
        {
            ScreenRight[i] = ScreenRight[i] / screen_right_size;
            ScreenUp[i] = ScreenUp[i] / screen_up_size;
            ScreenFront[i] = ScreenFront[i] / screen_front_size;
        }

        // Move camera

        for (int i = 0; i < 3; i++)
        {
            CameraAcceleration[i] = AccelerationVector[0] * RotationMatrix[i * 3];
            CameraAcceleration[i] += AccelerationVector[1] * RotationMatrix[i * 3 + 1];
            CameraAcceleration[i] += AccelerationVector[2] * RotationMatrix[i * 3 + 2];

            CameraSpeed[i] += CameraAcceleration[i] * limited_time_diff;
            CameraSpeed[i] += ((-CameraPosition[i] / ParticlesBoundary) - CameraSpeed[i]) * limited_time_diff;
            CameraPosition[i] += CameraSpeed[i] * limited_time_diff;
            if (Math.abs(CameraPosition[i]) > ParticlesBoundary)
            {
                if (CameraPosition[i] > 0)
                    CameraPosition[i] = ParticlesBoundary;
                else
                    CameraPosition[i] = -ParticlesBoundary;
            }
        }

        // Camera settings

        float Z_FAR = 1 + FarPlane;
        float Z_NEAR = 1;

        // Set ViewMatrix values

        // NOTE: column-major order
        // m[offset +  0] m[offset +  4] m[offset +  8] m[offset + 12]
        // m[offset +  1] m[offset +  5] m[offset +  9] m[offset + 13]
        // m[offset +  2] m[offset +  6] m[offset + 10] m[offset + 14]
        // m[offset +  3] m[offset +  7] m[offset + 11] m[offset + 15]

        ViewMatrix[12] = 0;
        ViewMatrix[13] = 0;
        ViewMatrix[14] = 0;
        for (int i = 0; i < 3; i++)
        {
            ViewMatrix[i * 4] = ScreenRight[i];
            ViewMatrix[i * 4 + 1] = ScreenUp[i];
            ViewMatrix[i * 4 + 2] = ScreenFront[i];

            ViewMatrix[12] += ViewMatrix[i * 4] * -(CameraPosition[i] - ScreenFront[i]) ;
            ViewMatrix[13] += ViewMatrix[i * 4 + 1] * -CameraPosition[i];
            ViewMatrix[14] += ViewMatrix[i * 4 + 2] * -CameraPosition[i];
        }

        // Set ProjectionMatrix values

        // NOTE: column-major order
        // m[offset +  0] m[offset +  4] m[offset +  8] m[offset + 12]
        // m[offset +  1] m[offset +  5] m[offset +  9] m[offset + 13]
        // m[offset +  2] m[offset +  6] m[offset + 10] m[offset + 14]
        // m[offset +  3] m[offset +  7] m[offset + 11] m[offset + 15]

        float tanHalfFovy = (float) Math.tan(FOVRadian / 2);
        ProjectionMatrix[0] = 1 / (AspectRatio * tanHalfFovy);
        ProjectionMatrix[5] = 1 / (tanHalfFovy);
        ProjectionMatrix[10] = - (Z_FAR + Z_NEAR) / (Z_FAR - Z_NEAR);
        ProjectionMatrix[11] = -1;
        ProjectionMatrix[14] = -(2 * Z_FAR * Z_NEAR) / (Z_FAR - Z_NEAR);

        // Render

        float[] particle_colors = _ColorManager.GetParticleColors();
        float[] bg_color = _ColorManager.GetBGColor();
        GLES20.glClearColor(bg_color[0], bg_color[1], bg_color[2], 1);

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(ParticleProgram);

        GLES20.glUniformMatrix4fv(ViewUniform, 1, false, ViewMatrix, 0);
        GLES20.glUniformMatrix4fv(ProjectionUniform, 1, false, ProjectionMatrix, 0);
        GLES20.glUniform2f(MinSizeUniform, ParticleMinSize[0],  ParticleMinSize[1]);
        GLES20.glUniform2f(SizeDiffUniform, ParticleSizeDiff[0], ParticleSizeDiff[1]);
        GLES20.glUniform3fv(ColorsUniform, 10, particle_colors, 0);
        // Uploading to GPU each time (bad way):
        //GLES20.glVertexAttribPointer(PositionAttribute, 3, GLES20.GL_FLOAT, false, 0, ParticleVertexPositionsBuffer);
        //GLES20.glEnableVertexAttribArray(PositionAttribute);
        //GLES20.glVertexAttribPointer(VertexInfoAttribute, 3, GLES20.GL_FLOAT, false, 0, ParticleVertexInfoBuffer);
        //GLES20.glEnableVertexAttribArray(VertexInfoAttribute);
        //GLES20.glDrawElements(GLES20.GL_TRIANGLES, IndicesCount, GLES20.GL_UNSIGNED_SHORT, ParticleTriangleIndicesBuffer);
        // Using VBOs:
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ParticleVertexPositionsBufferId);
        GLES20.glVertexAttribPointer(PositionAttribute, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(PositionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ParticleVertexInfoBufferId);
        GLES20.glVertexAttribPointer(VertexInfoAttribute, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(VertexInfoAttribute);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ParticleTriangleIndicesBufferId);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, IndicesCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    String ReadRawTextResource(int id)
    {
        return ReadInputStream(context.getResources().openRawResource(id));
    }

    String ReadInputStream(InputStream stream)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        StringBuilder builder = new StringBuilder();

        try
        {
            while ((line = reader.readLine()) != null)
            {
                builder.append(line);
                builder.append('\n');
            }
        } catch (IOException e) { return null; }

        return builder.toString();
    }
}
