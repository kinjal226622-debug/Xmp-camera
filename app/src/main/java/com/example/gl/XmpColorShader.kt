package com.example.gl

import com.example.xmp.XmpPreset
import kotlin.math.cos
import kotlin.math.sin

object XmpColorShader {

    const val VERTEX_SHADER = """
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTextureCoord;
        uniform mat4 uTextureMatrix;

        void main() {
            gl_Position = aPosition;
            vTextureCoord = (uTextureMatrix * aTextureCoord).xy;
        }
    """

    const val FRAGMENT_SHADER_OES = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;

        uniform float uIntensity;
        uniform float uExposure;
        uniform float uContrast;
        uniform float uHighlights;
        uniform float uShadows;
        uniform float uWhites;
        uniform float uBlacks;
        uniform float uTemperature;
        uniform float uTint;
        uniform float uVibrance;
        uniform float uSaturation;
        uniform float uVignette;
        uniform float uGrain;
        uniform vec3 uShadowTint;
        uniform float uShadowTintSat;
        uniform vec3 uHighlightTint;
        uniform float uHighlightTintSat;
        uniform float uTime;
        uniform float uCompareSplit; // 0.0 to 1.0; x < uCompareSplit ? filtered : original

        // Pseudo-random generator for cinematic film grain
        float rand(vec2 co) {
            return fract(sin(dot(co.xy + vec2(uTime * 0.01, uTime * 0.03), vec2(12.9898, 78.233))) * 43758.5453);
        }

        vec3 rgb2hsv(vec3 c) {
            vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

            float d = q.x - min(q.w, q.y);
            float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }

        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }

        void main() {
            vec4 rawColor = texture2D(sTexture, vTextureCoord);
            vec3 orig = rawColor.rgb;

            // If in split-compare mode and coordinate is past split line, render original
            if (uCompareSplit < 1.0 && vTextureCoord.x > uCompareSplit) {
                // Draw a 1px white divider line
                if (abs(vTextureCoord.x - uCompareSplit) < 0.003) {
                    gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
                } else {
                    gl_FragColor = vec4(orig, rawColor.a);
                }
                return;
            }

            vec3 color = orig;

            // 1. Exposure (Lightroom EV exposure scale)
            color = color * exp2(uExposure);

            // 2. Whites & Blacks
            color = clamp(color + vec3(uWhites * 0.2), 0.0, 1.0);
            color = clamp(color + vec3(uBlacks * 0.15) * (1.0 - color), 0.0, 1.0);

            // 3. Highlights & Shadows (tonal zone shaping)
            float luma = dot(color, vec3(0.299, 0.587, 0.114));
            float highlightWeight = smoothstep(0.5, 1.0, luma);
            float shadowWeight = 1.0 - smoothstep(0.0, 0.5, luma);

            color += color * (uHighlights * 0.3 * highlightWeight);
            color += color * (uShadows * 0.35 * shadowWeight);

            // 4. Contrast
            color = clamp((color - 0.5) * (1.0 + uContrast) + 0.5, 0.0, 1.0);

            // 5. White Balance (Temperature: Amber/Blue, Tint: Magenta/Green)
            color.r += uTemperature * 0.15;
            color.b -= uTemperature * 0.15;
            color.g -= uTint * 0.12;
            color.r += uTint * 0.06;
            color.b += uTint * 0.06;
            color = clamp(color, 0.0, 1.0);

            // 6. Split Toning / Color Grading
            if (uShadowTintSat > 0.0) {
                color = mix(color, color * uShadowTint, shadowWeight * uShadowTintSat);
            }
            if (uHighlightTintSat > 0.0) {
                color = mix(color, color + (uHighlightTint - 0.5) * (highlightWeight * uHighlightTintSat), 0.5);
            }

            // 7. Saturation & Vibrance (vibrance protects already saturated skin tones)
            float maxC = max(max(color.r, color.g), color.b);
            float minC = min(min(color.r, color.g), color.b);
            float satVal = (maxC - minC) / (maxC + 1.0e-5);
            float vibranceFactor = uVibrance * (1.0 - satVal);

            luma = dot(color, vec3(0.299, 0.587, 0.114));
            color = mix(vec3(luma), color, 1.0 + uSaturation + vibranceFactor);

            // 8. Vignette
            if (uVignette != 0.0) {
                vec2 center = vec2(0.5, 0.5);
                float dist = distance(vTextureCoord, center) * 1.414;
                float vig = smoothstep(0.4, 1.1, dist);
                if (uVignette < 0.0) {
                    color = mix(color, color * (1.0 + uVignette), vig);
                } else {
                    color = mix(color, color + vec3(uVignette * 0.4), vig);
                }
            }

            // 9. Cinematic Film Grain
            if (uGrain > 0.0) {
                float noise = (rand(vTextureCoord) - 0.5) * (uGrain * 0.15);
                color = clamp(color + noise, 0.0, 1.0);
            }

            // 10. Blend graded color with original based on Preset Intensity (0% to 100%)
            vec3 finalColor = mix(orig, clamp(color, 0.0, 1.0), uIntensity);

            gl_FragColor = vec4(finalColor, rawColor.a);
        }
    """

    fun hueToRgbVec3(hueDegrees: Float): FloatArray {
        val h = (hueDegrees % 360f) / 60f
        val x = 1f - kotlin.math.abs((h % 2f) - 1f)
        return when {
            h < 1f -> floatArrayOf(1f, x, 0f)
            h < 2f -> floatArrayOf(x, 1f, 0f)
            h < 3f -> floatArrayOf(0f, 1f, x)
            h < 4f -> floatArrayOf(0f, x, 1f)
            h < 5f -> floatArrayOf(x, 0f, 1f)
            else -> floatArrayOf(1f, 0f, x)
        }
    }
}
