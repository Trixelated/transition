package com.example.slideshow.newslideshow.glstuff;

import android.opengl.GLES20;

import androidx.annotation.NonNull;

import org.intellij.lang.annotations.Language;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


/**
 * A GLSL program to apply transition on images
 *
 * @author Gaurav Jaiswar
 */
public class FilterGLProgram {

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private int mProgram;
    private int mPositionHandle;
    private int mTextureCoordinateHandle;
    private int mMVPMatrixHandle;
    private int mTextureUniformHandle, sTextureUniformHandle;
    private final int COORDS_PER_VERTEX = 3;
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    private final float[] rectCoords = {
            -1.0f, 1.0f, 0.0f, // top left
            -1.0f, -1.0f, 0.0f, // bottom left
            1.0f, -1.0f, 0.0f, // bottom right
            1.0f, 1.0f, 0.0f  // top right
    };

    private final float[] textureCoords = {
            0.0f, 1.0f, // Top left
            0.0f, 0.0f, // Bottom left
            1.0f, 0.0f, // Bottom right
            1.0f, 1.0f  // Top right
    };

    private final String vertexShaderCode = "attribute vec4 vPosition;" +
            "attribute vec2 aTexCoordinate;" +
            "uniform mat4 uMVPMatrix;" +
            "varying vec2 vTexCoordinate;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  vTexCoordinate = aTexCoordinate;" +
            "}";

    private final String burnShader = "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform sampler2D sTexture;\n" +
            "varying vec2 vTexCoordinate;\n" +
            "uniform float progress;\n" +
            "\n" +
            "vec3 color = vec3(0.9,0.4,0.2);\n" +
            "\n" +
            "vec4 getFromColor(vec2 uv){\n" +
            "    return texture2D(uTexture, vec2(uv.x, 1.0 - uv.y));\n" +
            "}\n" +
            "\n" +
            "vec4 getToColor(vec2 uv){\n" +
            "    return texture2D(sTexture, vec2(uv.x, 1.0 - uv.y));\n" +
            "}\n" +
            "\n" +
            "vec4 transition(vec2 uv){\n" +
            "    return mix(\n" +
            "    getFromColor(uv) + vec4(progress*color,1.0),\n" +
            "    getToColor(uv) + vec4((1.0- progress) * color,1.0),\n" +
            "    progress\n" +
            "    );" +
            "\n" +
            "}\n" +
            "\n" +
            "void main(){\n" +
            "    gl_FragColor = transition(vTexCoordinate);\n" +
            "}\n";

    @Language("GLSL")
    private final String mixShader = "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform sampler2D sTexture;\n" +
            "varying vec2 vTexCoordinate;\n" +
            "uniform float progress;\n" +
            "uniform highp vec3 iResolution;\n" +
            "\n" +
            "vec4 getFromColor(vec2 uv){\n" +
            "    return texture2D(uTexture, vec2(uv.x, 1.0 - uv.y));\n" +
            "}\n" +
            "\n" +
            "vec4 getToColor(vec2 uv){\n" +
            "    return texture2D(sTexture, vec2(uv.x, 1.0 - uv.y));\n" +
            "}\n" +
            "\n" +
            "float mapProgress(float startPoint, float endPoint){\n" +
            "    return (progress - startPoint) / (endPoint - startPoint);\n" +
            "}\n" +
            "\n" +
            "vec4 burn(vec2 uv, float progress){\n" +
            "    vec3 color = vec3(0.3, 0.2, 0.5);\n" +
            "    return mix(getToColor(uv) + vec4(progress*color, 1.0), getToColor(uv) + vec4((1.0- progress) * color, 1.0), progress);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "const float PI = 3.14159265358979;\n" +
            "const float rad = 120.0;\n" +
            "float z = 1.5;\n" +
            "\n" +
            "\n" +
            "float dist(){\n" +
            "    float scale = 2.0;\n" +
            "    \n" +
            "    return scale/10.;\n" +
            "}\n" +
            "\n" +
            "float deg(){\n" +
            "    return rad / 180. * PI;\n" +
            "}\n" +
            "\n" +
            "vec2 refl(vec2 p, vec2 o, vec2 n)" +
            "{\n" +
            "    return 2.0 * o + 2.0 * n * dot(p - o, n) - p;\n" +
            "}\n" +
            "\n" +
            "vec2 rot(vec2 p, vec2 o, float a)" +
            "{\n" +
            "    float s = sin(a);\n" +
            "    float c = cos(a);\n" +
            "    return o + mat2(c, -s, s, c) * (p - o);\n" +
            "}\n" +
            "\n" +
            "vec4 mainImage(vec2 uv,float progress)\n" +
            "{\n" +
            "    float ratio =1.0;\n" +
            "    float speed = 5.0;\n" +
            "    \n" +
            "    vec2 uv0 = uv;\n" +
            "    uv -= 0.5;\n" +
            "    uv.x *= ratio;\n" +
            "    uv *= z;\n" +
            "    uv = rot(uv, vec2(0.0), progress * speed);\n" +
            "    float theta = progress * 6. + PI / .5;\n" +
            "    for (int iter = 0; iter < 10; iter++) {\n" +
            "        for (float i = 0.; i < 2. * PI; i += deg()) {\n" +
            "            float ts = sign(asin(cos(i))) == 1.0 ? 1.0 : 0.0;\n" +
            "            if (((ts == 1.0) && (uv.y - dist() * cos(i) > tan(i) * (uv.x + dist() * + sin(i)))) || ((ts == 0.0) && (uv.y - dist() * cos(i) < tan(i) * (uv.x + dist() * + sin(i))))) {\n" +
            "                uv = refl(vec2(uv.x + sin(i) * dist() * 2., uv.y - cos(i) * dist() * 2.), vec2(0., 0.), vec2(cos(i), sin(i)));\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    uv += 0.5;\n" +
            "    uv = rot(uv, vec2(0.5), progress * -speed);\n" +
            "    uv -= 0.5;\n" +
            "    uv.x /= ratio;\n" +
            "    uv += 0.5;\n" +
            "    uv = 2. * abs(uv / 2. - floor(uv / 2. + 0.5));\n" +
            "    vec2 uvMix = mix(uv, uv0, cos(progress * PI * 2.) / 2. + 0.5);\n" +
            "    vec4 color = mix(getFromColor(uvMix), getToColor(uvMix), cos((progress - 1.) * PI) / 2. + 0.5);\n" +
            "    return color;\n" +
            "\n" +
            "}\n" +
            "float strength = 0.4;\n" +
            "\n" +
            "\n" +
            "float Linear_ease(in float begin, in float change, in float duration, in float time) {\n" +
            "    return change * time / duration + begin;\n" +
            "}\n" +
            "\n" +
            "float Exponential_easeInOut(in float begin, in float change, in float duration, in float time) {\n" +
            "    if (time == 0.0)\n" +
            "    return begin;\n" +
            "    else if (time == duration)\n" +
            "    return begin + change;\n" +
            "    time = time / (duration / 2.0);\n" +
            "    if (time < 1.0)\n" +
            "    return change / 2.0 * pow(2.0, 10.0 * (time - 1.0)) + begin;\n" +
            "    return change / 2.0 * (-pow(2.0, -10.0 * (time - 1.0)) + 2.0) + begin;\n" +
            "}\n" +
            "\n" +
            "float Sinusoidal_easeInOut(in float begin, in float change, in float duration, in float time) {\n" +
            "    return -change / 2.0 * (cos(PI * time / duration) - 1.0) + begin;\n" +
            "}\n" +
            "\n" +
            "float rand(vec2 co) {\n" +
            "    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);\n" +
            "}\n" +
            "\n" +
            "vec3 crossFade(in vec2 uv, in float dissolve) {\n" +
            "    return mix(getFromColor(uv).rgb, getToColor(uv).rgb, dissolve);\n" +
            "}\n" +
            "\n" +
            "vec4 crossFadeZoom(vec2 uv,float progress) {\n" +
            "    vec2 texCoord = uv.xy / vec2(1.0).xy;\n" +
            "    \n" +
            "    vec2 center = vec2(Linear_ease(0.25, 0.5, 1.0, progress), 0.5);\n" +
            "    float dissolve = Exponential_easeInOut(0.0, 1.0, 1.0, progress);\n" +
            "\n" +
            "    \n" +
            "    float strength = Sinusoidal_easeInOut(0.0, strength, 0.5, progress);\n" +
            "\n" +
            "    vec3 color = vec3(0.0);\n" +
            "    float total = 0.0;\n" +
            "    vec2 toCenter = center - texCoord;\n" +
            "\n" +
            "    \n" +
            "    float offset = rand(uv);\n" +
            "\n" +
            "    for (float t = 0.0; t <= 40.0; t++) {\n" +
            "        float percent = (t + offset) / 40.0;\n" +
            "        float weight = 4.0 * (percent - percent * percent);\n" +
            "        color += crossFade(texCoord + toCenter * percent * strength, dissolve) * weight;\n" +
            "        total += weight;\n" +
            "    }\n" +
            "    return vec4(color / total, 1.0);\n" +
            "}\n" +
            "\n" +
            "float zoom_quickness = 0.8;\n" +
            "float nQuick(){\n" +
            "    return clamp(zoom_quickness, 0.2, 1.0);\n" +
            "}\n" +
            "vec2 zoom(vec2 uv, float amount) {\n" +
            "    return 0.5 + ((uv - 0.5) * (1.0 - amount));\n" +
            "}\n" +
            "\n" +
            "vec4 transitionZoom(vec2 uv,float progress) {\n" +
            "    return mix(\n" +
            "    getFromColor(zoom(uv, smoothstep(0.0, nQuick(), progress))),\n" +
            "    getToColor(uv),\n" +
            "    smoothstep(nQuick() - 0.2, 1.0, progress)\n" +
            "    );\n" +
            "}\n" +
            "\n" +
            "\n" +
            "vec4 effectRiver(vec2 uv)\n" +
            "{\n" +
            "    vec2 p = 2.0 * uv - 1.0;\n" +
            "    vec3 col = vec3(0);\n" +
            "    float t = progress *5.0;\n" +
            "    col += vec3(1) * 1.0 / (1.0 + 60.0 * abs(p.y + sin(p.x * 4.0 + t) * 0.3) + abs(sin(p.x * 2.3 + t * 1.4) * 20.));\n" +
            "    t += 1.56;\n" +
            "    col += vec3(1) * 1.0 / (1.0 + 60.0 * abs(p.y + sin(p.x * 5.0 + t) * 0.3) + abs(sin(p.x * 2.3 + t * 1.4) * 20.));\n" +
            "    t += 3.134;\n" +
            "    col += vec3(1) * 1.0 / (1.0 + 60.0 * abs(p.y + sin(p.x * 6.0 + t) * 0.3) + abs(sin(p.x * 2.3 + t * 1.4) * 20.));\n" +
            "    col.g -= col.r;\n" +
            "    return vec4(col, 1.0);\n" +
            "}\n" +
            "vec4 transition(vec2 uv) {\n" +
            "    if(progress>=0.0 && progress<=0.05){\n" +
            "       return getFromColor(uv);\n" +
            "    } else if(progress>=0.05 && progress<=0.15){\n" +
            "        return transitionZoom(uv,mapProgress(0.05,0.15));\n" +
            "    }else if(progress>=0.15 && progress<=0.4){\n" +
            "        return mainImage(uv,mapProgress(0.15,0.4));\n" +
            "    }else if(progress>=0.4 && progress<=0.61){\n" +
            "        return crossFadeZoom(uv,mapProgress(0.4,0.61));\n" +
            "     }\n" +
            "    return getToColor(uv)+effectRiver(uv);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "void main(){\n" +
            "    gl_FragColor = transition(vTexCoordinate);\n" +
            "}\n";

    /*private final String mixShader = "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform sampler2D sTexture;\n" +
            "varying vec2 vTexCoordinate;\n" +
            "uniform float progress;\n" +
            "uniform highp vec3 iResolution;\n" +
            "\n" +
            "vec4 getFromColor(vec2 uv){\n" +
            "    return texture2D(uTexture, vec2(uv.x, 1.0 - uv.y));\n" +
            "}\n" +
            "\n" +
            "vec4 getToColor(vec2 uv){\n" +
            "    return texture2D(sTexture, vec2(uv.x, 1.0 - uv.y));\n" +
            "}\n" +
            "\n" +
            "float zoom_quickness = 0.8;\n" +
            "\n" +
            "float nQuick(){\n" +
            "    return clamp(zoom_quickness, 0.2, 1.0);\n" +
            "}\n" +
            "\n" +
            "vec2 zoom(vec2 uv, float amount) {\n" +
            "    return 0.5 + ((uv - 0.5) * (1.0 - amount));\n" +
            "}\n" +
            "\n" +
            "float amplitude = 1.0;\n" +
            "float waves = 30.0;\n" +
            "float colorSeparation = 0.3;\n" +
            "float PI = 3.14159265358979323846264;\n" +
            "\n" +
            "float compute(vec2 p, float progress, vec2 center) {\n" +
            "    vec2 o = p * sin(progress * amplitude) - center;\n" +
            "    // horizontal vector\n" +
            "    vec2 h = vec2(1., 0.);\n" +
            "    // butterfly polar function (don't ask me why this one :))\n" +
            "    float theta = acos(dot(o, h)) * waves;\n" +
            "    return (exp(cos(theta)) - 2. * cos(4. * theta) + pow(sin((2. * theta - PI) / 24.), 5.)) / 10.;\n" +
            "}\n" +
            "\n" +
            "vec4 scale(in vec2 uv) {\n" +
            "    uv = 0.5 + (uv - 0.5) * progress;\n" +
            "    return getToColor(uv);\n" +
            "}\n" +
            "\n" +
            "vec4 effect(vec2 uv) {\n" +
            "    float x;\n" +
            "    if (uv.x >= 0.0 && uv.x <= 0.5) {\n" +
            "        x = uv.x + 0.25;\n" +
            "    }else {\n" +
            "        x = uv.x - 0.25;\n" +
            "    }\n" +
            "\n" +
            "    return texture2D(uTexture, vec2(x, uv.y));\n" +
            "}\n" +
            "\n" +
            "vec4 effect2(vec2 uv) {\n" +
            "    float y;\n" +
            "    if (uv.y >= 0.0 && uv.y <= 0.5) {\n" +
            "        y = uv.y + 0.25;\n" +
            "    } else {\n" +
            "        y = uv.y - 0.25;\n" +
            "    }\n" +
            "    return texture2D(uTexture, vec2(uv.x, y));\n" +
            "}\n" +
            "\n" +
            "vec4 effect3(vec2 uv) {\n" +
            "    if (uv.x <= 0.5) {\n" +
            "        uv.x = uv.x * 2.0;\n" +
            "    } else {\n" +
            "        uv.x = (uv.x - 0.5) * 2.0;\n" +
            "    }\n" +
            "    if (uv.y <= 0.5) {\n" +
            "        uv.y = uv.y * 2.0;\n" +
            "    } else {\n" +
            "        uv.y = (uv.y - 0.5) * 2.0;\n" +
            "    }\n" +
            "\n" +
            "    return texture2D(uTexture, uv);\n" +
            "}\n" +
            "\n" +
            "vec4 effect4(vec2 uv) {\n" +
            "    if (uv.x <= 1.0 / 3.0) {\n" +
            "        uv.x = uv.x * 3.0;\n" +
            "    } else if (uv.x <= 2.0 / 3.0) {\n" +
            "        uv.x = (uv.x - 1.0 / 3.0) * 3.0;\n" +
            "    } else {\n" +
            "        uv.x = (uv.x - 2.0 / 3.0) * 3.0;\n" +
            "    }\n" +
            "\n" +
            "    if (uv.y <= 1.0 / 3.0) {\n" +
            "        uv.y = uv.y * 3.0;\n" +
            "    } else if (uv.y <= 2.0 / 3.0) {\n" +
            "        uv.y = (uv.y - 1.0 / 3.0) * 3.0;\n" +
            "    } else {\n" +
            "        uv.y = (uv.y - 2.0 / 3.0) * 3.0;\n" +
            "    }\n" +
            "    return texture2D(uTexture, uv);\n" +
            "}\n" +
            "\n" +
            "vec4 effect5(vec2 uv) {\n" +
            "    float waveu = sin((uv.y + progress) * 20.0) * 0.08;\n" +
            "    return texture2D(uTexture, uv + vec2(waveu, 0.0));\n" +
            "}\n" +
            "\n" +
            "vec4 effect6(vec2 uv) {\n" +
            "    float waveu = sin((uv.y + progress) * 20.0) * 0.015;\n" +
            "    float duration = 0.7;\n" +
            "    float maxAlpha = 0.4;\n" +
            "    float maxScale = 1.8;\n" +
            "\n" +
            "    float mProgress = mod(progress, duration) / duration;\n" +
            "    float alpha = maxAlpha * (1.0 - mProgress);\n" +
            "    float scale = 1.0 + (maxScale - 1.0) * mProgress;\n" +
            "    \n" +
            "    float weakX = 0.5 + (uv.x - 0.5) / scale;\n" +
            "    float weakY = 0.5 + (uv.y - 0.5) / scale;\n" +
            "    vec2 weakTextureCoords = vec2(weakX, weakY);\n" +
            "\n" +
            "\n" +
            "    vec4 weakMask = texture2D(uTexture, weakTextureCoords);\n" +
            "    vec4 mask = texture2D(uTexture, uv);\n" +
            "    \n" +
            "    " +
            "return texture2D(uTexture, uv + vec2(waveu, 0)) + weakMask * alpha;\n" +
            "}\n" +
            "\n" +
            "vec4 soulZoom(vec2 uv) {\n" +
            "    \n" +
            "    float duration = 0.066666;\n" +
            "    float maxAlpha = 0.4;\n" +
            "    float maxScale = 1.8;\n" +
            "\n" +
            "    float mProgress = mod(progress, duration) / duration; // 0~1\n" +
            "    float alpha = maxAlpha * (1.0 - mProgress);\n" +
            "    float scale = 1.0 + (maxScale - 1.0) * mProgress;\n" +
            "\n" +
            "    float weakX = 0.5 + (uv.x - 0.5) / scale;\n" +
            "    float weakY = 0.5 + (uv.y - 0.5) / scale;\n" +
            "    vec2 weakTextureCoords = vec2(weakX, weakY);\n" +
            "\n" +
            "    vec4 weakMask = texture2D(uTexture, weakTextureCoords);\n" +
            "\n" +
            "    vec4 mask = texture2D(uTexture, uv);\n" +
            "\n" +
            "    return mask * (1.0 - alpha) + weakMask * alpha;\n" +
            "}\n" +
            "\n" +
            "vec4 rotateFade(vec2 uv){\n" +
            "    vec2 center = vec2(0.5, 0.5);\n" +
            "    float rotations = 1.0;\n" +
            "    float scale = 1.5;\n" +
            "    vec4 backColor = vec4(0.15, 0.15, 0.15, 1.0);\n" +
            " " +
            "   \n" +
            "    vec2 difference = uv - center;\n" +
            "    vec2 dir = normalize(difference);\n" +
            "    float dist = length(difference);\n" +
            " " +
            " " +
            "  \n" +
            "    float mProgress = (progress - 0.4) / (0.6 - 0.4);\n" +
            "\n" +
            "    float angle = 2.0 * PI * rotations * mProgress;\n" +
            "\n" +
            "    float c = cos(angle);\n" +
            "    float s = sin(angle);\n" +
            "\n" +
            "    float currentScale = mix(scale, 1.0, 2.0 * abs(mProgress - 0.5));\n" +
            "    vec2 rotatedDir = vec2(dir.x * c - dir.y * s, dir.x * s + dir.y * c);\n" +
            "    vec2 rotatedUv = center + rotatedDir * dist / currentScale;\n" +
            "\n" +
            "    if (rotatedUv.x < 0.0 || rotatedUv.x > 1.0 ||\n" +
            "    rotatedUv.y < 0.0 || rotatedUv.y > 1.0)\n" +
            "    return backColor;\n" +
            "\n" +
            "    return mix(getFromColor(rotatedUv), getToColor(rotatedUv), mProgress);\n" +
            "}\n" +
            "\n" +
            "vec4 wipeRight(vec2 uv,bool flip) {\n" +
            "    float duration = 0.2;\n" +
            "    float mProgress = mod(progress, duration) / duration;\n" +
            "    \n" +
            "    vec2 p = uv.xy / vec2(1.0).xy;\n" +
            "    vec4 a;\n" +
            "    vec4 b;\n" +
            "    if(flip){\n" +
            "        a = getToColor(p);\n" +
            "        b = getFromColor(p);\n" +
            "    }else {\n" +
            "        a = getFromColor(p);\n" +
            "        b = getToColor(p);\n" +
            "    }\n" +
            "    return mix(a, b, step(0.0 + p.x, mProgress));\n" +
            "}\n" +
            "\n" +
            "vec4 wipeLeft(vec2 uv, bool flip) {\n" +
            "    float duration = 0.2;\n" +
            "    float mProgress = mod(progress, duration) / duration;\n" +
            "    \n" +
            "    vec2 p = uv.xy / vec2(1.0).xy;\n" +
            "    \n" +
            "    vec4 a;\n" +
            "    vec4 b;\n" +
            "    if(flip){\n" +
            "        a = getToColor(p);\n" +
            "        b = getFromColor(p);\n" +
            "    }else {\n" +
            "        a = getFromColor(p);\n" +
            "        b = getToColor(p);\n" +
            "    }\n" +
            "    \n" +
            "    return mix(a, b, step(1.0 - p.x, mProgress));\n" +
            "}\n" +
            "\n" +
            "vec4 morph(vec2 uv){\n" +
            "    float strength = 0.1;\n" +
            "    vec4 ca = getFromColor(uv);\n" +
            "    vec4 cb = getToColor(uv);\n" +
            "\n" +
            "    vec2 oa = (((ca.rg + ca.b)*0.5)*2.0-1.0);\n" +
            "    vec2 ob = (((cb.rg + cb.b)*0.5)*2.0-1.0);\n" +
            "    vec2 oc = mix(oa,ob,0.5) * strength;\n" +
            "    float w0 = progress;\n" +
            "    float w1 = 1.0-w0;\n" +
            "\n" +
            "    return mix(getFromColor(uv+oc*w0), getToColor(uv-oc*w1), progress);\n" +
            "}\n" +
            "\n" +
            "float inHeart(vec2 p, vec2 center, float size) {\n" +
            "    if (size == 0.0) return 0.0;\n" +
            "    vec2 o = (p - center) / (1.6 * size);\n" +
            "    float a = o.x * o.x + o.y * o.y - 0.3;\n" +
            "    return step(a * a * a, o.x * o.x * o.y * o.y * o.y);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "\n" +
            "int OCTAVES = 5;\n" +
            "\n" +
            "vec3 mod289(vec3 x)" +
            "{\n" +
            "    return x - floor(x * (1.0 / 289.0)) * 289.0;\n" +
            "}\n" +
            "\n" +
            "vec2 mod289(vec2 x)" +
            "{\n" +
            "    return x - floor(x * (1.0 / 289.0)) * 289.0;\n" +
            "}\n" +
            "\n" +
            "vec3 permute(vec3 x)" +
            "{\n" +
            "    return mod289(((x * 34.0) + 1.0) * x);\n" +
            "}\n" +
            "\n" +
            "float snoise(vec2 v)\n" +
            "{\n" +
            "    const vec4 C = vec4(0.211324865405187,\n" +
            "    0.366025403784439,\n" +
            "    -0.577350269189626,\n" +
            "    0.024390243902439);\n" +
            "    vec2 i = floor(v + dot(v, C.yy));\n" +
            "    vec2 x0 = v - i + dot(i, C.xx);\n" +
            "\n" +
            "    vec2 i1;\n" +
            "    i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);\n" +
            "    vec4 x12 = x0.xyxy + C.xxzz;\n" +
            "    x12.xy -= i1;\n" +
            "\n" +
            "    i = mod289(i);\n" +
            "    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0))\n" +
            "    + i.x + vec3(0.0, i1.x, 1.0));\n" +
            "\n" +
            "    vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);\n" +
            "    m = m * m;\n" +
            "    m = m * m;\n" +
            "\n" +
            "    vec3 x = 2.0 * fract(p * C.www) - 1.0;\n" +
            "    vec3 h = abs(x) - 0.5;\n" +
            "    vec3 ox = floor(x + 0.5);\n" +
            "    vec3 a0 = x - ox;\n" +
            "\n" +
            "    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);\n" +
            "    vec3 g;\n" +
            "    g.x = a0.x * x0.x + h.x * x0.y;\n" +
            "    g.yz = a0.yz * x12.xz + h.yz * x12.yw;\n" +
            "    return 130.0 * dot(m, g);\n" +
            "}\n" +
            "\n" +
            "vec2 rand2(vec2 p)\n" +
            "{\n" +
            "    p = vec2(dot(p, vec2(12.9898, 78.233)), dot(p, vec2(26.65125, 83.054543)));\n" +
            "    return fract(sin(p) * 43758.5453);\n" +
            "}\n" +
            "\n" +
            "float rand(vec2 p)\n" +
            "{\n" +
            "    return fract(sin(dot(p.xy, vec2(54.90898, 18.233))) * 4337.5453);\n" +
            "}\n" +
            "\n" +
            "vec3 hsv2rgb(vec3 c)\n" +
            "{\n" +
            "    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
            "    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
            "    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
            "}\n" +
            "\n" +
            "float stars(in vec2 x, float numCells, float size, float br)\n" +
            "{\n" +
            "    vec2 n = x * numCells;\n" +
            "    vec2 f = floor(n);\n" +
            "\n" +
            "    float d = 1.0e10;\n" +
            "    for (int i = -1; i <= 1; ++i)\n" +
            "    {\n" +
            "        for (int j = -1; j <= 1; ++j)\n" +
            "        {\n" +
            "            vec2 g = f + vec2(float(i), float(j));\n" +
            "            g = n - g - rand2(mod(g, numCells)) + rand(g);\n" +
            "            g *= 1. / (numCells * size);\n" +
            "            d = min(d, dot(g, g));\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    return br * (smoothstep(.95, 1., (1. - sqrt(d))));\n" +
            "}\n" +
            "\n" +
            "float fractalNoise(in vec2 coord, in float persistence, in float lacunarity)\n" +
            "{\n" +
            "    float n = 0.;\n" +
            "    float frequency = 1.;\n" +
            "    float amplitude = 1.;\n" +
            "    for (int o = 0; o < OCTAVES; ++o)\n" +
            "    {\n" +
            "        n += amplitude * snoise(coord * frequency);\n" +
            "        amplitude *= persistence;\n" +
            "        frequency *= lacunarity;\n" +
            "    }\n" +
            "    return n;\n" +
            "}\n" +
            "\n" +
            "vec3 fractalNebula(in vec2 coord, vec3 color, float transparency)\n" +
            "{\n" +
            "    float n = fractalNoise(coord, .5, 2.);\n" +
            "    return n * color * transparency;\n" +
            "}\n" +
            "\n" +
            "vec4 sparkel(vec2 uv){\n" +
            "    float res = max(iResolution.x, iResolution.y);\n" +
            "    vec2 coord = gl_FragCoord.xy / res;\n" +
            "    vec3 result = vec3(0.);\n" +
            "    vec3 nebulaColor1 = hsv2rgb(vec3(.5 + .5 * sin(progress * 20.1), 0.5, .25));\n" +
            "    result += fractalNebula(coord + vec2(.1, .1), nebulaColor1, 1.);\n" +
            "    result += stars(coord + 1., 4., 0.5, 2.) * vec3(.55, .55, .55);\n" +
            "    result += stars(coord + 0.05, 8., 0.4, 1.) * vec3(.97, .55, .55);\n" +
            "    vec4 effect = vec4(result, 1.0);\n" +
            "\n" +
            "    return getToColor(uv) + effect;\n" +
            "}\n" +
            "\n" +
            "vec4 circleOpen(vec2 uv){\n" +
            "    float smoothness =0.3;\n" +
            "    bool opening = true;\n" +
            "    vec2 center = vec2(0.5, 0.5);\n" +
            "    float SQRT_2 = 1.414213562373;\n" +
            "\n" +
            "    float mProgress = (progress - 0.2) / (0.6 - 0.2);\n" +
            "\n" +
            "    float x = opening ? mProgress : 1.-mProgress;\n" +
            "    float m = smoothstep(-smoothness, 0.0, SQRT_2*distance(center, uv) - x*(1.+smoothness));\n" +
            "    return mix(getFromColor(uv), getToColor(uv), opening ? 1.-m : m);\n" +
            "}\n" +
            "\n" +
            "vec4 effectRiver(vec2 uv)\n" +
            "{\n" +
            "    vec2 p = 2.0 * uv - 1.0;\n" +
            "    vec3 col = vec3(0);\n" +
            "    float t = progress;\n" +
            "    col += vec3(1) * 1.0 / (1.0 + 60.0 * abs(p.y + sin(p.x * 4.0 + t) * 0.3) + abs(sin(p.x * 2.3 + t * 1.4) * 20.));\n" +
            "    t += 1.56;\n" +
            "    col += vec3(1) * 1.0 / (1.0 + 60.0 * abs(p.y + sin(p.x * 5.0 + t) * 0.3) + abs(sin(p.x * 2.3 + t * 1.4) * 20.));\n" +
            "    t += 3.134;\n" +
            "    col += vec3(1) * 1.0 / (1.0 + 60.0 * abs(p.y + sin(p.x * 6.0 + t) * 0.3) + abs(sin(p.x * 2.3 + t * 1.4) * 20.));\n" +
            "    col.g -= col.r;\n" +
            "    return vec4(col, 1.0);\n" +
            "}\n" +
            "\n" +
            "vec4 transition(vec2 uv) {\n" +
            "   return effectRiver(uv)+getToColor(uv);\n" +
            "}\n" +
            "\n" +
            "void main(){\n" +
            "    gl_FragColor = transition(vTexCoordinate);\n" +
            "}\n";*/


    //glass fade
    /*float size = 0.04;
float zoom = 50.0;
float colorSeparation = 0.3;

vec4 transition(vec2 p,float progress) {
    float inv = 1. - progress;
    vec2 disp = size * vec2(cos(zoom * p.x), sin(zoom * p.y));
    vec4 texTo = getToColor(p + inv * disp);
    vec4 texFrom = vec4(
        getFromColor(p + progress * disp * (1.0 - colorSeparation)).r,
        getFromColor(p + progress * disp).g,
        getFromColor(p + progress * disp * (1.0 + colorSeparation)).b,
        1.0);
    return texTo * progress + texFrom * inv;
}*/

    //wave fade
    /*vec2 offset(float progress, float x, float theta) {
    float phase = progress * progress + progress + theta;
    float shifty = 0.03 * progress * cos(10.0 * (progress + x));
    return vec2(0, shifty);
}

vec4 transition(vec2 p,float progress) {
    return mix(getFromColor(p + offset(progress, p.x, 0.0)), getToColor(p + offset(1.0 - progress, p.x, 3.14)), progress);
}*/

    //shake glitch effect
    /*float druction = 5000.0;
float nowtime = 0.0;
float ratio =3.0/4.0;
float movexred = 0.01;
float value = 0.0;
float centerys[11];
float usepro = 5.0;

float Rand(vec2 v) {
    return fract(sin(dot(v.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

float getvalue() {
    value = 0.05 + value + Rand(vec2(usepro, ratio + value)) / 8.0;
    float zhengfu = Rand(vec2(usepro, ratio + value)) > 0.5 ? -1.0 : 1.0;
    return value * zhengfu;
}

float getnearx(float posy) {
    posy += Rand(vec2(usepro * nowtime, nowtime));
    posy = fract(posy);

    float rtn = 0.0;
    for (int i = 1;i < 11; i++) {
        if (posy < abs(centerys[i])) {
            float centery = abs(centerys[i]) + abs(centerys[i - 1]);
            centery /= 2.0;
            float cha = centery - abs(centerys[i - 1]);
            rtn = posy - centery;
            rtn = sqrt(cha * cha - rtn * rtn) / 8.0;
            if (centerys[i] < 0.0) {
                rtn *= -1.0;
            }
            break;
        }
    }
    return rtn;
}

vec4 effect(vec2 uv) {
    nowtime = mod(progress, druction) / druction;
    centerys[0] = 0.0;
    centerys[1] = getvalue();
    centerys[2] = getvalue();
    centerys[3] = getvalue();
    centerys[4] = getvalue();
    centerys[5] = getvalue();
    centerys[6] = getvalue();
    centerys[7] = getvalue();
    centerys[8] = getvalue();
    centerys[9] = getvalue();
    centerys[10] = getvalue();


    vec2 texCoord = uv.xy / vec2(1.0).xy;
    vec4 colorgb = getFromColor(texCoord);

    float startmovex = getnearx(texCoord.y);

    movexred = movexred / ratio;
    float offx = 0.0;
    bool isin = fract(nowtime * 4.0 - texCoord.y) < 0.05;
    if (isin) {
        offx = Rand(vec2(nowtime, texCoord.y));
        offx = offx > 0.5 ? offx : -offx;
    }

    texCoord.x = texCoord.x + offx / 100.0 + startmovex;

    if (isin) {
        float sp = 10.0 * ratio;
        float cheng = texCoord.x * sp;
        float weiba = fract(cheng);
        float usex = cheng - weiba;
        usex /= sp;
        offx = Rand(vec2(usex * texCoord.y, texCoord.y * 1000.0));
        float panduan = 0.96;
        if (fract(nowtime * 50.0) > 0.1) {
            panduan = 0.99;
        }
        if (offx > panduan) {
            return mix(vec4(1.0), getFromColor(texCoord), weiba);
        }
    }

    texCoord.x = texCoord.x + startmovex;
    vec4 colorr = getFromColor(texCoord);
    float random = Rand(vec2(nowtime));
    if (random < 0.3) {
        colorgb.r = colorr.r;
    } else if (random < 0.5) {
        colorgb.g = colorr.g;
    } else {
        colorgb.b = colorr.b;
    }
    return colorgb;
}*/

    //shake glitch transition
    /*vec4 transitionGlitch(vec2 p,float progress){
    vec2 block = floor(p.xy / vec2(16));
    vec2 uv_noise = block / vec2(64);
    uv_noise += floor(vec2(progress) * vec2(1200.0, 3500.0)) / vec2(64);
    vec2 dist = progress > 0.0 ? (fract(uv_noise) - 0.5) * 0.3 *(1.0 -progress) : vec2(0.0);
    vec2 red = p + dist * 0.2;
    vec2 green = p + dist * .3;
    vec2 blue = p + dist * .5;

    return vec4(
    mix(getFromColor(red), getToColor(red), progress).r,
    mix(getFromColor(green), getToColor(green), progress).g,
    mix(getFromColor(blue), getToColor(blue), progress).b,
    1.0
    );

}
    * */

    //crossFadeZoom
    /*float strength = 0.4;

const float PI = 3.141592653589793;

float Linear_ease(in float begin, in float change, in float duration, in float time) {
    return change * time / duration + begin;
}

float Exponential_easeInOut(in float begin, in float change, in float duration, in float time) {
    if (time == 0.0)
    return begin;
    else if (time == duration)
    return begin + change;
    time = time / (duration / 2.0);
    if (time < 1.0)
    return change / 2.0 * pow(2.0, 10.0 * (time - 1.0)) + begin;
    return change / 2.0 * (-pow(2.0, -10.0 * (time - 1.0)) + 2.0) + begin;
}

float Sinusoidal_easeInOut(in float begin, in float change, in float duration, in float time) {
    return -change / 2.0 * (cos(PI * time / duration) - 1.0) + begin;
}

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 crossFade(in vec2 uv, in float dissolve) {
    return mix(getFromColor(uv).rgb, getToColor(uv).rgb, dissolve);
}

vec4 crossFadeZoom(vec2 uv,float progress) {
    vec2 texCoord = uv.xy / vec2(1.0).xy;
    // Linear interpolate center across center half of the image
    vec2 center = vec2(Linear_ease(0.25, 0.5, 1.0, progress), 0.5);
    float dissolve = Exponential_easeInOut(0.0, 1.0, 1.0, progress);

    // Mirrored sinusoidal loop. 0->strength then strength->0
    float strength = Sinusoidal_easeInOut(0.0, strength, 0.5, progress);

    vec3 color = vec3(0.0);
    float total = 0.0;
    vec2 toCenter = center - texCoord;

    //randomize the lookup values to hide the fixed number of samples
    float offset = rand(uv);

    for (float t = 0.0; t <= 40.0; t++) {
        float percent = (t + offset) / 40.0;
        float weight = 4.0 * (percent - percent * percent);
        color += crossFade(texCoord + toCenter * percent * strength, dissolve) * weight;
        total += weight;
    }
    return vec4(color / total, 1.0);
}*/


    public enum Filters {
        Fade("None"),

        WaterDrop("WaterDrop"),
        Burn("Burn"),
        ColorPhase("Color Phase"),
        FadeColor("Fade Color"),
        DirectionalWipe("Directional Wipe"),
        Directional("Directional"),
        WipeRight("WipeRight"),
        DreamyZoom("DreamyZoom"),
        Circle("Circle"),
        CircleOpen("Circle Open"),
        Perlin("Perlin"),
        RotateScaleFade("Rotate scale fade"),
        PolarFunction("Polar Function"),
        Heart("Heart"),
        WindowSlice("WindowSlice"),
        DoorWay("Door Way"),
        SimpleZoom("SimpleZoom"),
        Wind("Wind"),
        PinWheel("Pin Wheel"),
        Squeeze("Squeeze"),
        DoomScreen("Doom Screen"),
        GlitchMemory("GlitchMemories"),
        GlitchDisplace("Glitch Displace"),
        Kaleidoscope("Kaleidoscope"),
        Morph("Morph"),
        Bounce("Bounce"),
        Swap("Swap"),
        Cube("Cube"),
        Hexagonalize("Hexagonalize"),
        Pixelize("Pixelize"),
        DirectionalWrap("DirectionalWrap"),
        Mosaic("Mosaic"),
        GridFlip("GridFlip");
//        MixTest("None");

        private final String assetPath;

        /**
         * @param assetPath path from asset/thumb (case-sensitive)
         */
        Filters(String assetPath) {
            this.assetPath = assetPath;
        }

        public String getAssetPath() {
            return assetPath;
        }
    }

    public FilterGLProgram(@NonNull Filters filter) {

        ByteBuffer bb = ByteBuffer.allocateDirect(rectCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(rectCoords);
        vertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        tb.order(ByteOrder.nativeOrder());
        textureBuffer = tb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        String fragmentShader = getFragmentShader(filter);
        /*if (filter == Filters.MixTest) {
            fragmentShader = mixShader;
        }*/
        mProgram = GlUtil.createProgram(vertexShaderCode, fragmentShader);

        GlUtil.checkGlError("creating program");

        if (mProgram <= 0) {
            throw new RuntimeException("failed to create program " + fragmentShader);
        }

    }

    @NonNull
    private String getFragmentShader(@NonNull Filters filter) {


        StringBuilder shaderBuilder = new StringBuilder();

        shaderBuilder
                .append("#define DEG2RAD 0.03926990816987241548078304229099\n")
                .append("#define PI 3.14159265358979323\n")
                .append("#define POW2(X) X*X\n")
                .append("#define POW3(X) X*X*X\n")
                .append("precision mediump float;\n")
                .append("uniform sampler2D uTexture;\n")
                .append("uniform sampler2D sTexture;\n")
                .append("varying vec2 vTexCoordinate;\n")
                .append("uniform float progress;\n")
                .append("uniform highp vec3 iResolution;\n")
                .append("\n")
                .append("vec4 getFromColor(vec2 uv){\n")
                .append("    return texture2D(uTexture, vec2(uv.x, 1.0 - uv.y));\n")
                .append("}\n")
                .append("\n")
                .append("vec4 getToColor(vec2 uv){\n")
                .append("    return texture2D(sTexture, vec2(uv.x, 1.0 - uv.y));\n")
                .append("}\n")
                .append("\n")
                .append("float mapProgress(float startPoint, float endPoint ){\n" +
                        "    return  (progress - startPoint) / (endPoint - startPoint);\n" +
                        "}")
                .append("\n");


        switch (filter) {
            case Burn: {
                shaderBuilder.append("vec3 color = vec3(0.9,0.4,0.2);\n")
                        .append("vec4 transition(vec2 uv){\n")
                        .append("    return mix(\n")
                        .append("    getFromColor(uv) + vec4(progress*color,1.0),\n")
                        .append("    getToColor(uv) + vec4((1.0- progress) * color,1.0),\n")
                        .append("    progress\n")
                        .append("    );")
                        .append("\n")
                        .append("}\n");
                break;
            }
           
            case WaterDrop: {
                shaderBuilder.append("\n")
                        .append("float amplitude =30.0;\n")
                        .append("float speed =30.0;\n")
                        .append("vec4 transition(vec2 p) {\n")
                        .append("    vec2 dir = p - vec2(.5);\n")
                        .append("    float dist = length(dir);\n")
                        .append("    if (dist > progress) {\n")
                        .append("        return mix(getFromColor(p), getToColor(p), progress);\n")
                        .append("    } else {\n")
                        .append("        vec2 offset = dir * sin(dist * amplitude - progress * speed);\n")
                        .append("        return mix(getFromColor(p + offset), getToColor(p), progress);\n")
                        .append("    }\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case ColorPhase: {
                shaderBuilder.append("\n")
                        .append("vec4 fromStep = vec4(0.0, 0.2, 0.4, 0.0);\n")
                        .append("vec4 toStep = vec4(0.6, 0.8, 1.0, 1.0);\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("    vec4 a = getFromColor(uv);\n")
                        .append("    vec4 b = getToColor(uv);\n")
                        .append("    return mix(a, b, smoothstep(fromStep, toStep, vec4(progress)));\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case FadeColor: {
                shaderBuilder.append("\n")
                        .append("vec3 color= vec3(0.0);\n")
                        .append("float colorPhase= 0.4 ;\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("    return mix(\n")
                        .append("    mix(vec4(color, 1.0), getFromColor(uv), smoothstep(1.0-colorPhase, 0.0, progress)),\n")
                        .append("    mix(vec4(color, 1.0), getToColor(uv), smoothstep( colorPhase, 1.0, progress)),\n")
                        .append("    progress);")
                        .append("\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case DirectionalWipe: {
                shaderBuilder.append("\n")
                        .append("vec2 direction = vec2(1.0, -1.0);\n")
                        .append("float smoothness = 0.5;\n")
                        .append("const vec2 center = vec2(0.5, 0.5);\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("    vec2 v = normalize(direction);\n")
                        .append("    v /= abs(v.x)+abs(v.y);\n")
                        .append("    float d = v.x * center.x + v.y * center.y;\n")
                        .append("    float m = (1.0-step(progress, 0.0)) * ")
                        .append("(1.0 - smoothstep(-smoothness, 0.0, v.x * uv.x + v.y * uv.y - (d-0.5+progress*(1.+smoothness))));\n")
                        .append("    return mix(getFromColor(uv), getToColor(uv), m);")
                        .append("\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case Directional: {
                shaderBuilder.append("\n")
                        .append("vec2 direction = vec2(0.0, 1.0);\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("    vec2 p = uv + progress * sign(direction);\n")
                        .append("    vec2 f = fract(p);\n")
                        .append("    return mix(\n")
                        .append("    getToColor(f),\n")
                        .append("    getFromColor(f),\n")
                        .append("    step(0.0, p.y) * step(p.y, 1.0) * step(0.0, p.x) * step(p.x, 1.0)\n")
                        .append("    );")
                        .append("\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case WipeRight: {
                shaderBuilder.append("\n")
                        .append("vec4 transition(vec2 uv) {\n")
                        .append("    vec2 p=uv.xy/vec2(1.0).xy;\n")
                        .append("    vec4 a=getFromColor(p);\n")
                        .append("    vec4 b=getToColor(p);\n")
                        .append("    return mix(a, b, step(0.0+p.x,progress));")
                        .append("\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case DreamyZoom: {
                shaderBuilder.append("\n")
                        .append("float rotation = 6.0;\n")
                        .append("float scale= 1.2;\n")
                        .append("float ratio = 0.50046337;\n")
                        .append("\n")
                        .append("vec4 transition(vec2 uv) {\n")
                        .append("    \n")
                        .append("    float phase = progress < 0.5 ? progress * 2.0 : (progress - 0.5) * 2.0;\n")
                        .append("    float angleOffset = progress < 0.5 ? mix(0.0, rotation * DEG2RAD, phase) : mix(-rotation * DEG2RAD, 0.0, phase);\n")
                        .append("    float newScale = progress < 0.5 ? mix(1.0, scale, phase) : mix(scale, 1.0, phase);\n")
                        .append("\n")
                        .append("    vec2 center = vec2(0, 0);\n")
                        .append("    vec2 assumedCenter = vec2(0.5, 0.5);\n")
                        .append("    vec2 p = (uv.xy - vec2(0.5, 0.5)) / newScale * vec2(ratio, 1.0);\n")
                        .append("    float angle = atan(p.y, p.x) + angleOffset;\n")
                        .append("    float dist = distance(center, p);\n")
                        .append("    p.x = cos(angle) * dist / ratio + 0.5;\n")
                        .append("    p.y = sin(angle) * dist + 0.5;\n")
                        .append("    vec4 c = progress < 0.5 ? getFromColor(p) : getToColor(p);\n")
                        .append("    return c + (progress < 0.5 ? mix(0.0, 1.0, phase) : mix(1.0, 0.0, phase));\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case Circle: {
                shaderBuilder.append("\n")
                        .append("vec2 center=vec2(0.5);\n")
                        .append("vec3 backColor=vec3(0.1, 0.1, 0.1);\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {")
                        .append("\n")
                        .append("    float distance = length(uv - center);\n")
                        .append("    float radius = sqrt(8.0) * abs(progress - 0.5);\n")
                        .append("\n")
                        .append("    if (distance > radius) {\n")
                        .append("        return vec4(backColor, 1.0);\n")
                        .append("    }")
                        .append(" else {\n")
                        .append("        if (progress < 0.5) return getFromColor(uv);\n")
                        .append("        else return getToColor(uv);\n")
                        .append("    }\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case CircleOpen: {
                shaderBuilder.append("\n")
                        .append("float smoothness =0.3;\n")
                        .append("bool opening = true;\n")
                        .append("const vec2 center = vec2(0.5, 0.5);\n")
                        .append("const float SQRT_2 = 1.414213562373;\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("    float x = opening ? progress : 1.-progress;\n")
                        .append("    float m = smoothstep(-smoothness, 0.0, SQRT_2*distance(center, uv) - x*(1.+smoothness));\n")
                        .append("    return mix(getFromColor(uv), getToColor(uv), opening ? 1.-m : m")
                        .append(");\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case Perlin: {
                shaderBuilder
                        .append("float scale = 4.0;\n")
                        .append("float smoothness = 0.01;\n")
                        .append("float seed = 12.9898;\n")
                        .append("float random(vec2 co){\n")
                        .append("    highp float a = seed;\n")
                        .append("    highp float b = 78.233;\n")
                        .append("    highp float c = 43758.5453;\n")
                        .append("    highp float dt= dot(co.xy ,vec2(a,b));\n")
                        .append("    highp float sn= mod(dt,3.14);\n")
                        .append("    return fract(sin(sn) * c);\n")
                        .append("}\n")
                        .append("float noise (in vec2 st) {\n")
                        .append("    vec2 i = floor(st);\n")
                        .append("    vec2 f = fract(st);\n")
                        .append("    float a = random(i);\n")
                        .append("    float b = random(i + vec2(1.0, 0.0));\n")
                        .append("    float c = random(i + vec2(0.0, 1.0));\n")
                        .append("    float d = random(i + vec2(1.0, 1.0));\n")
                        .append("    vec2 u = f*f*(3.0-2.0*f);\n")
                        .append("    return mix(a, b, u.x) +\n")
                        .append("            (c - a)* u.y * (1.0 - u.x) +\n")
                        .append("            (d - b) * u.x * u.y;\n")
                        .append("}\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("  vec4 from;\n")
                        .append("  vec4 to;\n")
                        .append("  from = getFromColor(uv);\n")
                        .append("  to = getToColor(uv);\n")
                        .append("  float n = noise(uv * scale);\n")
                        .append("  float p = mix(-smoothness, 1.0 + smoothness, progress);\n")
                        .append("  float lower = p - smoothness;\n")
                        .append("  float higher = p + smoothness;\n")
                        .append("  float q = smoothstep(lower, higher, n);\n")
                        .append("\n")
                        .append("  return mix( from,   to,  1.0 - q);\n")
                        .append("\n")
                        .append("}");
                break;
            }
            case RotateScaleFade: {
                shaderBuilder.append("const vec2 center = vec2(.5,0.5);\n")
                        .append("float rotations =1.0;\n")
                        .append("float scale =8.0;\n")
                        .append("const vec4 backColor = vec4(0.15,.15,.15,1.0);\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("    vec2 difference = uv - center;\n")
                        .append("    vec2 dir = normalize(difference);\n")
                        .append("    float dist = length(difference);\n")
                        .append("    float angle = 2.0 * PI * rotations * progress;\n")
                        .append("    float c = cos(angle);\n")
                        .append("    float s = sin(angle);\n")
                        .append("    float currentScale = mix(scale, 1.0, 2.0 * abs(progress - 0.5));\n")
                        .append("\n")
                        .append("    vec2 rotatedDir = vec2(dir.x  * c - dir.y * s, dir.x * s + dir.y * c);\n")
                        .append("    vec2 rotatedUv = center + rotatedDir * dist / currentScale;\n")
                        .append("\n")
                        .append("    if (rotatedUv.x < 0.0 || rotatedUv.x > 1.0 || rotatedUv.y < 0.0 || rotatedUv.y > 1.0){\n")
                        .append("        return backColor;\n")
                        .append("    }\n")
                        .append("    return mix(getFromColor(rotatedUv), getToColor(rotatedUv), progress);\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case GridFlip: {
                shaderBuilder
                        .append("ivec2 size = ivec2(4);\n")
                        .append("float pause = 0.1;\n")
                        .append("float dividerWidth = 0.05;\n")
                        .append("vec4 bgcolor = vec4(0.0,0.0,0.0,1.0);\n")
                        .append("float randomness =0.1;\n")
                        .append("\n")
                        .append("float rand (vec2 co) {\n")
                        .append("    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n")
                        .append("}\n")
                        .append("\n")
                        .append("float getDelta(vec2 p) {\n")
                        .append("    vec2 rectanglePos = floor(vec2(size) * p);\n")
                        .append("    vec2 rectangleSize = vec2(1.0 / vec2(size).x, 1.0 / vec2(size).y);\n")
                        .append("    float top = rectangleSize.y * (rectanglePos.y + 1.0);\n")
                        .append("    float bottom = rectangleSize.y * rectanglePos.y;\n")
                        .append("    float left = rectangleSize.x * rectanglePos.x;\n")
                        .append("    float right = rectangleSize.x * (rectanglePos.x + 1.0);\n")
                        .append("    float minX = min(abs(p.x - left), abs(p.x - right));\n")
                        .append("    float minY = min(abs(p.y - top), abs(p.y - bottom));\n")
                        .append("    return min(minX, minY);\n")
                        .append("}\n")
                        .append("\n")
                        .append("float getDividerSize() {\n")
                        .append("    vec2 rectangleSize = vec2(1.0 / vec2(size).x, 1.0 / vec2(size).y);\n")
                        .append("    return min(rectangleSize.x, rectangleSize.y) * dividerWidth;\n")
                        .append("}\n")
                        .append("\n").append("vec4 transition(vec2 p) {\n")
                        .append("    if(progress < pause) {\n")
                        .append("        float currentProg = progress / pause;\n")
                        .append("        float a = 1.0;\n")
                        .append("        if(getDelta(p) < getDividerSize()) {\n")
                        .append("            a = 1.0 - currentProg;\n")
                        .append("        }\n")
                        .append("        return mix(bgcolor, getFromColor(p), a);\n")
                        .append("    } else if(progress < 1.0 - pause){\n")
                        .append("        if(getDelta(p) < getDividerSize()) {\n")
                        .append("            return bgcolor;\n")
                        .append("        } else {\n")
                        .append("            float currentProg = (progress - pause) / (1.0 - pause * 2.0);\n")
                        .append("            vec2 q = p;\n")
                        .append("            vec2 rectanglePos = floor(vec2(size) * q);\n")
                        .append("\n")
                        .append("            float r = rand(rectanglePos) - randomness;\n")
                        .append("            float cp = smoothstep(0.0, 1.0 - r, currentProg);\n")
                        .append("\n").append("            float rectangleSize = 1.0 / vec2(size).x;\n")
                        .append("            float delta = rectanglePos.x * rectangleSize;\n")
                        .append("            float offset = rectangleSize / 2.0 + delta;\n")
                        .append("\n").append("            p.x = (p.x - offset)/abs(cp - 0.5)*0.5 + offset;\n")
                        .append("            vec4 a = getFromColor(p);\n")
                        .append("            vec4 b = getToColor(p);\n")
                        .append("\n")
                        .append("            float s = step(abs(vec2(size).x * (q.x - delta) - 0.5), abs(cp - 0.5));\n")
                        .append("            return mix(bgcolor, mix(b, a, step(cp, 0.5)), s);\n")
                        .append("        }\n")
                        .append("    } else {\n")
                        .append("        float currentProg = (progress - 1.0 + pause) / pause;\n")
                        .append("        float a = 1.0;\n")
                        .append("        if(getDelta(p) < getDividerSize()) {\n")
                        .append("            a = currentProg;\n")
                        .append("        }\n")
                        .append("        return mix(bgcolor, getToColor(p), a);\n")
                        .append("    }\n")
                        .append("}\n")
                        .append("\n");
                break;
            }
            case PolarFunction: {
                shaderBuilder
                        .append("int segments = 5;\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append(" float angle = atan(uv.y - 0.5, uv.x - 0.5) - 0.5 * PI;\n")
                        .append(" float normalized = (angle + 1.5 * PI) * (2.0 * PI);\n")
                        .append(" float radius = (cos(float(segments) * angle) + 4.0) / 4.0;\n")
                        .append(" float difference = length(uv - vec2(0.5, 0.5));\n")
                        .append(" if (difference > radius * progress)\n")
                        .append("   return getFromColor(uv);\n")
                        .append("else \n")
                        .append("   return getToColor(uv);\n")
                        .append("}\n");
                break;
            }
            case Heart: {
                shaderBuilder
                        .append("float inHeart (vec2 p, vec2 center, float size) {\n")
                        .append(" if (size==0.0) return 0.0;\n")
                        .append(" vec2 o = (p-center)/(1.6*size);\n")
                        .append(" float a = o.x*o.x+o.y*o.y-0.3;\n")
                        .append(" return step(a*a*a, o.x*o.x*o.y*o.y*o.y);\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append(" return mix( getFromColor(uv), getToColor(uv),inHeart(uv, vec2(0.5, 0.4), progress) );\n")
                        .append("}")
                ;
                break;
            }
            case WindowSlice: {
                shaderBuilder
                        .append("float count =10.0;\n")
                        .append("float smoothness =0.5; \n")
                        .append("\n")
                        .append("vec4 transition (vec2 p) {\n")
                        .append("  float pr = smoothstep(-smoothness, 0.0, p.x - progress * (1.0 + smoothness));\n")
                        .append("  float s = step(pr, fract(count * p.x));\n")
                        .append("  return mix(getFromColor(p), getToColor(p), s);\n")
                        .append("}");
                break;
            }
            case DoorWay: {
                shaderBuilder
                        .append("float reflection =0.4;\n")
                        .append("float perspective =.4;\n")
                        .append("float depth =3.0;\n")
                        .append("\n")
                        .append("const vec4 black = vec4(0.0, 0.0, 0.0, 1.0);\n")
                        .append("const vec2 boundMin = vec2(0.0, 0.0);\n")
                        .append("const vec2 boundMax = vec2(1.0, 1.0);\n")
                        .append("\n")
                        .append("bool inBounds (vec2 p) {\n")
                        .append("  return all(lessThan(boundMin, p)) && all(lessThan(p, boundMax));\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec2 project (vec2 p) {\n")
                        .append("  return p * vec2(1.0, -1.2) + vec2(0.0, -0.02);\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 bgColor (vec2 p, vec2 pto) {\n")
                        .append(" vec4 c = black;\n")
                        .append(" pto = project(pto);\n")
                        .append(" if (inBounds(pto)) {\n")
                        .append("   c += mix(black, getToColor(pto), reflection * mix(1.0, 0.0, pto.y));\n")
                        .append(" }\n")
                        .append(" return c;\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 transition (vec2 p) {\n")
                        .append("  vec2 pfr = vec2(-1.), pto = vec2(-1.);\n")
                        .append(" float middleSlit = 2.0 * abs(p.x-0.5) - progress;\n")
                        .append("  if (middleSlit > 0.0) {\n")
                        .append("    pfr = p + (p.x > 0.5 ? -1.0 : 1.0) * vec2(0.5*progress, 0.0);\n")
                        .append("    float d = 1.0/(1.0+perspective*progress*(1.0-middleSlit));\n")
                        .append("    pfr.y -= d/2.;\n")
                        .append("    pfr.y *= d;\n")
                        .append("    pfr.y += d/2.;\n")
                        .append("}\n")
                        .append(" float size = mix(1.0, depth, 1.-progress);\n")
                        .append(" pto = (p + vec2(-0.5, -0.5)) * vec2(size, size) + vec2(0.5, 0.5);\n")
                        .append(" if (inBounds(pfr)) {\n")
                        .append("   return getFromColor(pfr);\n")
                        .append(" } else if (inBounds(pto)) {\n")
                        .append("     return getToColor(pto);\n")
                        .append(" } else {\n")
                        .append("   return bgColor(p, pto);\n")
                        .append(" }\n")
                        .append("}\n");
                break;
            }
            case SimpleZoom: {
                shaderBuilder.append("\n")
                        .append("float zoom_quickness =0.8;\n")
                        .append("float nQuick = clamp(zoom_quickness,0.2,1.0);\n")
                        .append("\n")
                        .append("vec2 zoom(vec2 uv, float amount) {\n")
                        .append("  return 0.5 + ((uv - 0.5) * (1.0-amount));\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("  return mix(\n")
                        .append("    getFromColor(zoom(uv, smoothstep(0.0, nQuick, progress))),\n")
                        .append("    getToColor(uv),\n")
                        .append("   smoothstep(nQuick-0.2, 1.0, progress)\n")
                        .append(" );\n")
                        .append("}\n");
                break;
            }
            case Wind: {
                shaderBuilder
                        .append("float size =0.2;\n")
                        .append("\n")
                        .append("float rand (vec2 co) {\n")
                        .append("  return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("  float r = rand(vec2(0, uv.y));\n")
                        .append("  float m = smoothstep(0.0, -size, uv.x*(1.0-size) + size*r - (progress * (1.0 + size)));\n")
                        .append("  return mix(getFromColor(uv),getToColor(uv),m );\n")
                        .append("}\n");
                break;
            }
            case PinWheel: {
                shaderBuilder
                        .append("float speed =2.0;\n")
                        .append("\n")
                        .append("vec4 transition(vec2 uv) {\n")
                        .append("  vec2 p = uv.xy / vec2(1.0).xy;\n")
                        .append("  float circPos = atan(p.y - 0.5, p.x - 0.5) + progress * speed;\n")
                        .append("  float modPos = mod(circPos, 3.1415 / 4.);\n")
                        .append("  float signed = sign(progress - modPos);\n")
                        .append("  return mix(getToColor(p), getFromColor(p), step(signed, 0.5));\n")
                        .append("}\n");
                break;
            }
            case Squeeze: {
                shaderBuilder
                        .append("float colorSeparation=0.04;\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("  float y = 0.5 + (uv.y-0.5) / (1.0-progress);\n")
                        .append("  if (y < 0.0 || y > 1.0) {\n")
                        .append("     return getToColor(uv);\n")
                        .append("  } else {\n")
                        .append("   vec2 fp = vec2(uv.x, y);\n")
                        .append("   vec2 off = progress * vec2(0.0, colorSeparation);\n")
                        .append("   vec4 c = getFromColor(fp);\n")
                        .append("   vec4 cn = getFromColor(fp - off);\n")
                        .append("   vec4 cp = getFromColor(fp + off);\n")
                        .append("    return vec4(cn.r, c.g, cp.b, c.a);\n")
                        .append(" }\n")
                        .append("}\n");
                break;
            }
            case DoomScreen: {

                shaderBuilder.append("\n")
                        .append("int bars= 30;\n")
                        .append("float amplitude= 2.0;\n")
                        .append("float noise = 0.1;\n")
                        .append("float frequency= 0.5;\n")
                        .append("float dripScale = 0.5;\n")
                        .append("\n")
                        .append("float rand(int num) {\n")
                        .append("  return fract(mod(float(num) * 67123.313, 12.0) * sin(float(num) * 10.3) * cos(float(num)));\n")
                        .append("}\n")
                        .append("\n")
                        .append("float wave(int num) {\n")
                        .append("  float fn = float(num) * frequency * 0.1 * float(bars);\n")
                        .append("  return cos(fn * 0.5) * cos(fn * 0.13) * sin((fn+10.0) * 0.3) / 2.0 + 0.5;\n")
                        .append("}\n")
                        .append("\n")
                        .append("float drip(int num) {\n")
                        .append("  return sin(float(num) / float(bars - 1) * 3.141592) * dripScale;\n")
                        .append("}\n")
                        .append("\n")
                        .append("float pos(int num) {\n")
                        .append("  return (noise == 0.0 ? wave(num) : mix(wave(num), rand(num), noise)) + (dripScale == 0.0 ? 0.0 : drip(num));\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 transition(vec2 uv) {\n")
                        .append("  int bar = int(uv.x * (float(bars)));\n")
                        .append("  float scale = 1.0 + pos(bar) * amplitude;\n")
                        .append("  float phase = progress * scale;\n")
                        .append("  float posY = uv.y / vec2(1.0).y;\n")
                        .append("  vec2 p;\n")
                        .append("  vec4 c;\n")
                        .append("  if (phase + posY < 1.0) {\n")
                        .append("    p = vec2(uv.x, uv.y + mix(0.0, vec2(1.0).y, phase)) / vec2(1.0).xy;\n")
                        .append("    c = getFromColor(p);\n")
                        .append("  } else {\n")
                        .append("    p = uv.xy / vec2(1.0).xy;\n")
                        .append("    c = getToColor(p);\n")
                        .append("  }\n")
                        .append("\n")
                        .append("  return c;\n")
                        .append("}");
                break;
            }
            case GlitchMemory: {
                shaderBuilder.append("vec4 transition(vec2 p) {\n")
                        .append("  vec2 block = floor(p.xy / vec2(16));\n")
                        .append("  vec2 uv_noise = block / vec2(64);\n")
                        .append("  uv_noise += floor(vec2(progress) * vec2(1200.0, 3500.0)) / vec2(64);\n")
                        .append("  vec2 dist = progress > 0.0 ? (fract(uv_noise) - 0.5) * 0.3 *(1.0 -progress) : vec2(0.0);\n")
                        .append("  vec2 red = p + dist * 0.2;\n")
                        .append("  vec2 green = p + dist * .3;\n")
                        .append("  vec2 blue = p + dist * .5;\n")
                        .append("\n")
                        .append("  return vec4(")
                        .append("   mix(getFromColor(red), getToColor(red), progress).r,")
                        .append("   mix(getFromColor(green), getToColor(green), progress).g,")
                        .append("   mix(getFromColor(blue), getToColor(blue), progress).b,")
                        .append("   1.0);\n")
                        .append("}");
                break;
            }
            case GlitchDisplace: {
                shaderBuilder
                        .append("float random(vec2 co){\n")
                        .append("    float a = 12.9898;\n")
                        .append("    float b = 78.233;\n")
                        .append("    float c = 43758.5453;\n")
                        .append("    float dt= dot(co.xy ,vec2(a,b));\n")
                        .append("    float sn= mod(dt,3.14);\n")
                        .append("    return fract(sin(sn) * c);\n")
                        .append("}\n")
                        .append("float voronoi( in vec2 x ) {\n")
                        .append("    vec2 p = floor( x );\n")
                        .append("    vec2 f = fract( x );\n")
                        .append("    float res = 8.0;\n")
                        .append("    for( float j=-1.; j<=1.; j++ )\n")
                        .append("    for( float i=-1.; i<=1.; i++ ) {\n")
                        .append("        vec2  b = vec2( i, j );\n")
                        .append("        vec2  r = b - f + random( p + b );\n")
                        .append("        float d = dot( r, r );\n")
                        .append("        res = min( res, d );\n")
                        .append("    }\n")
                        .append("    return sqrt( res );\n")
                        .append("}")
                        .append("\n")
                        .append("vec2 displace(vec4 tex, vec2 texCoord, float dotDepth, float textureDepth, float strength) {\n")
                        .append("    float b = voronoi(.003 * texCoord + 2.0);\n")
                        .append("    float g = voronoi(0.2 * texCoord);\n")
                        .append("    float r = voronoi(texCoord - 1.0);\n")
                        .append("    vec4 dt = tex * 1.0;\n")
                        .append("    vec4 dis = dt * dotDepth + 1.0 - tex * textureDepth;\n")
                        .append("\n")
                        .append("    dis.x = dis.x - 1.0 + textureDepth*dotDepth;\n")
                        .append("    dis.y = dis.y - 1.0 + textureDepth*dotDepth;\n")
                        .append("    dis.x *= strength;\n")
                        .append("    dis.y *= strength;\n")
                        .append("    vec2 res_uv = texCoord ;\n")
                        .append("    res_uv.x = res_uv.x + dis.x - 0.0;\n")
                        .append("    res_uv.y = res_uv.y + dis.y;\n")
                        .append("    return res_uv;\n")
                        .append("}\n")
                        .append("\n")
                        .append("float ease1(float t) {\n")
                        .append("  return t == 0.0 || t == 1.0\n")
                        .append("    ? t\n")
                        .append("    : t < 0.5\n")
                        .append("      ? +0.5 * pow(2.0, (20.0 * t) - 10.0)\n")
                        .append("      : -0.5 * pow(2.0, 10.0 - (t * 20.0)) + 1.0;\n")
                        .append("}\n")
                        .append("float ease2(float t) {\n")
                        .append("  return t == 1.0 ? t : 1.0 - pow(2.0, -10.0 * t);\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 transition(vec2 uv) {\n")
                        .append("  vec2 p = uv.xy / vec2(1.0).xy;\n")
                        .append("  vec4 color1 = getFromColor(p);\n")
                        .append("  vec4 color2 = getToColor(p);\n")
                        .append("  vec2 disp = displace(color1, p, 0.33, 0.7, 1.0-ease1(progress));\n")
                        .append("  vec2 disp2 = displace(color2, p, 0.33, 0.5, ease2(progress));\n")
                        .append("  vec4 dColor1 = getToColor(disp);\n")
                        .append("  vec4 dColor2 = getFromColor(disp2);\n")
                        .append("  float val = ease1(progress);\n")
                        .append("  vec3 gray = vec3(dot(min(dColor2, dColor1).rgb, vec3(0.299, 0.587, 0.114)));\n")
                        .append("  dColor2 = vec4(gray, 1.0);\n")
                        .append("  dColor2 *= 2.0;\n")
                        .append("  color1 = mix(color1, dColor2, smoothstep(0.0, 0.5, progress));\n")
                        .append("  color2 = mix(color2, dColor1, smoothstep(1.0, 0.5, progress));\n")
                        .append("  return mix(color1, color2, val);\n")
                        .append("}");
                break;
            }
            case Kaleidoscope: {
                shaderBuilder
                        .append("float speed = 1.0;\n")
                        .append("float angle = 1.0;\n")
                        .append("float power = 1.5;\n")
                        .append("vec4 transition(vec2 uv) {\n")
                        .append("  vec2 p = uv.xy / vec2(1.0).xy;\n")
                        .append("  vec2 q = p;\n")
                        .append("  float t = pow(progress, power)*speed;\n")
                        .append("  p = p -0.5;\n")
                        .append("  for (int i = 0; i < 7; i++) {\n")
                        .append("    p = vec2(sin(t)*p.x + cos(t)*p.y, sin(t)*p.y - cos(t)*p.x);\n")
                        .append("    t += angle;\n")
                        .append("    p = abs(mod(p, 2.0) - 1.0);\n")
                        .append("  }\n")
                        .append("  abs(mod(p, 1.0));\n")
                        .append("  return mix(\n")
                        .append("    mix(getFromColor(q), getToColor(q), progress),\n")
                        .append("    mix(getFromColor(p), getToColor(p), progress), 1.0 - 2.0*abs(progress - 0.5));\n")
                        .append("}");
                break;
            }
            case Morph: {
                shaderBuilder
                        .append("float strength = 0.1;\n")
                        .append("\n")
                        .append("vec4 transition(vec2 p) {\n")
                        .append("  vec4 ca = getFromColor(p);\n")
                        .append("  vec4 cb = getToColor(p);\n")
                        .append("  \n")
                        .append("  vec2 oa = (((ca.rg+ca.b)*0.5)*2.0-1.0);\n")
                        .append("  vec2 ob = (((cb.rg+cb.b)*0.5)*2.0-1.0);\n")
                        .append("  vec2 oc = mix(oa,ob,0.5)*strength;\n")
                        .append("  \n")
                        .append("  float w0 = progress;\n")
                        .append("  float w1 = 1.0-w0;\n")
                        .append("  return mix(getFromColor(p+oc*w0), getToColor(p-oc*w1), progress);\n")
                        .append("}");
                break;
            }
            case Bounce: {
                shaderBuilder
                        .append("vec4 shadow_colour = vec4(0.,0.,0.,.6);\n")
                        .append("float shadow_height = 0.075;\n")
                        .append("float bounces = 3.0;")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("  float time = progress;\n")
                        .append("  float stime = sin(time * PI / 2.);\n")
                        .append("  float phase = time * PI * bounces;\n")
                        .append("  float y = (abs(cos(phase))) * (1.0 - stime);\n")
                        .append("  float d = uv.y - y;\n")
                        .append("  return mix(\n")
                        .append("    mix(\n")
                        .append("      getToColor(uv),\n")
                        .append("      shadow_colour,\n")
                        .append("      step(d, shadow_height) * (1. - mix(\n")
                        .append("        ((d / shadow_height) * shadow_colour.a) + (1.0 - shadow_colour.a),\n")
                        .append("        1.0,\n")
                        .append("        smoothstep(0.95, 1., progress) // fade-out the shadow at the end\n")
                        .append("      ))\n")
                        .append("    ),\n")
                        .append("    getFromColor(vec2(uv.x, uv.y + (1.0 - y))),\n")
                        .append("    step(d, 0.0)\n")
                        .append("  );\n")
                        .append("}");
                break;
            }
            case Swap: {
                shaderBuilder
                        .append("float reflection = 0.4;\n")
                        .append("float perspective = 0.2;\n")
                        .append("float depth = 3.0;\n")
                        .append("const vec4 black = vec4(0.0, 0.0, 0.0, 1.0);\n")
                        .append("const vec2 boundMin = vec2(0.0, 0.0);\n")
                        .append("const vec2 boundMax = vec2(1.0, 1.0);\n")
                        .append(" \n")
                        .append("bool inBounds (vec2 p) {\n")
                        .append("  return all(lessThan(boundMin, p)) && all(lessThan(p, boundMax));\n")
                        .append("}\n")
                        .append(" \n")
                        .append("vec2 project (vec2 p) {\n")
                        .append("  return p * vec2(1.0, -1.2) + vec2(0.0, -0.02);\n")
                        .append("}\n")
                        .append(" \n")
                        .append("vec4 bgColor (vec2 p, vec2 pfr, vec2 pto) {\n")
                        .append("  vec4 c = black;\n")
                        .append("  pfr = project(pfr);\n")
                        .append("  if (inBounds(pfr)) {\n")
                        .append("    c += mix(black, getFromColor(pfr), reflection * mix(1.0, 0.0, pfr.y));\n")
                        .append("  }\n")
                        .append("  pto = project(pto);\n")
                        .append("  if (inBounds(pto)) {\n")
                        .append("    c += mix(black, getToColor(pto), reflection * mix(1.0, 0.0, pto.y));\n")
                        .append("  }\n")
                        .append("  return c;\n")
                        .append("}\n")
                        .append(" \n")
                        .append("vec4 transition(vec2 p) {\n")
                        .append("  vec2 pfr, pto = vec2(-1.);\n")
                        .append(" \n")
                        .append("  float size = mix(1.0, depth, progress);\n")
                        .append("  float persp = perspective * progress;\n")
                        .append("  pfr = (p + vec2(-0.0, -0.5)) * vec2(size/(1.0-perspective*progress), size/(1.0-size*persp*p.x)) + vec2(0.0, 0.5);\n")
                        .append(" \n")
                        .append("  size = mix(1.0, depth, 1.-progress);\n")
                        .append("  persp = perspective * (1.-progress);\n")
                        .append("  pto = (p + vec2(-1.0, -0.5)) * vec2(size/(1.0-perspective*(1.0-progress)), size/(1.0-size*persp*(0.5-p.x))) + vec2(1.0, 0.5);\n")
                        .append("\n")
                        .append("  if (progress < 0.5) {\n")
                        .append("    if (inBounds(pfr)) {\n")
                        .append("      return getFromColor(pfr);\n")
                        .append("    }\n")
                        .append("    if (inBounds(pto)) {\n")
                        .append("      return getToColor(pto);\n")
                        .append("    }  \n")
                        .append("  }\n")
                        .append("  if (inBounds(pto)) {\n")
                        .append("    return getToColor(pto);\n")
                        .append("  }\n")
                        .append("  if (inBounds(pfr)) {\n")
                        .append("    return getFromColor(pfr);\n")
                        .append("  }\n")
                        .append("  return bgColor(p, pfr, pto);\n")
                        .append("}\n");
                break;
            }
            case Cube: {
                shaderBuilder
                        .append("float persp  = 0.7;\n")
                        .append("float unzoom = 0.3;\n")
                        .append("float reflection  = 0.4;\n")
                        .append("float floating = 3.0;\n")
                        .append("\n")
                        .append("vec2 project (vec2 p) {\n")
                        .append("  return p * vec2(1.0, -1.2) + vec2(0.0, -floating/100.);\n")
                        .append("}\n")
                        .append("\n")
                        .append("bool inBounds (vec2 p) {\n")
                        .append("  return all(lessThan(vec2(0.0), p)) && all(lessThan(p, vec2(1.0)));\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 bgColor (vec2 p, vec2 pfr, vec2 pto) {\n")
                        .append("  vec4 c = vec4(0.0, 0.0, 0.0, 1.0);\n")
                        .append("  pfr = project(pfr);\n")
                        .append("  if (inBounds(pfr)) {\n")
                        .append("    c += mix(vec4(0.0), getFromColor(pfr), reflection * mix(1.0, 0.0, pfr.y));\n")
                        .append("  }\n")
                        .append("  pto = project(pto);\n")
                        .append("  if (inBounds(pto)) {\n")
                        .append("    c += mix(vec4(0.0), getToColor(pto), reflection * mix(1.0, 0.0, pto.y));\n")
                        .append("  }\n")
                        .append("  return c;\n")
                        .append("}\n")
                        .append("vec2 xskew (vec2 p, float persp, float center) {\n")
                        .append("  float x = mix(p.x, 1.0-p.x, center);\n")
                        .append("  return (\n")
                        .append("    (\n")
                        .append("      vec2( x, (p.y - 0.5*(1.0-persp) * x) / (1.0+(persp-1.0)*x) )\n")
                        .append("      - vec2(0.5-distance(center, 0.5), 0.0)\n")
                        .append("    )\n")
                        .append("    * vec2(0.5 / distance(center, 0.5) * (center<0.5 ? 1.0 : -1.0), 1.0)\n")
                        .append("    + vec2(center<0.5 ? 0.0 : 1.0, 0.0)\n")
                        .append("  );\n")
                        .append("}\n")
                        .append("vec4 transition(vec2 op) {\n")
                        .append("  float uz = unzoom * 2.0*(0.5-distance(0.5, progress));\n")
                        .append("  vec2 p = -uz*0.5+(1.0+uz) * op;\n")
                        .append("  vec2 fromP = xskew(\n")
                        .append("    (p - vec2(progress, 0.0)) / vec2(1.0-progress, 1.0),\n")
                        .append("    1.0-mix(progress, 0.0, persp),\n")
                        .append("    0.0\n")
                        .append("  );\n")
                        .append("  vec2 toP = xskew(\n")
                        .append("    p / vec2(progress, 1.0),\n")
                        .append("    mix(pow(progress, 2.0), 1.0, persp),\n")
                        .append("    1.0\n")
                        .append("  );\n")
                        .append("  if (inBounds(fromP)) {\n")
                        .append("    return getFromColor(fromP);\n")
                        .append("  }else if (inBounds(toP)) {\n")
                        .append("    return getToColor(toP);\n")
                        .append("  }\n")
                        .append("  return bgColor(op, fromP, toP);\n")
                        .append("}");
                break;
            }
            case Hexagonalize: {
                shaderBuilder.append("int steps = 50;\n")
                        .append("float horizontalHexagons = 20.0;\n")
                        .append("float ratio = 1.0;\n").append("\n")
                        .append("struct Hexagon {\n")
                        .append("  float q;\n")
                        .append("  float r;\n")
                        .append("  float s;\n")
                        .append("};\n")
                        .append("\n")
                        .append("Hexagon createHexagon(float q, float r){\n")
                        .append("  Hexagon hex;\n")
                        .append("  hex.q = q;\n")
                        .append("  hex.r = r;\n")
                        .append("  hex.s = -q - r;\n")
                        .append("  return hex;\n")
                        .append("}\n")
                        .append("Hexagon roundHexagon(Hexagon hex){\n")
                        .append("  \n")
                        .append("  float q = floor(hex.q + 0.5);\n")
                        .append("  float r = floor(hex.r + 0.5);\n")
                        .append("  float s = floor(hex.s + 0.5);\n")
                        .append("\n")
                        .append("  float deltaQ = abs(q - hex.q);\n")
                        .append("  float deltaR = abs(r - hex.r);\n")
                        .append("  float deltaS = abs(s - hex.s);\n")
                        .append("\n")
                        .append("  if (deltaQ > deltaR && deltaQ > deltaS)\n")
                        .append("    q = -r - s;\n")
                        .append("  else if (deltaR > deltaS)\n")
                        .append("    r = -q - s;\n")
                        .append("  else\n")
                        .append("    s = -q - r;\n")
                        .append("\n")
                        .append("  return createHexagon(q, r);\n")
                        .append("}\n")
                        .append("\n")
                        .append("Hexagon hexagonFromPoint(vec2 point, float size) {\n")
                        .append("  \n")
                        .append("  point.y /= ratio;\n")
                        .append("  point = (point - 0.5) / size;\n")
                        .append("  \n")
                        .append("  float q = (sqrt(3.0) / 3.0) * point.x + (-1.0 / 3.0) * point.y;\n")
                        .append("  float r = 0.0 * point.x + 2.0 / 3.0 * point.y;\n")
                        .append("\n")
                        .append("  Hexagon hex = createHexagon(q, r);\n")
                        .append("  return roundHexagon(hex);\n")
                        .append("  \n")
                        .append("}\n")
                        .append("\n")
                        .append("vec2 pointFromHexagon(Hexagon hex, float size) {\n")
                        .append("  \n")
                        .append("  float x = (sqrt(3.0) * hex.q + (sqrt(3.0) / 2.0) * hex.r) * size + 0.5;\n")
                        .append("  float y = (0.0 * hex.q + (3.0 / 2.0) * hex.r) * size + 0.5;\n")
                        .append("  \n")
                        .append("  return vec2(x, y * ratio);\n")
                        .append("}\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("  \n")
                        .append("  float dist = 2.0 * min(progress, 1.0 - progress);\n")
                        .append("  dist = steps > 0 ? ceil(dist * float(steps)) / float(steps) : dist;\n")
                        .append("  \n")
                        .append("  float size = (sqrt(3.0) / 3.0) * dist / horizontalHexagons;\n")
                        .append("  \n")
                        .append("  vec2 point = dist > 0.0 ? pointFromHexagon(hexagonFromPoint(uv, size), size) : uv;\n")
                        .append("\n")
                        .append("  return mix(getFromColor(point), getToColor(point), progress);\n")
                        .append("  \n")
                        .append("}");
                break;
            }
            case Pixelize: {
                shaderBuilder
                        .append("ivec2 squaresMin = ivec2(20);\n")
                        .append("int steps = 50;\n")
                        .append("\n")
                        .append("vec4 transition(vec2 uv) {\n")
                        .append("  float d = min(progress, 1.0 - progress);\n")
                        .append("  float dist = steps>0 ? ceil(d * float(steps)) / float(steps) : d;\n")
                        .append("  vec2 squareSize = 2.0 * dist / vec2(squaresMin);\n")
                        .append("  vec2 p = dist>0.0 ? (floor(uv / squareSize) + 0.5) * squareSize : uv;\n")
                        .append("  return mix(getFromColor(p), getToColor(p), progress);\n")
                        .append("}");
                break;
            }
            case DirectionalWrap: {
                shaderBuilder
                        .append("float smoothness = 0.1;\n")
                        .append("vec2 direction = vec2(-1.0, 1.0);\n")
                        .append("const vec2 center = vec2(0.5, 0.5);\n")
                        .append("\n")
                        .append("vec4 transition (vec2 uv) {\n")
                        .append("  vec2 v = normalize(direction);\n")
                        .append("  v /= abs(v.x) + abs(v.y);\n")
                        .append("  float d = v.x * center.x + v.y * center.y;\n")
                        .append("  float m = 1.0 - smoothstep(-smoothness, 0.0, v.x * uv.x + v.y * uv.y - (d - 0.5 + progress * (1.0 + smoothness)));\n")
                        .append("  return mix(getFromColor((uv - 0.5) * (1.0 - m) + 0.5), getToColor((uv - 0.5) * m + 0.5), m);\n")
                        .append("}");
                break;
            }
            case Mosaic: {
                shaderBuilder
                        .append("int endx = 2;\n")
                        .append("int endy = -1;")
                        .append("\n")
                        .append("float Rand(vec2 v) {\n")
                        .append("  return fract(sin(dot(v.xy ,vec2(12.9898,78.233))) * 43758.5453);\n")
                        .append("}\n")
                        .append("vec2 Rotate(vec2 v, float a) {\n")
                        .append("  mat2 rm = mat2(cos(a), -sin(a),\n")
                        .append("                 sin(a), cos(a));\n")
                        .append("  return rm*v;\n")
                        .append("}\n")
                        .append("float CosInterpolation(float x) {\n")
                        .append("  return -cos(x*PI)/2.+.5;\n")
                        .append("}\n")
                        .append("vec4 transition(vec2 uv) {\n")
                        .append("  vec2 p = uv.xy / vec2(1.0).xy - .5;\n")
                        .append("  vec2 rp = p;\n")
                        .append("  float rpr = (progress*2.-1.);\n")
                        .append("  float z = -(rpr*rpr*2.) + 3.;\n")
                        .append("  float az = abs(z);\n")
                        .append("  rp *= az;\n")
                        .append("  rp += mix(vec2(.5, .5), vec2(float(endx) + .5, float(endy) + .5), POW2(CosInterpolation(progress)));\n")
                        .append("  vec2 mrp = mod(rp, 1.);\n")
                        .append("  vec2 crp = rp;\n")
                        .append("  bool onEnd = int(floor(crp.x))==endx&&int(floor(crp.y))==endy;\n")
                        .append("  if(!onEnd) {\n")
                        .append("    float ang = float(int(Rand(floor(crp))*4.))*.5*PI;\n")
                        .append("    mrp = vec2(.5) + Rotate(mrp-vec2(.5), ang);\n")
                        .append("  }\n")
                        .append("  if(onEnd || Rand(floor(crp))>.5) {\n")
                        .append("    return getToColor(mrp);\n")
                        .append("  } else {\n")
                        .append("    return getFromColor(mrp);\n")
                        .append("  }\n")
                        .append("}");
                break;
            }
            case Fade:
            default:
                shaderBuilder.append("\n")
                        .append("vec4 transition(vec2 uv){\n")
                        .append("    return mix(getFromColor(uv),getToColor(uv),progress);\n")
                        .append("}\n");
                break;
        }

        shaderBuilder.append("\n")
                .append("void main(){\n")
                .append("    gl_FragColor = mix(vec4(vec3(0.0),1.0), transition(vTexCoordinate), 1.0);\n")
                .append("}\n");

        return shaderBuilder.toString();
    }

    public void draw(float[] mvpMatrix, int textureId, int textureId2, float progress) {
        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textureBuffer);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "uTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        sTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "sTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId2);
        GLES20.glUniform1i(sTextureUniformHandle, 1);

        int timeHandle = GLES20.glGetUniformLocation(mProgram, "progress");
        GLES20.glUniform1f(timeHandle, progress);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void draw(float[] mvpMatrix, int textureId, int textureId2, float progress, int width, int height) {
        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textureBuffer);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "uTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        sTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "sTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId2);
        GLES20.glUniform1i(sTextureUniformHandle, 1);

        int timeHandle = GLES20.glGetUniformLocation(mProgram, "progress");
        GLES20.glUniform1f(timeHandle, progress);

        int iResolutionLoc = GLES20.glGetUniformLocation(mProgram, "iResolution");
        GLES20.glUniform3f(iResolutionLoc, width, height, 0.0f);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void release() {
        GLES20.glDeleteProgram(mProgram);
        mProgram = -1;
    }

}
