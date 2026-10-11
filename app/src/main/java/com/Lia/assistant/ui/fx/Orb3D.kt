package com.Lia.assistant.ui.fx

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.Lia.assistant.ui.theme.LocalReducedMotion
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Lit 3D sphere drawn per pixel by AGSL. Sphere normals come from the pixel position, the interior is
 * animated fractal noise, and the light follows the device tilt.
 */
private const val ORB_AGSL = """
uniform float2 resolution;
uniform float time;
uniform float amp;
uniform float pulse;
uniform float2 tilt;
uniform half4 coreColor;
uniform half4 rimColor;
uniform half4 haloColor;

float hash3(float3 p) {
    p = fract(p * 0.3183099 + float3(0.1, 0.1, 0.1));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float vnoise(float3 x) {
    float3 i = floor(x);
    float3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float x00 = mix(hash3(i), hash3(i + float3(1.0, 0.0, 0.0)), f.x);
    float x10 = mix(hash3(i + float3(0.0, 1.0, 0.0)), hash3(i + float3(1.0, 1.0, 0.0)), f.x);
    float x01 = mix(hash3(i + float3(0.0, 0.0, 1.0)), hash3(i + float3(1.0, 0.0, 1.0)), f.x);
    float x11 = mix(hash3(i + float3(0.0, 1.0, 1.0)), hash3(i + float3(1.0, 1.0, 1.0)), f.x);
    return mix(mix(x00, x10, f.y), mix(x01, x11, f.y), f.z);
}

float fbm(float3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p = p * 2.03;
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float s = 0.5 * min(resolution.x, resolution.y);
    float2 uv = (fragCoord - 0.5 * resolution) / s;
    float ang = atan(uv.y, uv.x);
    float r = length(uv);

    float R = 0.62 + pulse * sin(time * 2.5) + 0.02 * amp + 0.035 * amp * sin(ang * 5.0 + time * 4.0);

    float2 p2 = uv / R;
    float rr = length(p2);
    float sphereA = 1.0 - smoothstep(0.985, 1.0, rr);
    float z = sqrt(max(0.0, 1.0 - rr * rr));
    float3 n = float3(p2.x, -p2.y, z);

    float3 L = normalize(float3(-tilt.x * 0.9 + 0.35, tilt.y * 0.9 - 0.35, 0.85));
    float diff = max(dot(n, L), 0.0);
    float3 H = normalize(L + float3(0.0, 0.0, 1.0));
    float spec = pow(max(dot(n, H), 0.0), 48.0);
    float fres = pow(1.0 - z, 3.0);

    float cs = cos(time * 0.25);
    float sn = sin(time * 0.25);
    float3 q = float3(n.x * cs - n.z * sn, n.y, n.x * sn + n.z * cs) * 2.2;
    float energy = fbm(q + float3(time * 0.35, time * 0.2, 0.0));
    float swirl = 0.5 + 0.5 * sin(ang * 3.0 + time * (1.0 + amp * 2.0) + energy * 6.0);

    float3 core = float3(coreColor.rgb);
    float3 rim = float3(rimColor.rgb);
    float3 col = mix(core, rim, fres);
    col *= 0.35 + 0.65 * diff;
    col += core * energy * (0.45 + 0.55 * swirl) * (0.6 + amp);
    col += core * (1.0 - r) * amp * 0.6;
    col += float3(1.0) * spec * 0.7;
    col = clamp(col, 0.0, 1.5);

    float halo = pow(max(1.0 - (r - R) / 0.45, 0.0), 2.0) * 0.5;
    float hA = halo * haloColor.a * (1.0 - sphereA);
    float a = sphereA + hA;
    float3 pre = col * sphereA + float3(haloColor.rgb) * hA;
    float3 rgb = min(pre / max(a, 0.0001), float3(1.0));
    return half4(half3(rgb), half(a));
}
"""

/** Shader draw for Android 13+. Falls back to [NovaOrb] if the shader fails to compile. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun LiaOrb3D(
    state: OrbState,
    amplitude: Float,
    diameter: Dp,
    modifier: Modifier = Modifier,
    style: NovaOrbStyle = NovaOrbStyle.NOVA,
) {
    val reduced = LocalReducedMotion.current
    val shader = remember { runCatching { RuntimeShader(ORB_AGSL) }.getOrNull() }
    if (shader == null) {
        NovaOrb(state = state, amplitude = amplitude, diameter = diameter, style = style, modifier = modifier)
        return
    }

    val tiltState = rememberTilt(enabled = !reduced)
    val timeState by produceState(initialValue = 0f, reduced) {
        if (reduced) return@produceState
        var start = -1L
        while (true) {
            withFrameNanos { nanos ->
                if (start < 0) start = nanos
                value = (nanos - start) / 1_000_000_000f
            }
        }
    }

    val palette = paletteFor(state)
    val label = "Lia orb status: ${state.name.lowercase()}"

    Canvas(modifier.size(diameter).semantics { contentDescription = label }) {
        val tilt = if (reduced) Offset.Zero else tiltState.value
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", timeState * palette.speed)
        shader.setFloatUniform("amp", amplitude.coerceIn(0f, 1f))
        shader.setFloatUniform("pulse", palette.pulse)
        shader.setFloatUniform("tilt", tilt.x, tilt.y)
        shader.setFloatUniform("coreColor", argbToRgba(palette.core))
        shader.setFloatUniform("rimColor", argbToRgba(palette.rim))
        shader.setFloatUniform("haloColor", argbToRgba(palette.halo))
        drawShader(shader)
    }
}

private fun DrawScope.drawShader(shader: RuntimeShader) {
    drawIntoCanvas { canvas ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

/** Chooses the 3D shader on Android 13+ for the Nova style, otherwise the Canvas orb. */
@Composable
fun LiaOrb(
    state: OrbState,
    amplitude: Float,
    diameter: Dp,
    modifier: Modifier = Modifier,
    style: NovaOrbStyle = NovaOrbStyle.NOVA,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && style == NovaOrbStyle.NOVA) {
        LiaOrb3D(state, amplitude, diameter, modifier, style)
    } else {
        NovaOrb(state, amplitude, diameter, style, modifier)
    }
}

/** ARGB Long to RGBA floats in 0..1. */
internal fun argbToRgba(argb: Long): FloatArray = floatArrayOf(
    ((argb shr 16) and 0xFF) / 255f,
    ((argb shr 8) and 0xFF) / 255f,
    (argb and 0xFF) / 255f,
    ((argb shr 24) and 0xFF) / 255f,
)
